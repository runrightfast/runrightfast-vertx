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
package co.runrightfast.vertx.orientdb.modules;

import co.runrightfast.vertx.core.inject.qualifiers.ApplicationConfig;
import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import static co.runrightfast.vertx.core.utils.ConfigUtils.configPath;
import co.runrightfast.vertx.orientdb.config.OrientDBConfig;
import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 *
 * @author alfio
 */
@Module
public final class OrientDBConfigModule {

    @Provides
    @Singleton
    @OrientDBConfig.OrientDBConfiguration
    public Config provideOrientDBConfiguration(@ApplicationConfig final Config config) {
        return config.getConfig(configPath(CONFIG_NAMESPACE, "orientdb"));
    }

    @Provides
    @Singleton
    public OrientDBConfig provideOrientDBConfig(@OrientDBConfig.OrientDBConfiguration final Config config) {
        return new OrientDBConfig(config);
    }

}
