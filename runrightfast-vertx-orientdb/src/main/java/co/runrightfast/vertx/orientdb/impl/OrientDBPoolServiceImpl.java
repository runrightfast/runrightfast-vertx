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
import co.runrightfast.vertx.orientdb.OrientDBPoolConfig;
import co.runrightfast.vertx.orientdb.OrientDBPoolService;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import java.util.Optional;
import java.util.Set;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Builder
@Log
public class OrientDBPoolServiceImpl extends AbstractIdleService implements OrientDBPoolService {

    private ImmutableMap<String, OPartitionedDatabasePool> databasePools;
    private ImmutableMap<String, ODatabaseDocumentTxSupplier> oDatabaseDocumentTxSuppliers;

    @NonNull
    @Getter
    @Singular
    private final Set<OrientDBPoolConfig> databasePoolConfigs;

    @Override
    protected void startUp() throws Exception {
        createDatabasePools();
    }

    @Override
    protected void shutDown() throws Exception {
        if (databasePools != null) {
            databasePools.values().stream().forEach(OPartitionedDatabasePool::close);
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
        databasePoolConfigs.stream().forEach(poolConfig -> {
            try {
                final OPartitionedDatabasePool pool = poolConfig.createDatabasePool();
                oPartitionedDatabasePoolMapBuilder.put(poolConfig.getDatabaseName(), pool);
                oDatabaseDocumentTxSupplierMapBuilder.put(poolConfig.getDatabaseName(), createODatabaseDocumentTxSupplier(poolConfig, pool));
                log.logp(INFO, getClass().getName(), "createDatabasePools", () -> String.format("Created database pool for: %s", poolConfig));
            } catch (final Exception e) {
                log.logp(SEVERE, getClass().getName(), "createDatabasePools", e, () -> String.format("failed to create database pool for: %s", poolConfig));
            }
        });

        this.databasePools = oPartitionedDatabasePoolMapBuilder.build();
        this.oDatabaseDocumentTxSuppliers = oDatabaseDocumentTxSupplierMapBuilder.build();
    }

    private ODatabaseDocumentTxSupplier createODatabaseDocumentTxSupplier(final OrientDBPoolConfig poolConfig, final OPartitionedDatabasePool pool) {
        if (CollectionUtils.isNotEmpty(poolConfig.getHooks())) {
            return () -> {
                final ODatabaseDocumentTx db = pool.acquire();
                poolConfig.getHooks().stream().forEach(hook -> db.registerHook(hook.get()));
                return db;
            };
        } else {
            return () -> pool.acquire();
        }
    }

}
