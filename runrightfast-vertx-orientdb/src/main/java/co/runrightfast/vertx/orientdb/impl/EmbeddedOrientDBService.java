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

import co.runrightfast.vertx.orientdb.OrientDBService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;

/**
 *
 * @author alfio
 */
public class EmbeddedOrientDBService extends AbstractIdleService implements OrientDBService {

    private OServer server;

    private OPartitionedDatabasePool pool;

    @Override
    protected void startUp() throws Exception {

    }

    @Override
    protected void shutDown() throws Exception {
        if (server != null) {
            if (pool != null) {
                pool.close();
            }
            server.shutdown();
            server = null;
        }
    }

    @Override
    public ODatabaseDocumentTx get() {
        return pool.acquire();
    }

}
