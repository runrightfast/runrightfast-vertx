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
import static co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer.addRunRightFastHeaders;
import co.runrightfast.vertx.core.modules.RunRightFastApplicationModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.utils.VertxUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.RunVerticleHealthChecks;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.VerticleDeployment;
import co.runrightfast.vertx.orientdb.hooks.SetCreatedOnAndUpdatedOn;
import co.runrightfast.vertx.orientdb.impl.DatabasePoolConfig;
import co.runrightfast.vertx.orientdb.impl.EmbeddedOrientDBServiceConfig;
import co.runrightfast.vertx.orientdb.lifecycle.RunRightFastOrientDBLifeCycleListener;
import co.runrightfast.vertx.orientdb.plugins.OrientDBPluginWithProvidedHazelcastInstance;
import co.runrightfast.vertx.testSupport.EncryptionServiceWithDefaultCiphers;
import com.codahale.metrics.MetricFilter;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.graph.handler.OGraphServerHandler;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import com.orientechnologies.orient.server.handler.OServerSideScriptInterpreter;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
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
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
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
public class OrientDBVerticleTest {

    static final String CLASS_NAME = OrientDBVerticleTest.class.getSimpleName();

    static final File orientdbHome = new File(String.format("build/temp/%s/orientdb", CLASS_NAME));

    private final static EncryptionService encryptionService = new EncryptionServiceWithDefaultCiphers();

    private static final ProtobufMessageCodec<GetVerticleDeployments.Response> getVerticleDeploymentsResponseCodec = new ProtobufMessageCodec(
            GetVerticleDeployments.Response.getDefaultInstance(),
            encryptionService.cipherFunctions(GetVerticleDeployments.getDescriptor().getFullName())
    );

    static {
        System.setProperty("config.resource", String.format("%s.conf", CLASS_NAME));
        ConfigFactory.invalidateCaches();
    }

    @Module
    static class RunRightFastVerticleDeploymentModule {

        @Provides(type = Provides.Type.SET)
        @Singleton
        public RunRightFastVerticleDeployment provideOrientDBVerticleRunRightFastVerticleDeployment(
                final AppEventLogger logger,
                final EncryptionService encryptionService,
                final EmbeddedOrientDBServiceConfig embeddedOrientDBServiceConfig
        ) {
            return new RunRightFastVerticleDeployment(
                    () -> new OrientDBVerticle(
                            logger,
                            encryptionService,
                            embeddedOrientDBServiceConfig,
                            ImmutableSetMultimap.of(),
                            new OrientDBRepositoryVerticleDeployment(
                                    () -> new EventLogRepository(logger, encryptionService),
                                    EventLogRepository.class,
                                    new DeploymentOptions()
                            )
                    ),
                    OrientDBVerticle.class,
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
        public EmbeddedOrientDBServiceConfig providesEmbeddedOrientDBServiceConfig() {
            orientdbHome.mkdirs();
            try {
                FileUtils.cleanDirectory(orientdbHome);
                FileUtils.deleteDirectory(orientdbHome);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            log.logp(INFO, CLASS_NAME, "setUpClass", String.format("orientdbHome.exists() = %s", orientdbHome.exists()));

            final File configDirSrc = new File("src/test/resources/orientdb/config");
            final File configDirTarget = new File(orientdbHome, "config");
            try {
                FileUtils.copyFileToDirectory(new File(configDirSrc, "default-distributed-db-config.json"), configDirTarget);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final ApplicationId appId = ApplicationId.builder().group("co.runrightfast").name("runrightfast-vertx-orientdb").version("1.0.0").build();
            final AppEventLogger appEventLogger = new AppEventJDKLogger(appId);

            return EmbeddedOrientDBServiceConfig.builder()
                    .orientDBRootDir(orientdbHome.toPath())
                    .handler(this::oGraphServerHandler)
                    .handler(this::oHazelcastPlugin)
                    .handler(this::oServerSideScriptInterpreter)
                    .networkConfig(oServerNetworkConfiguration())
                    .user(new OServerUserConfiguration("root", "root", "*"))
                    .property(OGlobalConfiguration.DB_POOL_MIN, "1")
                    .property(OGlobalConfiguration.DB_POOL_MAX, "50")
                    .databasePoolConfig(new DatabasePoolConfig(CLASS_NAME, "admin", "admin", 10, true))
                    .databasePoolConfig(new DatabasePoolConfig(EventLogRepository.DB, "admin", "admin", 10, true))
                    .lifecycleListener(() -> new RunRightFastOrientDBLifeCycleListener(appEventLogger))
                    .hook(() -> new SetCreatedOnAndUpdatedOn())
                    .build();
        }

        private OServerNetworkConfiguration oServerNetworkConfiguration() {
            final OServerNetworkConfiguration network = new OServerNetworkConfiguration();
            network.protocols = ImmutableList.<OServerNetworkProtocolConfiguration>builder()
                    .add(new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName()))
                    .build();
            final OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
            binaryListener.ipAddress = "0.0.0.0";
            binaryListener.protocol = "binary";
            binaryListener.portRange = "2424-2430";
            binaryListener.socket = "default";
            network.listeners = ImmutableList.<OServerNetworkListenerConfiguration>builder()
                    .add(binaryListener)
                    .build();
            return network;
        }

        private OServerHandlerConfiguration oGraphServerHandler() {
            final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
            config.clazz = OGraphServerHandler.class.getName();
            config.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("enabled", "true"),
                new OServerParameterConfiguration("graph.pool.max", "50")
            };
            return config;
        }

        private OServerHandlerConfiguration oHazelcastPlugin() {
            final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
            config.clazz = OrientDBPluginWithProvidedHazelcastInstance.class.getName();
            config.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("enabled", "true"),
                new OServerParameterConfiguration("configuration.db.default", new File(orientdbHome, "config/default-distributed-db-config.json").getAbsolutePath()),};
            return config;
        }

        private OServerHandlerConfiguration oServerSideScriptInterpreter() {
            final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
            config.clazz = OServerSideScriptInterpreter.class.getName();
            config.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("enabled", "true"),
                new OServerParameterConfiguration("allowedLanguages", "SQL")
            };
            return config;
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
        metricRegistry.removeMatching(MetricFilter.ALL);
        app = DaggerOrientDBVerticleTest_TestApp.create();
        vertxService = app.vertxService();
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
