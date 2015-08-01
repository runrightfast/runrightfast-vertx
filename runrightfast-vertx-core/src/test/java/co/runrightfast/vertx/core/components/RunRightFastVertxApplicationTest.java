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

import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.VertxService.metricRegistry;
import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.core.application.RunRightFastApplication;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.modules.ApplicationConfigModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import com.codahale.metrics.MetricFilter;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class RunRightFastVertxApplicationTest {

    static class TestVerticle extends RunRightFastVerticle {

        @Getter
        private final RunRightFastVerticleId runRightFastVerticleId
                = RunRightFastVerticleId.builder()
                .group(RunRightFastVerticleId.RUNRIGHTFAST_GROUP)
                .name(getClass().getSimpleName())
                .version("1.0.0")
                .build();

        @Override
        protected void startUp() {
        }

        @Override
        protected void shutDown() {
        }

    }

    @Module
    static class RunRightFastVerticleDeploymentModule {

        @Provides(type = Provides.Type.SET)
        @Singleton
        public RunRightFastVerticleDeployment provideTestVerticleRunRightFastVerticleDeployment() {
            return RunRightFastVerticleDeployment.builder()
                    .deploymentOptions(new DeploymentOptions())
                    .verticle(new TestVerticle())
                    .build();
        }
    }

    @Component(
            modules = {
                ApplicationConfigModule.class,
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
                getVerticleDeploymentsResponseHandler(future)
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
                getVerticleDeploymentsResponseHandler(future)
        );
        final Object result = future.get(2000L, TimeUnit.MILLISECONDS);
    }

    private Handler<AsyncResult<Message<GetVerticleDeployments.Response>>> getVerticleDeploymentsResponseHandler(final CompletableFuture future) {
        return result -> {
            if (result.succeeded()) {
                log.logp(INFO, getClass().getName(), "test_vertx_default_options.success",
                        JsonUtils.toVertxJsonObject(ProtobufUtils.protobuMessageToJson(result.result().body())).encodePrettily()
                );
                future.complete(result.result().body());
            } else {
                log.logp(SEVERE, getClass().getName(), "test_vertx_default_options.failure", "get-verticle-deployments failed", result.cause());
                future.completeExceptionally(result.cause());
            }
        };
    }
}
