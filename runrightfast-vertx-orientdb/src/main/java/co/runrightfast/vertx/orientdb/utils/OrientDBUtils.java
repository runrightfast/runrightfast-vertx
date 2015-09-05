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
package co.runrightfast.vertx.orientdb.utils;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import java.io.IOException;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

/**
 *
 * @author alfio
 */
public interface OrientDBUtils {

    static void createDatabase(final OServerAdmin serverAdmin, final String database) {
        final Logger log = Logger.getLogger(OrientDBUtils.class.getName());
        try {
            if (!databaseExists(serverAdmin, database)) {
                serverAdmin.createDatabase(database, "document", "plocal");
                log.logp(INFO, OrientDBUtils.class.getName(), "createDatabase", String.format("created db : %s", database));
                serverAdmin.listDatabases().entrySet().forEach(entry -> log.info(String.format("%s -> %s", entry.getKey(), entry.getValue())));
            } else {
                log.logp(INFO, OrientDBUtils.class.getName(), "createDatabase", String.format("db already exists: %s", database));
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (serverAdmin != null) {
                serverAdmin.close();
            }
        }
    }

    static boolean databaseExists(final OServerAdmin serverAdmin, final String database) {
        try {
            return serverAdmin.listDatabases().containsKey(database);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
