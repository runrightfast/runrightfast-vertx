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
package co.runrightfast.vertx.core.verticles.verticleManager;

import co.runrightfast.vertx.core.RunRightFastVerticle;
import io.vertx.core.DeploymentOptions;
import lombok.NonNull;
import lombok.Value;

/**
 *
 * @author alfio
 */
@Value
public final class RunRightFastVerticleDeployment {

    @NonNull
    private final RunRightFastVerticle verticle;

    @NonNull
    private final DeploymentOptions deploymentOptions;

}
