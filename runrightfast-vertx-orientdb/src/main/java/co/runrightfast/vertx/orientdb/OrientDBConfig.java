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
import co.runrightfast.vertx.orientdb.impl.embedded.OAutomaticBackupConfig;
import co.runrightfast.vertx.orientdb.impl.embedded.OAutomaticBackupConfig.Delay;
import co.runrightfast.vertx.orientdb.impl.embedded.OGraphServerHandlerConfig;
import co.runrightfast.vertx.orientdb.impl.embedded.OHazelcastPluginConfig;
import co.runrightfast.vertx.orientdb.impl.embedded.OServerNetworkConfigurationSupplier;
import co.runrightfast.vertx.orientdb.impl.embedded.OServerSideScriptInterpreterConfig;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.handler.OJMXPlugin;
import com.orientechnologies.orient.server.plugin.livequery.OLiveQueryPlugin;
import com.typesafe.config.Config;
import static java.lang.Boolean.FALSE;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;
import static java.util.logging.Level.INFO;
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
                oServerSideScriptInterpreterConfig(config),
                oJMXPlugin(config),
                oLiveQueryPlugin(config),
                oAutomaticBackupConfig(config)
        );

        this.networkConfig = oServerNetworkConfigurationSupplier(config);
    }

    private Supplier<OServerHandlerConfiguration> oAutomaticBackupConfig(final Config config) {
        final OAutomaticBackupConfig backupConfig = ConfigUtils.getConfig(config, "server", "handlers", "OAutomaticBackupConfig").map(pluginConfig -> {
            return OAutomaticBackupConfig.builder()
                    .enabled(pluginConfig.getBoolean("enabled"))
                    .delay(new Delay(pluginConfig.getString("delay")))
                    .firstTime(pluginConfig.getString("firstTime"))
                    .backupDir(Paths.get(pluginConfig.getString("backup")))
                    .compressionLevel(pluginConfig.getInt("compressionLevel"))
                    .bufferSizeMB(pluginConfig.getInt("bufferSizeMB"))
                    .databaseIncludes(pluginConfig.getStringList("databaseIncludes"))
                    .databaseExcludes(pluginConfig.getStringList("databaseExcludes"))
                    .build();
        }).orElseGet(OAutomaticBackupConfig::disabledOAutomaticBackupConfig);
        log.logp(INFO, getClass().getName(), "oAutomaticBackupConfig", backupConfig.toString());
        return backupConfig;
    }

    private Supplier<OServerHandlerConfiguration> oLiveQueryPlugin(final Config config) {
        return () -> {
            final OServerHandlerConfiguration handlerConfig = new OServerHandlerConfiguration();
            handlerConfig.clazz = OLiveQueryPlugin.class.getName();
            final String enabled = ConfigUtils.getBoolean(config, "server", "handlers", "OLiveQueryPluginConfig", "enabled").orElse(FALSE).toString();
            log.logp(INFO, getClass().getName(), "oLiveQueryPlugin", "enabled = {}", enabled);
            handlerConfig.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("enabled", enabled)
            };
            return handlerConfig;
        };
    }

    private Supplier<OServerHandlerConfiguration> oJMXPlugin(final Config config) {
        return () -> {
            final OServerHandlerConfiguration handlerConfig = new OServerHandlerConfiguration();
            handlerConfig.clazz = OJMXPlugin.class.getName();
            final String enabled = ConfigUtils.getBoolean(config, "server", "handlers", "OJMXPluginConfig", "enabled").orElse(FALSE).toString();
            log.logp(INFO, getClass().getName(), "oJMXPlugin", "enabled = {}", enabled);
            handlerConfig.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("enabled", enabled),
                new OServerParameterConfiguration("profilerManaged", enabled)
            };
            return handlerConfig;
        };
    }

    private OServerNetworkConfigurationSupplier oServerNetworkConfigurationSupplier(final Config config) {
        if (ConfigUtils.getBoolean(config, "server", "network-config", "ssl", "enabled").orElse(FALSE)) {
            return new OServerNetworkConfigurationSupplier(new NetworkSSLConfig(config.getConfig(ConfigUtils.configPath("server", "network-config", "ssl"))));
        }
        final OServerNetworkConfigurationSupplier networkConfig = new OServerNetworkConfigurationSupplier(ConfigUtils.getInt(config, "server", "network-config", "port").orElse(OServerNetworkConfigurationSupplier.DEFAULT_PORT));
        log.logp(INFO, getClass().getName(), "oServerNetworkConfigurationSupplier", networkConfig.toString());
        return networkConfig;
    }

    private OGraphServerHandlerConfig oGraphServerHandlerConfig(final Config config) {
        final OGraphServerHandlerConfig handlerConfig = ConfigUtils.getConfig(config, "server", "handlers", "OGraphServerHandlerConfig")
                .map(OGraphServerHandlerConfig::new)
                .orElseGet(() -> new OGraphServerHandlerConfig());
        log.logp(INFO, getClass().getName(), "oGraphServerHandlerConfig", handlerConfig.toString());
        return handlerConfig;
    }

    private OHazelcastPluginConfig oHazelcastPluginConfig(final Config config) {
        final OHazelcastPluginConfig handlerConfig = ConfigUtils.getConfig(config, "server", "handlers", "OHazelcastPluginConfig")
                .map(pluginConfig -> {
                    final boolean enabled = pluginConfig.getBoolean("enabled");
                    if (!enabled) {
                        return new OHazelcastPluginConfig();
                    }
                    return new OHazelcastPluginConfig(this.nodeName, Paths.get(pluginConfig.getString("distributedDBConfigFilePath")));
                })
                .orElseGet(() -> new OHazelcastPluginConfig());
        log.logp(INFO, getClass().getName(), "oGraphServerHandlerConfig", handlerConfig.toString());
        return handlerConfig;
    }

    private OServerSideScriptInterpreterConfig oServerSideScriptInterpreterConfig(final Config config) {
        final OServerSideScriptInterpreterConfig handlerConfig = ConfigUtils.getConfig(config, "server", "handlers", "OServerSideScriptInterpreterConfig")
                .map(pluginConfig -> {
                    return new OServerSideScriptInterpreterConfig(
                            pluginConfig.getBoolean("enabled"),
                            pluginConfig.getStringList("allowedLanguages")
                    );
                })
                .orElseGet(() -> new OServerSideScriptInterpreterConfig());
        log.logp(INFO, getClass().getName(), "oGraphServerHandlerConfig", handlerConfig.toString());
        return handlerConfig;
    }

    public String defaultNodeName() {
        final int index = HOST.indexOf('.');
        if (index == -1) {
            return HOST;
        }

        return HOST.substring(0, index);
    }
}
