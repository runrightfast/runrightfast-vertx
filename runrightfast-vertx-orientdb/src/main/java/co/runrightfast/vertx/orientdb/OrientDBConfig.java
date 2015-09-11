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
package co.runrightfast.vertx.orientdb;

import co.runrightfast.vertx.core.utils.ConfigUtils;
import static co.runrightfast.vertx.core.utils.JvmProcess.HOST;
import co.runrightfast.vertx.orientdb.impl.embedded.OGraphServerHandlerConfig;
import com.typesafe.config.Config;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import javax.inject.Qualifier;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 */
@Log
public final class OrientDBConfig {

    @Qualifier
    @Documented
    @Retention(RUNTIME)
    public @interface OrientDBConfiguration {
    }

    @Getter
    private final Path homeDirectory;

    @Getter
    private final String nodeName;

    @Getter
    private final OGraphServerHandlerConfig oGraphServerHandlerConfig;

    public OrientDBConfig(@NonNull final Config config) {
        this.homeDirectory = Paths.get(ConfigUtils.getString(config, "server", "home", "dir").orElse("/orientdb"));
        this.nodeName = ConfigUtils.getString(config, "server", "nodeName").orElseGet(this::defaultNodeName);
        this.oGraphServerHandlerConfig = ConfigUtils.getConfig(config, "server", "handlers", "OGraphServerHandlerConfig").map(OGraphServerHandlerConfig::new).orElseGet(() -> new OGraphServerHandlerConfig());
        loadClientSSLConfig(config);
    }

    private void loadClientSSLConfig(final Config config) {
        ConfigUtils.getConfig(config, "client", "ssl").ifPresent(clientSSLConfig -> {
            final boolean enabled = ConfigUtils.getBoolean(clientSSLConfig, "enabled").orElse(Boolean.FALSE);
            log.logp(Level.INFO, getClass().getName(), "loadClientSSLConfig", "client.ssl.enabled = {0}", enabled);
            if (enabled) {
                System.setProperty("client.ssl.enabled", "true");
                System.setProperty("client.ssl.keyStore", clientSSLConfig.getString("keyStore"));
                System.setProperty("client.ssl.keyStorePass", clientSSLConfig.getString("keyStorePass"));
                System.setProperty("client.ssl.trustStore", clientSSLConfig.getString("trustStore"));
                System.setProperty("client.ssl.trustStorePass", clientSSLConfig.getString("trustStorePass"));
            }
        });
    }

    public String defaultNodeName() {
        final int index = HOST.indexOf('.');
        if (index == -1) {
            return HOST;
        }

        return HOST.substring(0, index);
    }
}
