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
package co.runrightfast.vertx.core.modules;

import co.runrightfast.core.ConfigurationException;
import co.runrightfast.vertx.core.inject.qualifiers.ApplicationConfig;
import co.runrightfast.vertx.core.inject.qualifiers.VertxServiceConfig;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import static co.runrightfast.vertx.core.utils.ConfigUtils.configPath;
import static co.runrightfast.vertx.core.utils.ConfigUtils.loadConfig;
import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 *
 * @author alfio
 */
@Module
public final class ApplicationConfigModule {

    @Provides
    @Singleton
    @ApplicationConfig
    public Config provideApplicationConfig() {
        return loadConfig(true);
    }

    /**
     * Expects the vertx config to be located at path: runrightfast.vertx
     *
     * @param appConfig app config
     * @return vertx config
     */
    @Provides
    @Singleton
    @VertxServiceConfig
    @SuppressWarnings({"ThrowableInstanceNotThrown", "ThrowableInstanceNeverThrown"})
    public Config provideVertxServiceConfig(@ApplicationConfig final Config appConfig) {
        final String configPath = configPath(CONFIG_NAMESPACE, "vertx");
        return ConfigUtils.getConfig(appConfig, configPath)
                .orElseThrow(() -> new ConfigurationException(String.format("Missing required config: %s", configPath)));
    }

}
