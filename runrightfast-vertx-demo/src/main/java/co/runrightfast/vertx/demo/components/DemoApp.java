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
package co.runrightfast.vertx.demo.components;

import co.runrightfast.vertx.core.components.RunRightFastVertxApplication;
import co.runrightfast.vertx.core.modules.CryptographyModule;
import co.runrightfast.vertx.core.modules.RunRightFastApplicationModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.vertx.demo.modules.EncryptionServiceModule;
import co.runrightfast.vertx.demo.modules.OrientDBModule;
import co.runrightfast.vertx.demo.modules.RunRightFastVerticleDeploymentModule;
import co.runrightfast.vertx.orientdb.modules.OrientDBVerticleDeploymentModule;
import dagger.Component;
import javax.inject.Singleton;

/**
 *
 * @author alfio
 */
@Component(
        modules = {
            RunRightFastApplicationModule.class,
            VertxServiceModule.class,
            RunRightFastVerticleDeploymentModule.class,
            EncryptionServiceModule.class,
            CryptographyModule.class,
            OrientDBModule.class,
            OrientDBVerticleDeploymentModule.class
        }
)
@Singleton
public interface DemoApp extends RunRightFastVertxApplication {

}
