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

import co.runrightfast.core.application.event.AppEvent;
import co.runrightfast.core.application.event.AppEventLogger;
import static co.runrightfast.core.application.event.ApplicationEvents.VERTICLE_DEPLOYMENT_FAILED;
import static co.runrightfast.core.application.event.ApplicationEvents.VERTICLE_DEPLOYMENT_SUCCESS;
import co.runrightfast.core.application.services.healthchecks.HealthCheckConfig;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.RunRightFastVerticleId.RUNRIGHTFAST_GROUP;
import co.runrightfast.vertx.core.eventbus.EventBusAddressMessageMapping;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import co.runrightfast.vertx.core.protobuf.MessageConversions;
import static co.runrightfast.vertx.core.protobuf.MessageConversions.toVerticleId;
import static co.runrightfast.vertx.core.utils.JmxUtils.verticleJmxDomain;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.HealthCheckResult;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.RunVerticleHealthChecks;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.VerticleId;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.health.HealthCheck;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.Message;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alfio
 */
@Log
public final class RunRightFastVerticleManager extends RunRightFastVerticle {

    public static final RunRightFastVerticleId VERTICLE_ID = RunRightFastVerticleId.builder()
            .group(RUNRIGHTFAST_GROUP)
            .name("verticle-manager")
            .version("0.1")
            .build();

    @Getter
    private final RunRightFastVerticleId runRightFastVerticleId = VERTICLE_ID;

    @Getter
    private final Set<RunRightFastVerticleDeployment> deployments;

    /**
     * Maps the Vertx verticle deployment id to the RunRightFastVerticleDeployment
     */
    @Getter
    private ImmutableMap<String, RunRightFastVerticleDeployment> deployedVerticles = ImmutableMap.of();

    private ImmutableMap<RunRightFastVerticleDeployment, JmxReporter> verticleJmxReporters = ImmutableMap.of();

    private JmxReporter jmxReporterForSelf;

    @Inject
    public RunRightFastVerticleManager(final AppEventLogger appEventLogger, final EncryptionService encryptionService, final Set<RunRightFastVerticleDeployment> deployments) {
        super(appEventLogger, encryptionService);
        checkArgument(CollectionUtils.isNotEmpty(deployments));
        this.deployments = ImmutableSet.copyOf(deployments);
    }

    @Override
    protected void startUp() {
        deployments.stream().forEach(this::deployVerticle);
        registerGetVerticleDeploymentsMessageConsumer();
        registerRunVerticleHealthChecksMessageConsumer();
        startJmxReporterForSelf();
    }

    private void registerRunVerticleHealthChecksMessageConsumer() {
        registerMessageConsumer(MessageConsumerConfig.<RunVerticleHealthChecks.Request, RunVerticleHealthChecks.Response>builder()
                .addressMessageMapping(EventBusAddressMessageMapping.builder()
                        .address(eventBusAddress("run-verticle-healthchecks"))
                        .requestDefaultInstance(RunVerticleHealthChecks.Request.getDefaultInstance())
                        .responseDefaultInstance(RunVerticleHealthChecks.Response.getDefaultInstance())
                        .build()
                ).handler(this::handleRunVerticleHealthChecksMessage)
                .ciphers(cipherFunctions(RunVerticleHealthChecks.getDefaultInstance()))
                .build()
        );
    }

    private void handleRunVerticleHealthChecksMessage(@NonNull final Message<RunVerticleHealthChecks.Request> message) {
        final RunVerticleHealthChecks.Response.Builder response = RunVerticleHealthChecks.Response.newBuilder();
        final RunVerticleHealthChecks.Request request = message.body();
        if (hasFilters(request)) {
            deployments.stream()
                    .filter(deployment -> {
                        final RunRightFastVerticleId id = deployment.getRunRightFastVerticleId();
                        if (CollectionUtils.isEmpty(deployment.getHealthChecks())) {
                            return false;
                        }
                        return request.getGroupsList().stream().filter(group -> group.equals(id.getGroup())).findFirst().isPresent()
                        || request.getNamesList().stream().filter(name -> name.equals(id.getName())).findFirst().isPresent()
                        || request.getVerticleIdsList().stream().filter(id::equalsVerticleId).findFirst().isPresent();
                    })
                    .map(this::runVerticleHealthChecks)
                    .forEach(response::addAllResults);
        } else {
            deployments.stream()
                    .map(this::runVerticleHealthChecks)
                    .forEach(response::addAllResults);
        }

        reply(message, response.build());
    }

    private List<HealthCheckResult> runVerticleHealthChecks(final RunRightFastVerticleDeployment deployment) {
        final VerticleId verticleId = toVerticleId(deployment.getRunRightFastVerticleId());
        final Set<RunRightFastHealthCheck> healthChecks = deployment.getHealthChecks();
        return healthChecks.stream().map(healthCheck -> {
            final HealthCheckResult.Builder result = HealthCheckResult.newBuilder();
            final HealthCheckConfig config = healthCheck.getConfig();
            final HealthCheck.Result healthCheckResult = healthCheck.getHealthCheck().execute();
            result.setHealthCheckName(config.getName());
            result.setHealthy(healthCheckResult.isHealthy());
            if (StringUtils.isNotBlank(healthCheckResult.getMessage())) {
                result.setMessage(healthCheckResult.getMessage());
            }
            result.setVerticleId(verticleId);
            return result.build();
        }).collect(Collectors.toList());
    }

    private void registerGetVerticleDeploymentsMessageConsumer() {
        registerMessageConsumer(MessageConsumerConfig.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .addressMessageMapping(EventBusAddressMessageMapping.builder()
                        .address(eventBusAddress("get-verticle-deployments"))
                        .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                        .responseDefaultInstance(GetVerticleDeployments.Response.getDefaultInstance())
                        .build()
                ).handler(this::handleGetVerticleDeploymentsMessage)
                .ciphers(cipherFunctions(GetVerticleDeployments.getDefaultInstance()))
                .build()
        );
    }

    private void handleGetVerticleDeploymentsMessage(@NonNull final Message<GetVerticleDeployments.Request> message) {
        final GetVerticleDeployments.Response.Builder response = GetVerticleDeployments.Response.newBuilder();
        final GetVerticleDeployments.Request request = message.body();
        if (hasFilters(request)) {
            deployments.stream()
                    .filter(deployment -> {
                        final RunRightFastVerticleId id = deployment.getRunRightFastVerticleId();
                        return request.getGroupsList().stream().filter(group -> group.equals(id.getGroup())).findFirst().isPresent()
                        || request.getNamesList().stream().filter(name -> name.equals(id.getName())).findFirst().isPresent()
                        || request.getVerticleIdsList().stream().filter(id::equalsVerticleId).findFirst().isPresent();
                    })
                    .map(MessageConversions::toVerticleDeployment)
                    .forEach(response::addDeployments);
        } else {
            deployments.stream()
                    .map(MessageConversions::toVerticleDeployment)
                    .forEach(response::addDeployments);
        }

        reply(message, response.build());
    }

    private boolean hasFilters(@NonNull final GetVerticleDeployments.Request request) {
        return request.getVerticleIdsCount() > 0 || request.getGroupsCount() > 0 || request.getNamesCount() > 0;
    }

    private boolean hasFilters(@NonNull final RunVerticleHealthChecks.Request request) {
        return request.getVerticleIdsCount() > 0 || request.getGroupsCount() > 0 || request.getNamesCount() > 0;
    }

    @Override
    protected void shutDown() {
        verticleJmxReporters.values().forEach(reporter -> {
            reporter.stop();
            reporter.close();
        });
        stopJmxReporterForSelf();
    }

    /**
     * The verticle is deployed asynchronously
     *
     * @param deployment config
     */
    private void deployVerticle(@NonNull final RunRightFastVerticleDeployment deployment) {
        if (deployment.getDeploymentOptions().getInstances() == 1) {
            deployVerticleInstance(deployment, deployment.getVerticleInstance(), deployment.getDeploymentOptions());
        } else {
            final DeploymentOptions deploymentOptions = new DeploymentOptions(deployment.getDeploymentOptions()).setInstances(1);
            for (int i = 0; i < deployment.getDeploymentOptions().getInstances(); i++) {
                final RunRightFastVerticleDeployment deploymentInstance = deployment.withNewVerticleInstance();
                if (i == 0) {
                    deployVerticleInstance(deployment, deployment.getVerticleInstance(), deploymentOptions);
                } else {
                    deployVerticleInstance(deploymentInstance, deploymentInstance.getVerticleInstance(), deploymentOptions);
                }
            }
        }
    }

    private void deployVerticleInstance(@NonNull final RunRightFastVerticleDeployment deployment, @NonNull final RunRightFastVerticle verticle, final DeploymentOptions deploymentOptions) {
        vertx.deployVerticle(verticle, deploymentOptions, result -> {
            if (result.succeeded()) {
                this.deployedVerticles = ImmutableMap.<String, RunRightFastVerticleDeployment>builder()
                        .putAll(deployedVerticles)
                        .put(result.result(), deployment)
                        .build();
                registerJmxReporter(deployment);
                appEventLogger.accept(AppEvent.info(VERTICLE_DEPLOYMENT_SUCCESS)
                        .setVerticleId(VERTICLE_ID)
                        .setData(deployment)
                        .build()
                );
            } else {
                appEventLogger.accept(AppEvent.error(VERTICLE_DEPLOYMENT_FAILED)
                        .setVerticleId(VERTICLE_ID)
                        .setData(deployment)
                        .setException(result.cause())
                        .build()
                );
            }
        });
    }

    private void registerJmxReporter(@NonNull final RunRightFastVerticleDeployment deployment) {
        if (verticleJmxReporters.containsKey(deployment)) {
            return;
        }

        final RunRightFastVerticleId verticleId = deployment.getRunRightFastVerticleId();
        final JmxReporter jmxReporter = JmxReporter.forRegistry(deployment.getMetricRegistry())
                .inDomain(verticleJmxDomain(verticleId, "metrics"))
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build();
        jmxReporter.start();
        verticleJmxReporters = ImmutableMap.<RunRightFastVerticleDeployment, JmxReporter>builder()
                .putAll(verticleJmxReporters)
                .put(deployment, jmxReporter)
                .build();
    }

    private void startJmxReporterForSelf() {
        if (jmxReporterForSelf != null) {
            return;
        }
        jmxReporterForSelf = JmxReporter.forRegistry(this.metricRegistry)
                .inDomain(verticleJmxDomain(runRightFastVerticleId, "metrics"))
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build();
        jmxReporterForSelf.start();
    }

    private void stopJmxReporterForSelf() {
        if (jmxReporterForSelf == null) {
            return;
        }
        jmxReporterForSelf.stop();
        jmxReporterForSelf.close();
        jmxReporterForSelf = null;
    }

    @Override
    public Set<RunRightFastHealthCheck> getHealthChecks() {
        return ImmutableSet.of();
    }

}
