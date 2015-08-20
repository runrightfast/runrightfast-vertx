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

import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import co.runrightfast.vertx.orientdb.OrientDBService;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseFactory;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import static java.util.logging.Level.INFO;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@RequiredArgsConstructor
@Log
public class EmbeddedOrientDBService extends AbstractIdleService implements OrientDBService {

    private static final String CLASS_NAME = EmbeddedOrientDBService.class.getSimpleName();

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
        serverConfig.properties = config.getProperties().entrySet().stream()
                .map(entry -> new OServerEntryConfiguration(entry.getKey().getKey(), entry.getValue()))
                .toArray(OServerEntryConfiguration[]::new);

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
            config.getLifecycleListeners().stream().forEach(orient::addDbLifecycleListener);
        }
    }

    private void registerHandlers(final OServerConfiguration serverConfig) {
        if (CollectionUtils.isNotEmpty(config.getHandlers())) {
            serverConfig.handlers = config.getHandlers().stream().collect(Collectors.toList());
        }
    }

    @Override
    public Set<String> getDatabaseNames() {
        return databasePools.keySet();
    }

    @Override
    public Optional<ODatabaseDocumentTxSupplier> getODatabaseDocumentTxSupplier(final String name) {
        checkArgument(isNotBlank(name));
        final OPartitionedDatabasePool pool = databasePools.get(name);
        if (pool == null) {
            return Optional.empty();
        }
        return Optional.of(() -> pool.acquire());
    }

    private void createDatabasePools() {
        final ImmutableMap.Builder<String, OPartitionedDatabasePool> oPartitionedDatabasePoolMapBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, ODatabaseDocumentTxSupplier> oDatabaseDocumentTxSupplierMapBuilder = ImmutableMap.builder();
        config.getDatabasePoolConfigs().stream().forEach(poolConfig -> {
            if (poolConfig.isCreateDatabase()) {
                createDatabase(poolConfig);
            }
            final OPartitionedDatabasePool pool = new OPartitionedDatabasePool(poolConfig.getDatabaseUrl(), poolConfig.getUserName(), poolConfig.getPassword(), poolConfig.getMaxPoolSize());
            oPartitionedDatabasePoolMapBuilder.put(poolConfig.getDatabaseName(), pool);

            if (CollectionUtils.isNotEmpty(config.getHooks())) {
                oDatabaseDocumentTxSupplierMapBuilder.put(poolConfig.getDatabaseName(), () -> {
                    final ODatabaseDocumentTx db = pool.acquire();
                    config.getHooks().stream().forEach(hook -> db.registerHook(hook.get()));
                    return db;
                });
            } else {
                oDatabaseDocumentTxSupplierMapBuilder.put(poolConfig.getDatabaseName(), () -> pool.acquire());
            }
        });

        this.databasePools = oPartitionedDatabasePoolMapBuilder.build();
    }

    private void createDatabase(final DatabasePoolConfig poolConfig) {
        final String database = StringUtils.split(poolConfig.getDatabaseUrl(), ':')[1];
        final Path databaseDir;
        if (database.startsWith("/")) {
            databaseDir = Paths.get(database);
        } else {
            databaseDir = Paths.get(config.getOrientDBRootDir().toString(), "databases", database).toAbsolutePath();
        }

        final String dbUrl = "plocal:" + databaseDir;
        try (final ODatabase db = new ODatabaseFactory().createDatabase("document", dbUrl).create()) {
            log.logp(INFO, CLASS_NAME, "createDatabase", String.format("created db = %s", db.getName()));
        }
    }

    private void registerDatabaseLifeCycleListeners() {
        if (CollectionUtils.isNotEmpty(config.getLifecycleListeners())) {
            config.getLifecycleListeners().forEach(l -> Orient.instance().addDbLifecycleListener(l));
        }
    }

}
