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
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

/**
 *
 * @author alfio
 */
@RequiredArgsConstructor
public class EmbeddedOrientDBService extends AbstractIdleService implements OrientDBService {

    private OServer server;

    private ImmutableMap<String, OPartitionedDatabasePool> databasePools = ImmutableMap.of();
    private final EmbeddedOrientDBServiceConfig config;

    @Override
    protected void startUp() throws Exception {
        registerLifecycleListeners();
        server = OServerMain.create(true);
        server.setServerRootDirectory(config.getOrientDBRootDir().toAbsolutePath().toString());

        final OServerConfiguration serverConfig = new OServerConfiguration();
        registerHandlers(serverConfig);
        serverConfig.network = config.getNetworkConfig();

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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
