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

import co.runrightfast.vertx.core.VertxService;
import co.runrightfast.vertx.core.impl.VertxServiceImpl;
import co.runrightfast.vertx.core.inject.qualifiers.VertxServiceConfig;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;

/**
 *
 * @author alfio
 */
@Module
public class VertxServiceModule {

    @Provides
    public VertxService vertxService(@VertxServiceConfig final Config config) {
        final VertxService service = new VertxServiceImpl(config);
        ServiceUtils.start(service);
        return service;
    }
}
