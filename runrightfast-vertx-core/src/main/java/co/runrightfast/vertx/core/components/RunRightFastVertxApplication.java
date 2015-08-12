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
package co.runrightfast.vertx.core.components;

import co.runrightfast.core.application.services.healthchecks.HealthChecksService;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.VertxService;
import co.runrightfast.vertx.core.application.RunRightFastApplication;

/**
 * Applications extend this component interface and include the appropriate modules to provide the VertxService
 *
 * @author alfio
 */
public interface RunRightFastVertxApplication {

    RunRightFastApplication runRightFastApplication();

    VertxService vertxService();

    HealthChecksService healthChecksService();

    /**
     * Use cases:
     *
     * <ol>
     * <li>To secure EventBus messages by encrypting the messages.
     * </ol>
     *
     * @return
     */
    EncryptionService encryptionService();
}
