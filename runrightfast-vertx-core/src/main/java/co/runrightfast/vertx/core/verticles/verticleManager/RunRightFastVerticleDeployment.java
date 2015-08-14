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
import static co.runrightfast.vertx.core.utils.JsonUtils.toJsonObject;
import static com.google.common.base.Preconditions.checkArgument;
import io.vertx.core.DeploymentOptions;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

/**
 *
 * @author alfio
 */
@Value
@Builder
@EqualsAndHashCode(of = "verticle")
public final class RunRightFastVerticleDeployment {

    @NonNull
    private final RunRightFastVerticle verticle;

    @NonNull
    private final DeploymentOptions deploymentOptions;

    public RunRightFastVerticleDeployment(@NonNull final RunRightFastVerticle verticle, @NonNull DeploymentOptions deploymentOptions) {
        this.verticle = verticle;
        this.deploymentOptions = deploymentOptions;
        validate();
    }

    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("verticleClass", verticle.getClass().getName())
                .add("verticleId", verticle.getRunRightFastVerticleId().toJson())
                .add("deploymentOptions", toJsonObject(deploymentOptions.toJson()))
                .build();
    }

    public void validate() {
        checkArgument(deploymentOptions.getExtraClasspath() == null, "Can't specify extraClasspath for already created verticle");
        checkArgument(deploymentOptions.getIsolationGroup() == null, "Can't specify isolationGroup for already created verticle");
        checkArgument(deploymentOptions.getIsolatedClasses() == null, "Can't specify isolatedClasses for already created verticle");
    }
}
