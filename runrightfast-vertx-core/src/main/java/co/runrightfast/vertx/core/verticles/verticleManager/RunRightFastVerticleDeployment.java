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

import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.utils.JsonUtils.toJsonObject;
import com.codahale.metrics.MetricRegistry;
import static com.google.common.base.Preconditions.checkArgument;
import io.vertx.core.DeploymentOptions;
import java.util.Set;
import java.util.function.Supplier;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@EqualsAndHashCode(of = "runRightFastVerticleId")
public final class RunRightFastVerticleDeployment {

    @Getter
    private final Supplier<RunRightFastVerticle> verticle;

    @Getter
    private final Class<? extends RunRightFastVerticle> verticleClass;

    @Getter
    private final DeploymentOptions deploymentOptions;

    @Getter
    private final RunRightFastVerticle verticleInstance;

    @Getter
    private final RunRightFastVerticleId runRightFastVerticleId;

    public RunRightFastVerticleDeployment(@NonNull final Supplier<RunRightFastVerticle> verticle, @NonNull final Class<? extends RunRightFastVerticle> verticleClass, final @NonNull DeploymentOptions deploymentOptions) {
        this.verticle = verticle;
        this.verticleClass = verticleClass;
        this.deploymentOptions = deploymentOptions;
        this.verticleInstance = verticle.get();
        this.runRightFastVerticleId = verticleInstance.getRunRightFastVerticleId();
        validate();
    }

    public RunRightFastVerticleDeployment(@NonNull final Supplier<RunRightFastVerticle> verticle, @NonNull final Class<? extends RunRightFastVerticle> verticleClass, @NonNull final DeploymentOptions deploymentOptions, @NonNull final RunRightFastVerticle verticleInstance) {
        this.verticle = verticle;
        this.verticleClass = verticleClass;
        this.deploymentOptions = deploymentOptions;
        this.verticleInstance = verticleInstance;
        this.runRightFastVerticleId = verticleInstance.getRunRightFastVerticleId();
        validate();
    }

    public RunRightFastVerticleDeployment withNewVerticleInstance() {
        return new RunRightFastVerticleDeployment(verticle, verticleClass, deploymentOptions, verticle.get());

    }

    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("verticleClass", verticle.getClass().getName())
                .add("verticleId", verticleInstance.getRunRightFastVerticleId().toJson())
                .add("deploymentOptions", toJsonObject(deploymentOptions.toJson()))
                .build();
    }

    public void validate() {
        final String errorMessage = "Can't specify %s for already created verticle";
        checkArgument(deploymentOptions.getExtraClasspath() == null, errorMessage, "extraClasspath");
        checkArgument(deploymentOptions.getIsolationGroup() == null, errorMessage, "isolationGroup");
        checkArgument(deploymentOptions.getIsolatedClasses() == null, errorMessage, "isolatedClasses");
    }

    public Set<RunRightFastHealthCheck> getHealthChecks() {
        return verticleInstance.getHealthChecks();
    }

    public MetricRegistry getMetricRegistry() {
        return verticleInstance.getMetricRegistry();
    }
}
