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

import co.runrightfast.core.ApplicationException;
import co.runrightfast.core.application.event.AppEventLogger;
import static co.runrightfast.core.application.services.healthchecks.HealthCheckConfig.FailureSeverity.FATAL;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.protobuf.test.RunRightFastVertxApplicationTestMessage;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.VertxService.metricRegistry;
import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.core.application.RunRightFastApplication;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.eventbus.EventBusAddressMessageMapping;
import co.runrightfast.vertx.core.eventbus.InvalidMessageException;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import co.runrightfast.vertx.core.eventbus.MessageHeader;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer;
import co.runrightfast.vertx.core.modules.RunRightFastApplicationModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.utils.VertxUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheck;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import javax.inject.Singleton;
import javax.json.Json;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class RunRightFastVertxApplicationTest {

    static class TestVerticle extends RunRightFastVerticle {

        static final RunRightFastVerticleId VERTICLE_ID = RunRightFastVerticleId.builder()
                .group(RunRightFastVerticleId.RUNRIGHTFAST_GROUP)
                .name(TestVerticle.class.getSimpleName())
                .version("1.0.0")
                .build();

        @Getter
        private final RunRightFastVerticleId runRightFastVerticleId = VERTICLE_ID;

        public TestVerticle(AppEventLogger logger) {
            super(logger);
        }

        @Override
        protected void startUp() {
            registerMessageConsumer(runRightFastVertxApplicationTestMessageMessageConsumerConfig());
        }

        private MessageConsumerConfig<RunRightFastVertxApplicationTestMessage.Request, RunRightFastVertxApplicationTestMessage.Response> runRightFastVertxApplicationTestMessageMessageConsumerConfig() {
            return MessageConsumerConfig.<RunRightFastVertxApplicationTestMessage.Request, RunRightFastVertxApplicationTestMessage.Response>builder()
                    .addressMessageMapping(EventBusAddressMessageMapping.builder()
                            .address(eventBusAddress(RunRightFastVertxApplicationTestMessage.class.getSimpleName()))
                            .requestDefaultInstance(RunRightFastVertxApplicationTestMessage.Request.getDefaultInstance())
                            .responseDefaultInstance(RunRightFastVertxApplicationTestMessage.Response.getDefaultInstance())
                            .build()
                    )
                    .handler(this::handleRunRightFastVertxApplicationTestMessageRequest)
                    .addExceptionFailureMapping(IllegalArgumentException.class, MessageConsumerConfig.Failure.BAD_REQUEST)
                    .build();
        }

        private void handleRunRightFastVertxApplicationTestMessageRequest(@NonNull final Message<RunRightFastVertxApplicationTestMessage.Request> message) {
            final RunRightFastVertxApplicationTestMessage.Request request = message.body();
            info.log("RunRightFastVertxApplicationTestMessage.Request.messageConsumerHandler", () -> Json.createObjectBuilder().add("message", request.getMessage()).build());
            if (request.getMessage() != null) {
                switch (request.getMessage()) {
                    case "IllegalArgumentException":
                        throw new IllegalArgumentException();
                    case "ApplicationException":
                        throw new ApplicationException("request processing failed");
                    case "IllegalStateException":
                        throw new IllegalStateException();
                    case "InvalidMessageException":
                        throw new InvalidMessageException();
                }
            }
            reply(message, RunRightFastVertxApplicationTestMessage.Response.newBuilder()
                    .setMessage(String.format("Received message @ %s : %s", Instant.now(), request.getMessage()))
                    .build()
            );
        }

        @Override
        protected void shutDown() {
        }

        @Override
        public Set<RunRightFastHealthCheck> getHealthChecks() {
            return ImmutableSet.of(
                    healthCheck1()
            );
        }

        private RunRightFastHealthCheck healthCheck1() {
            return RunRightFastHealthCheck.builder()
                    .config(healthCheckConfigBuilder()
                            .name("healthcheck-1")
                            .severity(FATAL)
                            .build()
                    )
                    .healthCheck(new HealthCheck() {

                        @Override
                        protected HealthCheck.Result check() throws Exception {
                            return HealthCheck.Result.healthy();
                        }
                    })
                    .build();
        }

    }

    @Module
    static class RunRightFastVerticleDeploymentModule {

        @Provides(type = Provides.Type.SET)
        @Singleton
        public RunRightFastVerticleDeployment provideTestVerticleRunRightFastVerticleDeployment(final AppEventLogger logger) {
            return RunRightFastVerticleDeployment.builder()
                    .deploymentOptions(new DeploymentOptions())
                    .verticle(new TestVerticle(logger))
                    .build();
        }
    }

    @Component(
            modules = {
                RunRightFastApplicationModule.class,
                VertxServiceModule.class,
                RunRightFastVerticleDeploymentModule.class
            }
    )
    @Singleton
    public static interface TestApp extends RunRightFastVertxApplication {
    }

    private static VertxService vertxService;

    private static RunRightFastVertxApplication app;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("config.resource", String.format("%s.conf", RunRightFastVertxApplicationTest.class.getSimpleName()));
        ConfigFactory.invalidateCaches();
        metricRegistry.removeMatching(MetricFilter.ALL);
        app = DaggerRunRightFastVertxApplicationTest_TestApp.create();
        vertxService = app.vertxService();
    }

    @AfterClass
    public static void tearDownClass() {
        ServiceUtils.stop(vertxService);
    }

    @Test
    public void testRunRightFastApplication() {
        final RunRightFastApplication runrightfastApp = app.runRightFastApplication();
        final ApplicationId appId = runrightfastApp.getApplicationId();
        log.logp(INFO, getClass().getName(), "testRunRightFastApplication", appId.toString());
        assertThat(appId.getGroup(), is("co.runrightfast"));
        assertThat(appId.getName(), is("test-app"));
        assertThat(appId.getVersion(), is("1.0.0"));

        final Config config = runrightfastApp.getConfig();
        log.logp(INFO, getClass().getName(), "testRunRightFastApplication", ConfigUtils.renderConfig(config));

        log.logp(INFO, getClass().getName(), "testRunRightFastApplication", "jmxDefaultDomain = {0}", runrightfastApp.getJmxDefaultDomain());
        assertThat(runrightfastApp.getJmxDefaultDomain(), is("co.runrightfast"));
    }

    @Test
    public void test_vertx_default_options() throws InterruptedException, ExecutionException, TimeoutException {
        log.info("test_vertx_default_options");
        final Vertx vertx = vertxService.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(true));

        assertThat(vertxService.deployments().size(), is(1));
        Thread.yield();
        vertxService.deployedVerticles().entrySet().stream().forEach(entry -> {
            log.logp(INFO, getClass().getName(), "test_vertx_default_options", String.format("%s -> %s", entry.getKey(), entry.getValue().toJson()));
        });
        assertThat(vertxService.deployedVerticles().size(), is(vertxService.deployments().size()));
    }

    @Test
    public void test_eventBus_GetVerticleDeployments() throws InterruptedException, ExecutionException, TimeoutException {
        log.info("test_eventBus_GetVerticleDeployments");
        final Vertx vertx = vertxService.getVertx();

        final RunRightFastVerticleId verticleManagerId = RunRightFastVerticleManager.VERTICLE_ID;
        final CompletableFuture future = new CompletableFuture();
        final String address = EventBusAddress.eventBusAddress(verticleManagerId, "get-verticle-deployments");
        vertx.eventBus().send(
                address,
                GetVerticleDeployments.Request.newBuilder().build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, GetVerticleDeployments.Response.class)
        );
        final Object result = future.get(2000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void test_eventBus_GetVerticleDeployments_usingProtobufMessageProducer() throws InterruptedException, ExecutionException, TimeoutException {
        log.info("test_eventBus_GetVerticleDeployments");
        final Vertx vertx = vertxService.getVertx();

        final RunRightFastVerticleId verticleManagerId = RunRightFastVerticleManager.VERTICLE_ID;
        final CompletableFuture future = new CompletableFuture();
        final String address = EventBusAddress.eventBusAddress(verticleManagerId, "get-verticle-deployments");

        final ProtobufMessageProducer producer = new ProtobufMessageProducer(
                vertx.eventBus(),
                address,
                GetVerticleDeployments.Response.getDefaultInstance(), SharedMetricRegistries.getOrCreate(getClass().getSimpleName())
        );
        producer.send(
                GetVerticleDeployments.Request.newBuilder().build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, GetVerticleDeployments.Response.class)
        );
        final Object result = future.get(2000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void test_eventBus_GetVerticleDeployments_atProcessSpecificAddress() throws InterruptedException, ExecutionException, TimeoutException {
        log.info("test_eventBus_GetVerticleDeployments_atProcessSpecificAddress");
        final Vertx vertx = vertxService.getVertx();

        final RunRightFastVerticleId verticleManagerId = RunRightFastVerticleManager.VERTICLE_ID;
        final CompletableFuture future = new CompletableFuture();
        final String address = EventBusAddress.toProcessSpecificEventBusAddress(EventBusAddress.eventBusAddress(verticleManagerId, "get-verticle-deployments"));
        vertx.eventBus().send(
                address,
                GetVerticleDeployments.Request.newBuilder().build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, GetVerticleDeployments.Response.class)
        );
        final Object result = future.get(2000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void test_eventbus_RunRightFastVertxApplicationTestMessage() throws Exception {
        log.info("test_eventbus_RunRightFastVertxApplicationTestMessage");
        final Vertx vertx = vertxService.getVertx();
        final CompletableFuture future = new CompletableFuture();
        final String address = EventBusAddress.eventBusAddress(TestVerticle.VERTICLE_ID, RunRightFastVertxApplicationTestMessage.class.getSimpleName());
        vertx.eventBus().send(
                address,
                RunRightFastVertxApplicationTestMessage.Request.newBuilder().setMessage("CIAO MUNDO!!!").build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, RunRightFastVertxApplicationTestMessage.Response.class)
        );
        final Object result = future.get(2000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void test_eventbus_RunRightFastVertxApplicationTestMessage_failure() throws Exception {
        log.info("test_eventbus_RunRightFastVertxApplicationTestMessage_failure");
        final Vertx vertx = vertxService.getVertx();
        final CompletableFuture future = new CompletableFuture();
        final String address = EventBusAddress.eventBusAddress(TestVerticle.VERTICLE_ID, RunRightFastVertxApplicationTestMessage.class.getSimpleName());
        vertx.eventBus().send(
                address,
                RunRightFastVertxApplicationTestMessage.Request.newBuilder().setMessage(IllegalArgumentException.class.getSimpleName()).build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, RunRightFastVertxApplicationTestMessage.Response.class)
        );
        try {
            final Object result = future.get(2000L, TimeUnit.MILLISECONDS);
            fail("expected ReplyException");
        } catch (final ExecutionException e) {
            final ReplyException replyException = (ReplyException) e.getCause();
            assertThat(replyException.failureCode(), is(MessageConsumerConfig.Failure.BAD_REQUEST.getCode()));
        }

        vertx.eventBus().send(
                address,
                RunRightFastVertxApplicationTestMessage.Request.newBuilder().setMessage(InvalidMessageException.class.getSimpleName()).build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, RunRightFastVertxApplicationTestMessage.Response.class)
        );
        try {
            final Object result = future.get(2000L, TimeUnit.MILLISECONDS);
            fail("expected ReplyException");
        } catch (final ExecutionException e) {
            final ReplyException replyException = (ReplyException) e.getCause();
            assertThat(replyException.failureCode(), is(MessageConsumerConfig.Failure.BAD_REQUEST.getCode()));
        }
    }

    private <A extends com.google.protobuf.Message> Handler<AsyncResult<Message<A>>> responseHandler(final CompletableFuture future, final Class<A> messageType) {
        return result -> {
            if (result.succeeded()) {
                try {
                    checkState(result.result().headers().contains(MessageHeader.FROM_ADDRESS.header));
                    checkState(result.result().headers().contains(MessageHeader.MESSAGE_ID.header));
                    checkState(result.result().headers().contains(MessageHeader.MESSAGE_TIMESTAMP.header));
                    log.logp(INFO, getClass().getName(), String.format("responseHandler::%s::headers", messageType.getName()),
                            String.format("result.result().headers().contains(MessageHeader.FROM_ADDRESS.header) =  %s", result.result().headers().contains(MessageHeader.FROM_ADDRESS.header))
                    );
                    log.logp(INFO, getClass().getName(), String.format("responseHandler::%s::headers", messageType.getName()),
                            String.format("result.result().headers().contains(MessageHeader.MESSAGE_ID.header) =  %s", result.result().headers().contains(MessageHeader.MESSAGE_ID.header))
                    );
                    log.logp(INFO, getClass().getName(), String.format("responseHandler::%s::headers", messageType.getName()),
                            String.format("result.result().headers().contains(MessageHeader.MESSAGE_TIMESTAMP.header) =  %s", result.result().headers().contains(MessageHeader.MESSAGE_TIMESTAMP.header))
                    );
                    log.logp(INFO, getClass().getName(), String.format("responseHandler::%s::headers", messageType.getName()),
                            JsonUtils.toVertxJsonObject(VertxUtils.toJson(result.result().headers())).encodePrettily()
                    );
                    log.logp(INFO, getClass().getName(), String.format("responseHandler::%s::message", messageType.getName()),
                            JsonUtils.toVertxJsonObject(ProtobufUtils.protobuMessageToJson(result.result().body())).encodePrettily()
                    );

                    future.complete(result.result().body());
                } catch (final Throwable e) {
                    future.completeExceptionally(e);
                }

            } else {
                log.logp(SEVERE, getClass().getName(), String.format("responseHandler.failure::%s", messageType.getName()), "request failed", result.cause());
                future.completeExceptionally(result.cause());
            }
        };
    }
}
