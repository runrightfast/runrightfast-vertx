/*
 Copyright 2015 Alfio Zappala

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package co.runrightfast.vertx.orientdb.impl;

import co.runrightfast.core.ApplicationException;
import co.runrightfast.vertx.core.utils.JvmProcess;
import co.runrightfast.vertx.orientdb.DatabasePoolConfig;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import static co.runrightfast.vertx.orientdb.OrientDBConstants.NETWORK_BINARY_PROTOCOL;
import static co.runrightfast.vertx.orientdb.OrientDBConstants.ROOT_USER;
import co.runrightfast.vertx.orientdb.OrientDBService;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@RequiredArgsConstructor
@Log
public final class EmbeddedOrientDBService extends AbstractIdleService implements OrientDBService {

    private OServer server;

    private ImmutableMap<String, OPartitionedDatabasePool> databasePools;
    private ImmutableMap<String, ODatabaseDocumentTxSupplier> oDatabaseDocumentTxSuppliers;
    private final EmbeddedOrientDBServiceConfig config;

    @Override
    protected void startUp() throws Exception {
        registerLifecycleListeners();
        server = OServerMain.create(true);
        server.setServerRootDirectory(config.getOrientDBRootDir().toAbsolutePath().toString());

        final OServerConfiguration serverConfig = new OServerConfiguration();
        registerHandlers(serverConfig);
        serverConfig.network = config.getNetworkConfig();
        serverConfig.users = config.getUsers().stream().toArray(OServerUserConfiguration[]::new);

        final ImmutableList.Builder<OServerEntryConfiguration> propertiesBuilder = ImmutableList.builder();
        config.getProperties().entrySet().stream()
                .map(entry -> new OServerEntryConfiguration(entry.getKey().getKey(), entry.getValue()))
                .forEach(propertiesBuilder::add);
        config.getGlobalConfigProperties().entrySet().stream()
                .map(entry -> new OServerEntryConfiguration(entry.getKey().key, entry.getValue()))
                .forEach(propertiesBuilder::add);
        serverConfig.properties = propertiesBuilder.build().stream().toArray(OServerEntryConfiguration[]::new);

        server.startup(serverConfig);
        server.activate();
        registerDatabaseLifeCycleListeners();
        createDatabasePools();
    }

    @Override
    protected void shutDown() throws Exception {
        if (server != null) {
            if (databasePools != null) {
                databasePools.values().stream().forEach(OPartitionedDatabasePool::close);
            }
            server.shutdown();
            server = null;
        }
    }

    private void registerLifecycleListeners() throws Exception {
        if (CollectionUtils.isNotEmpty(config.getLifecycleListeners())) {
            final Orient orient = Orient.instance();
            config.getLifecycleListeners().stream().map(Supplier::get).forEach(orient::addDbLifecycleListener);
        }
    }

    private void registerHandlers(final OServerConfiguration serverConfig) {
        if (CollectionUtils.isNotEmpty(config.getHandlers())) {
            serverConfig.handlers = config.getHandlers().stream().map(Supplier::get).collect(Collectors.toList());
        }
    }

    @Override
    public Set<String> getDatabaseNames() {
        return databasePools.keySet();
    }

    @Override
    public Optional<ODatabaseDocumentTxSupplier> getODatabaseDocumentTxSupplier(final String name) {
        checkArgument(isNotBlank(name));
        return Optional.ofNullable(oDatabaseDocumentTxSuppliers.get(name));
    }

    private void createDatabasePools() {
        final ImmutableMap.Builder<String, OPartitionedDatabasePool> oPartitionedDatabasePoolMapBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, ODatabaseDocumentTxSupplier> oDatabaseDocumentTxSupplierMapBuilder = ImmutableMap.builder();
        config.getDatabasePoolConfigs().stream().forEach(poolConfig -> {
            try {
                final OPartitionedDatabasePool pool = createDatabasePool(poolConfig);
                oPartitionedDatabasePoolMapBuilder.put(poolConfig.getDatabaseName(), pool);
                oDatabaseDocumentTxSupplierMapBuilder.put(poolConfig.getDatabaseName(), createODatabaseDocumentTxSupplier(poolConfig.getDatabaseName(), pool));
                log.logp(INFO, getClass().getName(), "createDatabasePools", () -> String.format("Created database pool for: %s", poolConfig));
            } catch (final Exception e) {
                log.logp(SEVERE, getClass().getName(), "createDatabasePools", e, () -> String.format("failed to create database pool for: %s", poolConfig));
            }
        });

        this.databasePools = oPartitionedDatabasePoolMapBuilder.build();
        this.oDatabaseDocumentTxSuppliers = oDatabaseDocumentTxSupplierMapBuilder.build();
    }

    private OPartitionedDatabasePool createDatabasePool(final DatabasePoolConfig poolConfig) {
        return new OPartitionedDatabasePool(poolConfig.getDatabaseUrl(), poolConfig.getUserName(), poolConfig.getPassword(), poolConfig.getMaxPoolSize());
    }

    private ODatabaseDocumentTxSupplier createODatabaseDocumentTxSupplier(final String database, final OPartitionedDatabasePool pool) {
        if (CollectionUtils.isNotEmpty(config.getHooks())) {
            return () -> {
                final ODatabaseDocumentTx db = pool.acquire();
                config.getHooks().stream().forEach(hook -> db.registerHook(hook.get()));
                return db;
            };
        } else {
            return () -> pool.acquire();
        }
    }

    private void registerDatabaseLifeCycleListeners() {
        if (CollectionUtils.isNotEmpty(config.getLifecycleListeners())) {
            config.getLifecycleListeners().forEach(l -> Orient.instance().addDbLifecycleListener(l.get()));
        }
    }

    @Override
    public OServerAdmin getServerAdmin() {
        final String ipAddress = config.getNetworkConfig().listeners.stream()
                .filter(l -> l.protocol.equals(NETWORK_BINARY_PROTOCOL))
                .findFirst()
                .map(l -> l.ipAddress.equals("0.0.0.0") ? JvmProcess.HOST : l.ipAddress)
                .orElse(JvmProcess.HOST);

        try {
            final OServerAdmin serverAdmin = new OServerAdmin(String.format("remote:%s", ipAddress));
            final OServerUserConfiguration userConfig = server.getUser(ROOT_USER);
            serverAdmin.connect(userConfig.name, userConfig.password);
            return serverAdmin;
        } catch (final IOException ex) {
            throw new ApplicationException(ex);
        }
    }

}
