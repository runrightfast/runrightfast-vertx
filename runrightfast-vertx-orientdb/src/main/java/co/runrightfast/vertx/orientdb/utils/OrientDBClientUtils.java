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

import co.runrightfast.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import java.util.logging.Level;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 */
@Log
public final class OrientDBClientUtils {

    private OrientDBClientUtils() {
    }

    /**
     * Sets JVM system properties required by OrientDB to configure SSL for remote client connections.
     *
     * NOTE: The JVM system properties must be set before OrientDB is initialized.
     *
     * <code>
     * {
     *    enabled = true
     *    keyStore = ${runrightfast.orientdb.client.cert.dir}/orientdb-client.ks
     *
     *    keyStorePass = NOT_SPECIFIED
     *    keyStorePass = ${?ORIENTDB_CLIENT_KEYSTORE_PASSWORD}
     *
     *    trustStore = ${runrightfast.orientdb.client.cert.dir}/orientdb-client.ts
     *
     *    trustStorePass = NOT_SPECIFIED
     *    trustStorePass = ${?ORIENTDB_CLIENT_TRUSTSTORE_PASSWORD}
     * }
     * </code>
     *
     * @param config
     */
    public static void loadClientSSLConfig(@NonNull final Config config) {
        final boolean enabled = ConfigUtils.getBoolean(config, "enabled").orElse(Boolean.FALSE);
        log.logp(Level.INFO, OrientDBClientUtils.class.getName(), "loadClientSSLConfig", "client.ssl.enabled = {0}", enabled);
        if (enabled) {
            System.setProperty("client.ssl.enabled", "true");
            System.setProperty("client.ssl.keyStore", config.getString("keyStore"));
            System.setProperty("client.ssl.keyStorePass", config.getString("keyStorePass"));
            System.setProperty("client.ssl.trustStore", config.getString("trustStore"));
            System.setProperty("client.ssl.trustStorePass", config.getString("trustStorePass"));
        }
    }

}
