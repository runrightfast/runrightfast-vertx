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
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.RunRightFastVerticleId.RUNRIGHTFAST_GROUP;
import co.runrightfast.vertx.core.eventbus.EventBusAddressMessageMapping;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.vertx.core.eventbus.Message;
import java.util.Set;
import java.util.logging.Level;
import javax.inject.Inject;
import lombok.Getter;
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
            .name(RunRightFastVerticleManager.class.getSimpleName())
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

    @Inject
    public RunRightFastVerticleManager(final Set<RunRightFastVerticleDeployment> deployments) {
        super();
        checkArgument(CollectionUtils.isNotEmpty(deployments));
        this.deployments = ImmutableSet.copyOf(deployments);
    }

    @Override
    protected void startUp() {
        deployments.stream().forEach(this::deployVerticle);
        registerGetVerticleDeploymentsMessageConsumer();
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

    private void handleGetVerticleDeploymentsMessage(final Message<GetVerticleDeployments.Request> request) {
        // TODO : process GetVerticleDeployments request
        final GetVerticleDeployments.Response response = GetVerticleDeployments.Response.newBuilder()
                .build();

        request.reply(response);
    }

    @Override
    protected void shutDown() {
    }

    /**
     * The verticle is deployed asynchronously
     *
     * @param deployment config
     */
    private void deployVerticle(final RunRightFastVerticleDeployment deployment) {
        vertx.deployVerticle(deployment.getVerticle(), deployment.getDeploymentOptions(), result -> {
            if (result.succeeded()) {
                final ImmutableMap.Builder<String, RunRightFastVerticleDeployment> mapBuilder = ImmutableMap.builder();
                mapBuilder.putAll(deployedVerticles);
                mapBuilder.put(result.result(), deployment);
                this.deployedVerticles = mapBuilder.build();
                log.logp(Level.INFO, CLASS_NAME, "deployVerticle", () -> deployment.toJson().toString());
            } else {
                log.logp(Level.SEVERE, CLASS_NAME, "deployVerticle", result.cause(), () -> deployment.toJson().toString());
                // TODO: raise an alert if the deployment fails
            }
        });
    }

}
