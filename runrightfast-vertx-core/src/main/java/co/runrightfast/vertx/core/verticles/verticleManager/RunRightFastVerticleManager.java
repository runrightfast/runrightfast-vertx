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

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.RunRightFastVerticleId.RUNRIGHTFAST_GROUP;
import co.runrightfast.vertx.core.eventbus.EventBusAddressMessageMapping;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import co.runrightfast.vertx.core.protobuf.MessageConversions;
import static co.runrightfast.vertx.core.utils.JmxUtils.verticleJmxDomain;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import com.codahale.metrics.JmxReporter;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.vertx.core.eventbus.Message;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.inject.Inject;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;

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

    private JmxReporter jmxReporter;

    @Inject
    public RunRightFastVerticleManager(final AppEventLogger appEventLogger, final Set<RunRightFastVerticleDeployment> deployments) {
        super(appEventLogger);
        checkArgument(CollectionUtils.isNotEmpty(deployments));
        this.deployments = ImmutableSet.copyOf(deployments);
    }

    @Override
    protected void startUp() {
        deployments.stream().forEach(this::deployVerticle);
        registerGetVerticleDeploymentsMessageConsumer();
        startJmxReporterForSelf();
    }

    private void registerGetVerticleDeploymentsMessageConsumer() {
        registerMessageConsumer(MessageConsumerConfig.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .addressMessageMapping(EventBusAddressMessageMapping.builder()
                        .address(eventBusAddress("get-verticle-deployments"))
                        .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                        .responseDefaultInstance(GetVerticleDeployments.Response.getDefaultInstance())
                        .build()
                ).handler(this::handleGetVerticleDeploymentsMessage)
                .build()
        );
    }

    private void handleGetVerticleDeploymentsMessage(@NonNull final Message<GetVerticleDeployments.Request> message) {
        final GetVerticleDeployments.Response.Builder response = GetVerticleDeployments.Response.newBuilder();
        final GetVerticleDeployments.Request request = message.body();
        if (hasFilters(request)) {
            deployments.stream()
                    .filter(deployment -> {
                        final RunRightFastVerticleId id = deployment.getVerticle().getRunRightFastVerticleId();
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
        vertx.deployVerticle(deployment.getVerticle(), deployment.getDeploymentOptions(), result -> {
            if (result.succeeded()) {
                this.deployedVerticles = ImmutableMap.<String, RunRightFastVerticleDeployment>builder()
                        .putAll(deployedVerticles)
                        .put(result.result(), deployment)
                        .build();
                registerJmxReporter(deployment);
                log.logp(Level.INFO, CLASS_NAME, "deployVerticle", () -> deployment.toJson().toString());
            } else {
                log.logp(Level.SEVERE, CLASS_NAME, "deployVerticle", result.cause(), () -> deployment.toJson().toString());
                // TODO: raise an alert if the deployment fails
            }
        });
    }

    private void registerJmxReporter(@NonNull final RunRightFastVerticleDeployment deployment) {
        if (verticleJmxReporters.containsKey(deployment)) {
            return;
        }

        final RunRightFastVerticle verticle = deployment.getVerticle();
        final RunRightFastVerticleId verticleId = verticle.getRunRightFastVerticleId();
        final JmxReporter jmxReporter = JmxReporter.forRegistry(verticle.getMetricRegistry())
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
        if (jmxReporter != null) {
            return;
        }
        jmxReporter = JmxReporter.forRegistry(this.metricRegistry)
                .inDomain(verticleJmxDomain(runRightFastVerticleId, "metrics"))
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build();
        jmxReporter.start();
    }

    private void stopJmxReporterForSelf() {
        if (jmxReporter == null) {
            return;
        }
        jmxReporter.stop();
        jmxReporter.close();
        jmxReporter = null;
    }

    @Override
    public Set<RunRightFastHealthCheck> getHealthChecks() {
        return ImmutableSet.of();
    }

}
