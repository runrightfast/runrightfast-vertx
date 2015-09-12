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
import co.runrightfast.vertx.orientdb.impl.embedded.NetworkSSLConfig;
import co.runrightfast.vertx.orientdb.impl.embedded.OGraphServerHandlerConfig;
import co.runrightfast.vertx.orientdb.impl.embedded.OHazelcastPluginConfig;
import co.runrightfast.vertx.orientdb.impl.embedded.OServerNetworkConfigurationSupplier;
import co.runrightfast.vertx.orientdb.impl.embedded.OServerSideScriptInterpreterConfig;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.typesafe.config.Config;
import static java.lang.Boolean.FALSE;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;
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
    private final List<Supplier<OServerHandlerConfiguration>> handlers;

    @Getter
    private final OServerNetworkConfigurationSupplier networkConfig;

    public OrientDBConfig(@NonNull final Config config) {
        this.homeDirectory = Paths.get(ConfigUtils.getString(config, "server", "home", "dir").orElse("/orientdb"));
        this.nodeName = ConfigUtils.getString(config, "server", "nodeName").orElseGet(this::defaultNodeName);
        ConfigUtils.getConfig(config, "client", "ssl").ifPresent(OrientDBClientConfig::loadClientSSLConfig);
        handlers = ImmutableList.of(
                oGraphServerHandlerConfig(config),
                oHazelcastPluginConfig(config),
                oServerSideScriptInterpreterConfig(config)
        );

        this.networkConfig = oServerNetworkConfigurationSupplier(config);
    }

    private OServerNetworkConfigurationSupplier oServerNetworkConfigurationSupplier(final Config config) {
        if (ConfigUtils.getBoolean(config, "server", "network-config", "ssl", "enabled").orElse(FALSE)) {
            return new OServerNetworkConfigurationSupplier(new NetworkSSLConfig(config.getConfig(ConfigUtils.configPath("server", "network-config", "ssl"))));
        }
        return new OServerNetworkConfigurationSupplier(ConfigUtils.getInt(config, "server", "network-config", "port").orElse(OServerNetworkConfigurationSupplier.DEFAULT_PORT));
    }

    private OGraphServerHandlerConfig oGraphServerHandlerConfig(final Config config) {
        return ConfigUtils.getConfig(config, "server", "handlers", "OGraphServerHandlerConfig")
                .map(OGraphServerHandlerConfig::new)
                .orElseGet(() -> new OGraphServerHandlerConfig());
    }

    private OHazelcastPluginConfig oHazelcastPluginConfig(final Config config) {
        return ConfigUtils.getConfig(config, "server", "handlers", "OHazelcastPluginConfig")
                .map(pluginConfig -> {
                    final boolean enabled = pluginConfig.getBoolean("enabled");
                    if (!enabled) {
                        return new OHazelcastPluginConfig();
                    }
                    return new OHazelcastPluginConfig(this.nodeName, Paths.get(pluginConfig.getString("distributedDBConfigFilePath")));
                })
                .orElseGet(() -> new OHazelcastPluginConfig());
    }

    private OServerSideScriptInterpreterConfig oServerSideScriptInterpreterConfig(final Config config) {
        return ConfigUtils.getConfig(config, "server", "handlers", "OServerSideScriptInterpreterConfig")
                .map(pluginConfig -> {
                    return new OServerSideScriptInterpreterConfig(
                            pluginConfig.getBoolean("enabled"),
                            pluginConfig.getStringList("allowedLanguages")
                    );
                })
                .orElseGet(() -> new OServerSideScriptInterpreterConfig());
    }

    public String defaultNodeName() {
        final int index = HOST.indexOf('.');
        if (index == -1) {
            return HOST;
        }

        return HOST.substring(0, index);
    }
}
