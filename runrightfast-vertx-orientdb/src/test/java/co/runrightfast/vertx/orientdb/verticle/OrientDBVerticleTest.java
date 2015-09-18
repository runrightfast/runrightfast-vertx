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
package co.runrightfast.vertx.orientdb.verticle;

import co.runrightfast.core.TypeSafeObjectRegistry;
import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.VertxService.metricRegistry;
import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.core.components.RunRightFastVertxApplication;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.eventbus.MessageHeader;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageCodec;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer;
import static co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer.addRunRightFastHeaders;
import co.runrightfast.vertx.core.modules.RunRightFastApplicationModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import static co.runrightfast.vertx.core.utils.ConcurrencyUtils.sleep;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.utils.VertxUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.RunVerticleHealthChecks;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.VerticleDeployment;
import co.runrightfast.vertx.orientdb.OrientDBPoolConfig;
import co.runrightfast.vertx.orientdb.OrientDBService;
import co.runrightfast.vertx.orientdb.config.OrientDBConfig;
import co.runrightfast.vertx.orientdb.hooks.SetCreatedOnAndUpdatedOn;
import co.runrightfast.vertx.orientdb.impl.embedded.EmbeddedOrientDBServiceConfig;
import co.runrightfast.vertx.orientdb.lifecycle.RunRightFastOrientDBLifeCycleListener;
import co.runrightfast.vertx.orientdb.modules.OrientDBVerticleWithRepositoriesDeploymentModule;
import co.runrightfast.vertx.orientdb.utils.OrientDBUtils;
import co.runrightfast.vertx.testSupport.EncryptionServiceWithDefaultCiphers;
import com.codahale.metrics.MetricFilter;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.client.remote.OServerAdmin;
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
import static io.vertx.core.eventbus.ReplyFailure.NO_HANDLERS;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.co.runrightfast.vertx.orientdb.classes.EventLogRecord;
import test.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.CreateEvent;
import test.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.GetEventCount;

/**
 *
 * @author alfio
 */
@Log
public class OrientDBVerticleTest {

    static final String CLASS_NAME = OrientDBVerticleTest.class.getSimpleName();

    private final static EncryptionService encryptionService = new EncryptionServiceWithDefaultCiphers();

    private static final File orientdbHome = new File("build/temp/OrientDBVerticleTest/orientdb");

    static {
        System.setProperty("config.resource", String.format("%s.conf", CLASS_NAME));
        //System.setProperty("runrightfast.orientdb.client.ssl.enabled", "false");
        ConfigFactory.invalidateCaches();

        orientdbHome.mkdirs();
        try {
            FileUtils.cleanDirectory(orientdbHome);
            FileUtils.deleteDirectory(orientdbHome);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        log.logp(INFO, CLASS_NAME, "setUpClass", String.format("orientdbHome=%s .exists=%s", orientdbHome, orientdbHome.exists()));

        final File configDirSrc = new File("src/test/resources/orientdb/config");
        final File configDirTarget = new File(orientdbHome, "config");
        try {
            FileUtils.copyFileToDirectory(new File(configDirSrc, "default-distributed-db-config.json"), configDirTarget);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final File configCertDirSrc = new File("src/test/resources/orientdb/config/cert");
        final File configCertDirTarget = new File(orientdbHome, "config/cert");
        try {
            FileUtils.copyDirectory(configCertDirSrc, configCertDirTarget);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Module
    static class RunRightFastVerticleDeploymentModule {

        @Provides(type = Provides.Type.SET)
        @Singleton
        public OrientDBRepositoryVerticleDeployment provideEventLogRepositoryDeployment(final AppEventLogger logger) {
            return new OrientDBRepositoryVerticleDeployment(
                    () -> new EventLogRepository(logger),
                    EventLogRepository.class,
                    new DeploymentOptions()
            );
        }

        @Provides
        @Singleton
        public EncryptionService provideEncryptionService() {
            return encryptionService;
        }

        @Provides
        @Singleton
        public Config providesTypesafeConfig() {
            return ConfigFactory.load();
        }

        @Provides
        @Singleton
        public EmbeddedOrientDBServiceConfig providesEmbeddedOrientDBServiceConfig(final OrientDBConfig config) {

            final ApplicationId appId = ApplicationId.builder().group("co.runrightfast").name("runrightfast-vertx-orientdb").version("1.0.0").build();
            final AppEventLogger appEventLogger = new AppEventJDKLogger(appId);

            return EmbeddedOrientDBServiceConfig.newBuilder(config)
                    .databasePoolConfig(new OrientDBPoolConfig(CLASS_NAME, String.format("remote:localhost/%s", CLASS_NAME), "admin", "admin", 10, ImmutableSet.of(() -> new SetCreatedOnAndUpdatedOn())))
                    .databasePoolConfig(new OrientDBPoolConfig(EventLogRepository.DB, String.format("remote:localhost/%s", EventLogRepository.DB), "admin", "admin", 10, EventLogRecord.class))
                    .lifecycleListener(() -> new RunRightFastOrientDBLifeCycleListener(appEventLogger))
                    .build();
        }

    }

    @Component(
            modules = {
                RunRightFastApplicationModule.class,
                VertxServiceModule.class,
                RunRightFastVerticleDeploymentModule.class,
                OrientDBVerticleWithRepositoriesDeploymentModule.class
            }
    )
    @Singleton
    public static interface TestApp extends RunRightFastVertxApplication {
    }

    private static VertxService vertxService;

    private static RunRightFastVertxApplication app;

    @BeforeClass
    public static void setUpClass() throws InterruptedException, ExecutionException {
        metricRegistry.removeMatching(MetricFilter.ALL);
        app = DaggerOrientDBVerticleTest_TestApp.create();
        vertxService = app.vertxService();
        initDatabase();
    }

    private static void initDatabase() {
        Optional<OrientDBService> orientDBService = TypeSafeObjectRegistry.GLOBAL_OBJECT_REGISTRY.get(OrientDBService.ORIENTDB_SERVICE);
        while (!orientDBService.isPresent()) {
            log.log(Level.WARNING, "Waiting for OrientDBService ...");
            sleep(Duration.ofSeconds(2));
            orientDBService = TypeSafeObjectRegistry.GLOBAL_OBJECT_REGISTRY.get(OrientDBService.ORIENTDB_SERVICE);
        }

        final OServerAdmin admin = orientDBService.get().getServerAdmin();
        try {
            OrientDBUtils.createDatabase(admin, EventLogRepository.DB);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            admin.close();
        }

        EventLogRepository.initDatabase(orientDBService.get().getODatabaseDocumentTxSupplier(EventLogRepository.DB).get().get());
    }

    @AfterClass
    public static void tearDownClass() {
        ServiceUtils.stop(vertxService);
    }

    /**
     * Test of startUp method, of class OrientDBVerticle.
     */
    @Test
    public void testVerticle() throws Exception {
        log.info("test_eventBus_GetVerticleDeployments");
        final Vertx vertx = vertxService.getVertx();

        final RunRightFastVerticleId verticleManagerId = RunRightFastVerticleManager.VERTICLE_ID;
        final CompletableFuture<GetVerticleDeployments.Response> getVerticleDeploymentsFuture = new CompletableFuture<>();
        final long timeout = 60000L;
        vertx.eventBus().send(
                EventBusAddress.eventBusAddress(verticleManagerId, "get-verticle-deployments"),
                GetVerticleDeployments.Request.newBuilder().build(),
                new DeliveryOptions().setSendTimeout(timeout),
                responseHandler(getVerticleDeploymentsFuture, GetVerticleDeployments.Response.class)
        );
        final GetVerticleDeployments.Response getVerticleDeploymentsResponse = getVerticleDeploymentsFuture.get(timeout, TimeUnit.MILLISECONDS);
        final int totalHealthCheckCount = getVerticleDeploymentsResponse.getDeploymentsList().stream().collect(Collectors.summingInt(VerticleDeployment::getHealthChecksCount));

        final CompletableFuture<RunVerticleHealthChecks.Response> future = new CompletableFuture<>();
        vertx.eventBus().send(
                EventBusAddress.eventBusAddress(verticleManagerId, "run-verticle-healthchecks"),
                RunVerticleHealthChecks.Request.newBuilder().build(),
                addRunRightFastHeaders(new DeliveryOptions().setSendTimeout(timeout)),
                responseHandler(future, RunVerticleHealthChecks.Response.class)
        );

        final RunVerticleHealthChecks.Response response = future.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(response.getResultsCount(), is(totalHealthCheckCount));

    }

    @Test
    public void testEventLogRepository_getEventCount() throws Exception {
        final Vertx vertx = vertxService.getVertx();
        final RunRightFastVerticleId verticleId = EventLogRepository.VERTICLE_ID;

        final long timeout = 60000L;

        final ProtobufMessageProducer<GetEventCount.Request> getEventCountMessageProducer = new ProtobufMessageProducer<>(
                vertx.eventBus(),
                EventBusAddress.eventBusAddress(verticleId, GetEventCount.class),
                new ProtobufMessageCodec<>(GetEventCount.Request.getDefaultInstance()),
                metricRegistry
        );

        // because the verticles are deployed asynchronously, the EventLogRepository verticle may not yet be deployed yet
        // the message consumer for the Verticle only gets registered, while the verticle is starting. Thus, the message consumer may not yet be registered.
        while (true) {
            final CompletableFuture<GetEventCount.Response> getEventCountFuture = new CompletableFuture<>();
            getEventCountMessageProducer.send(GetEventCount.Request.getDefaultInstance(), responseHandler(getEventCountFuture, GetEventCount.Response.class));
            try {
                getEventCountFuture.get(timeout, TimeUnit.MILLISECONDS);
                break;
            } catch (final ExecutionException e) {
                if (e.getCause() instanceof ReplyException) {
                    final ReplyException replyException = (ReplyException) e.getCause();
                    if (replyException.failureType() == NO_HANDLERS) {
                        log.log(WARNING, "Waiting for EventLogRepository ... ", e);
                        Thread.sleep(5000L);
                        continue;
                    }
                }
                throw e;
            }
        }
    }

    @Test
    public void testEventLogRepository() throws Exception {
        final Vertx vertx = vertxService.getVertx();
        final RunRightFastVerticleId verticleManagerId = EventLogRepository.VERTICLE_ID;

        final CompletableFuture<GetEventCount.Response> getEventCountFuture = new CompletableFuture<>();
        final long timeout = 60000L;

        // because the verticles are deployed asynchronously, the EventLogRepository verticle may not yet be deployed yet
        // the message codec for the Verticle only gets registered, while the verticle is starting. Thus, the message codec may not yet be registered.
        while (true) {
            try {
                vertx.eventBus().send(
                        EventBusAddress.eventBusAddress(verticleManagerId, GetEventCount.class),
                        GetEventCount.Request.getDefaultInstance(),
                        new DeliveryOptions().setSendTimeout(timeout),
                        responseHandler(getEventCountFuture, GetEventCount.Response.class)
                );
                break;
            } catch (final IllegalArgumentException e) {
                if (e.getMessage().contains("No message codec for type")) {
                    log.log(WARNING, "Waiting for EventLogRepository ... ", e);
                    Thread.sleep(5000L);
                } else {
                    throw e;
                }
            }
        }
        final GetEventCount.Response getEventCountResponse = getEventCountFuture.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(getEventCountResponse.getCount(), is(0L));

        final CompletableFuture<CreateEvent.Response> createEventFuture = new CompletableFuture<>();
        vertx.eventBus().send(
                EventBusAddress.eventBusAddress(verticleManagerId, CreateEvent.class),
                CreateEvent.Request.newBuilder().setEvent("testEventLogRepository").build(),
                new DeliveryOptions().setSendTimeout(timeout),
                responseHandler(createEventFuture, CreateEvent.Response.class)
        );
        final CreateEvent.Response createEventResponse = createEventFuture.get(timeout, TimeUnit.MILLISECONDS);
        log.info(String.format("record id = %d::%d", createEventResponse.getId().getClusterId(), createEventResponse.getId().getPosition()));

        final CompletableFuture<GetEventCount.Response> getEventCountFuture2 = new CompletableFuture<>();
        vertx.eventBus().send(
                EventBusAddress.eventBusAddress(verticleManagerId, GetEventCount.class),
                GetEventCount.Request.getDefaultInstance(),
                new DeliveryOptions().setSendTimeout(timeout),
                responseHandler(getEventCountFuture2, GetEventCount.Response.class)
        );
        final GetEventCount.Response getEventCountResponse2 = getEventCountFuture2.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(getEventCountResponse2.getCount(), is(1L));
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
