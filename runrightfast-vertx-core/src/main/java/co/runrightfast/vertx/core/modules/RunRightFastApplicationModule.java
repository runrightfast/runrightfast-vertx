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
import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.core.application.services.healthchecks.HealthChecksService;
import co.runrightfast.core.application.services.healthchecks.impl.HealthChecksServiceImpl;
import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.core.application.RunRightFastApplication;
import co.runrightfast.vertx.core.inject.qualifiers.ApplicationConfig;
import co.runrightfast.vertx.core.inject.qualifiers.VertxServiceConfig;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import static co.runrightfast.vertx.core.utils.ConfigUtils.configPath;
import static co.runrightfast.vertx.core.utils.ConfigUtils.loadConfig;
import static co.runrightfast.vertx.core.utils.JmxUtils.applicationJmxDomain;
import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 *
 * @author alfio
 */
@Module
public final class RunRightFastApplicationModule {

    @Provides
    @Singleton
    @ApplicationConfig
    public Config provideApplicationConfig() {
        return loadConfig(true);
    }

    /**
     * Expects the vertx config to be located at path: runrightfast.vertx
     *
     * @param config app config
     * @return vertx config
     */
    @Provides
    @Singleton
    @VertxServiceConfig
    @SuppressWarnings({"ThrowableInstanceNotThrown", "ThrowableInstanceNeverThrown"})
    public Config provideVertxServiceConfig(@ApplicationConfig final Config config) {
        final String configPath = configPath(CONFIG_NAMESPACE, "vertx");
        return ConfigUtils.getConfig(config, configPath)
                .orElseThrow(() -> new ConfigurationException(String.format("Missing required config: %s", configPath)));
    }

    @Provides
    @Singleton
    public ApplicationId provideApplicationId(@ApplicationConfig final Config config) {
        final Config appConfig = ConfigUtils.getConfig(config, CONFIG_NAMESPACE)
                .orElseThrow(() -> new ConfigurationException(String.format("Missing required config: %s", CONFIG_NAMESPACE)));
        return ApplicationId.fromConfig(appConfig);
    }

    @Provides
    @Singleton
    public RunRightFastApplication provideRunRightFastApplication(final ApplicationId applicationId, @ApplicationConfig final Config config) {
        return RunRightFastApplication.builder().applicationId(applicationId).config(config).build();
    }

    @Provides
    @Singleton
    public AppEventLogger provideAppEventLogger(final ApplicationId applicationId) {
        return new AppEventJDKLogger(applicationId);
    }

    @Provides
    @Singleton
    public HealthChecksService provideHealthChecksService(final AppEventLogger appEventLogger, @ApplicationConfig final Config config) {
        return new HealthChecksServiceImpl(appEventLogger, applicationJmxDomain(config));
    }

}
