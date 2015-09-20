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
package co.runrightfast.vertx.core.protobuf;

import co.runrightfast.core.application.services.healthchecks.HealthCheckConfig;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.verticles.messages.VerticleId;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.DeploymentOptions;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.HealthCheck;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.VerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.VerticleType;
import java.util.Collection;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alfio
 */
public interface MessageConversions {

    static VerticleDeployment toVerticleDeployment(@NonNull final RunRightFastVerticleDeployment deployment, @NonNull final Set<String> deploymentIds) {
        final VerticleDeployment.Builder builder = VerticleDeployment.newBuilder()
                .setVerticleClass(deployment.getVerticleClass().getName())
                .setVerticleId(toVerticleId(deployment.getRunRightFastVerticleId()))
                .setDeploymentOptions(toDeploymentOptions(deployment.getDeploymentOptions()));

        final Set<RunRightFastHealthCheck> healthChecks = deployment.getHealthChecks();
        if (CollectionUtils.isNotEmpty(healthChecks)) {
            healthChecks.stream()
                    .map(healthCheck -> toHealthCheck(healthCheck, builder.getVerticleId()))
                    .forEach(builder::addHealthChecks);
        }

        if (CollectionUtils.isNotEmpty(deploymentIds)) {
            builder.addAllDeploymentIds(deploymentIds);
        }

        return builder.build();
    }

    static HealthCheck toHealthCheck(@NonNull final RunRightFastHealthCheck healthCheck, final VerticleId verticleId) {
        final HealthCheckConfig config = healthCheck.getConfig();
        final HealthCheck.Builder builder = HealthCheck.newBuilder()
                .setVerticleId(verticleId)
                .setHealthCheckName(config.getName())
                .setHealthCheckClass(healthCheck.getHealthCheck().getClass().getName())
                .setFailureSeverity(toFailureSeverity(config.getSeverity()));
        if (CollectionUtils.isNotEmpty(config.getTags())) {
            config.getTags().stream().forEach(builder::addTags);
        }
        return builder.build();
    }

    static HealthCheck.FailureSeverity toFailureSeverity(@NonNull final HealthCheckConfig.FailureSeverity severity) {
        switch (severity) {
            case LOW:
                return HealthCheck.FailureSeverity.LOW;
            case MEDIUM:
                return HealthCheck.FailureSeverity.MEDIUM;
            case HIGH:
                return HealthCheck.FailureSeverity.HIGH;
            case FATAL:
                return HealthCheck.FailureSeverity.FATAL;
            default:
                throw new IllegalArgumentException("Uknown severity : " + severity.name());
        }
    }

    static VerticleId toVerticleId(@NonNull final RunRightFastVerticleId id) {
        return VerticleId.newBuilder()
                .setGroup(id.getGroup())
                .setName(id.getName())
                .setVersion(id.getVersion())
                .build();
    }

    static DeploymentOptions toDeploymentOptions(@NonNull final io.vertx.core.DeploymentOptions options) {
        final DeploymentOptions.Builder builder = DeploymentOptions.newBuilder();
        if (options.getConfig() != null) {
            builder.setConfigJson(options.getConfig().encode());
        }
        if (CollectionUtils.isNotEmpty(options.getExtraClasspath())) {
            options.getExtraClasspath().stream().forEach(builder::addExtraClasspath);
        }
        if (CollectionUtils.isNotEmpty(options.getIsolatedClasses())) {
            options.getIsolatedClasses().stream().forEach(builder::addIsolatedClasses);
        }
        if (StringUtils.isNotBlank(options.getIsolationGroup())) {
            builder.setIsolationGroup(options.getIsolationGroup());
        }
        builder.setHa(options.isHa());
        builder.setInstances(options.getInstances());
        builder.setVerticleType(getVerticleType(options));
        return builder.build();
    }

    static VerticleType getVerticleType(@NonNull final io.vertx.core.DeploymentOptions options) {
        if (options.isWorker()) {
            return VerticleType.MULTITHREADED;
        }

        if (options.isMultiThreaded()) {
            return VerticleType.MULTITHREADED;
        }

        return VerticleType.STANDARD;
    }

    static JsonArray toJsonArray(final Collection<RunRightFastVerticleDeployment> deployments) {
        if (deployments == null) {
            return JsonUtils.EMPTY_ARRAY;
        }

        final JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        deployments.stream().forEach(deployment -> jsonArray.add(deployment.toJson()));
        return jsonArray.build();
    }

}
