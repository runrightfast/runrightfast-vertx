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
package co.runrightfast.vertx.demo.testHarness.jmx;

import co.runrightfast.core.AppConfig;
import co.runrightfast.core.crypto.Decryption;
import co.runrightfast.core.crypto.Encryption;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.eventbus.EventBusUtils;
import static co.runrightfast.vertx.core.eventbus.EventBusUtils.deliveryOptions;
import co.runrightfast.vertx.core.eventbus.MessageHeader;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageCodec;
import static co.runrightfast.vertx.core.eventbus.ProtobufMessageCodec.getProtobufMessageCodec;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer;
import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import static co.runrightfast.vertx.core.utils.ConfigUtils.configPath;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.JvmProcess;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import co.runrightfast.vertx.demo.orientdb.EventLogRepository;
import co.runrightfast.vertx.orientdb.OrientDBConfig;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.CreateEvent;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.GetEventCount;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.GetEvents;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.NonNull;
import lombok.extern.java.Log;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Log
public final class DemoMXBeanImpl implements DemoMXBean {

    private final Vertx vertx;

    private final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(DemoMXBean.class.getName());

    private ProtobufMessageProducer getVerticleDeploymentsMessageSender;

    private final EncryptionService encryptionService;

    private final Encryption encryption;

    private final Decryption decryption;

    private final String GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS = "DemoMXBean/GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS/" + UUID.randomUUID().toString();

    private final String ECHO_ADDRESS = "DemoMXBean/ECHO";

    private final String ECHO_REPLY_TO_ADDRESS = "DemoMXBean/ECHO_REPLY_TO_ADDRESS/" + UUID.randomUUID().toString();

    private MessageConsumer<String> echoMessageConsumer;

    private ProtobufMessageProducer<GetEventCount.Request> getEventCountMessageProducer;

    private final String eventLogRepoDBUrl;

    public DemoMXBeanImpl(@NonNull final Vertx vertx, @NonNull final EncryptionService encryptionService, final AppConfig appConfig) {
        this.vertx = vertx;
        this.encryptionService = encryptionService;
        initJmxReporter();

        final String KEY = GetEventCount.Request.getDescriptor().getFullName();
        encryption = encryptionService.encryption(KEY);
        decryption = encryptionService.decryption(KEY);

        registerGetVerticleDeploymentsMessageConsumer();
        initEchoMessageConsumer();

        final OrientDBConfig orientDBConfig = new OrientDBConfig(appConfig.getConfig().getConfig(configPath(CONFIG_NAMESPACE, "orientdb")));
        this.eventLogRepoDBUrl = String.format("plocal:%s/databases/%s", orientDBConfig.getHomeDirectory().toAbsolutePath(), EventLogRepository.DB);
        log.info("eventLogRepoDBUrl = " + eventLogRepoDBUrl);
    }

    private void registerGetVerticleDeploymentsMessageConsumer() {
        this.vertx.eventBus().consumer(GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS, this::handleGetVerticleDeploymentsResponse)
                .completionHandler(res -> {
                    if (res.succeeded()) {
                        log.log(Level.INFO, "The handler registration has reached all nodes: {0}", GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS);
                    } else {
                        log.log(Level.INFO, "Registration failed : {0}", GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS);
                    }
                });
    }

    private void initJmxReporter() {
        final JmxReporter jmxReporter = JmxReporter.forRegistry(this.metricRegistry)
                .inDomain(String.format("%s.metrics", DemoMXBean.class.getSimpleName()))
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build();
        jmxReporter.start();
    }

    private void initEchoMessageConsumer() {
        this.vertx.eventBus().consumer(ECHO_REPLY_TO_ADDRESS, this::handleEchoResponse).completionHandler(res -> {
            if (res.succeeded()) {
                log.log(Level.INFO, "The handler registration has reached all nodes: {0}", ECHO_REPLY_TO_ADDRESS);
            } else {
                log.log(Level.INFO, "Registration failed : {0}", ECHO_REPLY_TO_ADDRESS);
            }
        });

        registerMessageConsumer();
    }

    void handleEchoRequest(final Message<String> request) {
        log.info("*** RECIEVED MESSAGE : " + request.body());
        MessageHeader.getReplyToAddress(request).ifPresent(replyTo -> {
            vertx.eventBus().send(replyTo, String.format("%s: RECEIVED MESSAGE: %s", Instant.now(), request.body()));
        });
    }

    void handleEchoResponse(final Message<String> responseMessage) {
        log.info(responseMessage.body());
    }

    void handleGetVerticleDeploymentsResponse(final Message<GetVerticleDeployments.Response> responseMessage) {
        final GetVerticleDeployments.Response response = responseMessage.body();
        final JsonObject json = Json.createObjectBuilder()
                .add("headers", JsonUtils.toJsonObject(responseMessage.headers()))
                .add("body", ProtobufUtils.protobuMessageToJson(response))
                .build();
        log.info(JsonUtils.toVertxJsonObject(json).encodePrettily());
    }

    @Override
    public String verticleDeployments() {
        if (getVerticleDeploymentsMessageSender == null) {
            getVerticleDeploymentsMessageSender = new ProtobufMessageProducer(
                    vertx.eventBus(),
                    EventBusAddress.eventBusAddress(RunRightFastVerticleManager.VERTICLE_ID, "get-verticle-deployments"),
                    getProtobufMessageCodec(GetVerticleDeployments.Request.getDefaultInstance()).get(),
                    metricRegistry
            );
        }

        final CompletableFuture<com.google.protobuf.Message> future = new CompletableFuture();

        getVerticleDeploymentsMessageSender.send(
                GetVerticleDeployments.Request.newBuilder().build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, GetVerticleDeployments.Response.class)
        );

        final com.google.protobuf.Message response;
        try {
            response = future.get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }

        // TODO: aggregate GetVerticleDeployments.Response from all RunRightFastVerticleManager instances deployed within the JVm
        // ping
        return JsonUtils.toVertxJsonObject(ProtobufUtils.protobuMessageToJson(response)).encodePrettily();
    }

    @Override
    public void verticleDeploymentsAcrossCluster() {
        getVerticleDeploymentsMessageSender.publish(
                GetVerticleDeployments.Request.newBuilder().build(),
                EventBusUtils.withReplyToAddress(EventBusUtils.deliveryOptions(), GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS)
        );
    }

    private <A extends com.google.protobuf.Message> Handler<AsyncResult<Message<A>>> responseHandler(final CompletableFuture future, final Class<A> messageType) {
        return result -> {
            if (result.succeeded()) {
                future.complete(result.result().body());
            } else {
                log.logp(SEVERE, getClass().getName(), String.format("responseHandler.failure::%s", messageType.getName()), "request failed", result.cause());
                future.completeExceptionally(result.cause());
            }
        };
    }

    @Override
    public String encrypt(@NonNull final String data) {
        return Base64.getEncoder().encodeToString(encryption.apply(data.getBytes(UTF_8)));
    }

    @Override
    public String decrypt(final @NonNull String data) {
        return new String(decryption.apply(Base64.getDecoder().decode(data)), UTF_8);
    }

    @Override
    public void publishMessage(@NonNull final String message) {
        vertx.eventBus().publish(ECHO_ADDRESS, message, EventBusUtils.withReplyToAddress(deliveryOptions(), ECHO_REPLY_TO_ADDRESS));
    }

    @Override
    public synchronized void registerMessageConsumer() {
        if (!isMessagConsumerRegistered()) {
            this.echoMessageConsumer = this.vertx.eventBus().consumer(ECHO_ADDRESS, this::handleEchoRequest);
            this.echoMessageConsumer.completionHandler(res -> {
                if (res.succeeded()) {
                    log.log(Level.INFO, "The handler registration has reached all nodes: {0}", ECHO_ADDRESS);
                } else {
                    log.log(Level.INFO, "Registration failed : {0}", ECHO_ADDRESS);
                }
            });
        }
    }

    @Override
    public synchronized void unregisterMessageConsumer() {
        if (echoMessageConsumer != null) {
            echoMessageConsumer.unregister(res -> {
                if (res.succeeded()) {
                    log.info("The handler un-registration has reached all nodes");
                } else {
                    log.info("Un-registration failed!");
                }
            });
            echoMessageConsumer = null;
        }
    }

    @Override
    public boolean isMessagConsumerRegistered() {
        return this.echoMessageConsumer != null;
    }

    @Override
    public String lookupIPAddress(final String dnsServer, final String host) {
        final DnsClient client = vertx.createDnsClient(53, dnsServer);
        final CompletableFuture<String> future = new CompletableFuture<>();
        client.lookup("vertx.io", result -> {
            if (result.succeeded()) {
                future.complete(result.result());
            } else {
                future.completeExceptionally(result.cause());
            }
        });

        try {
            return future.get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String[] getIPAddresses(final String host) {
        try {
            return Arrays.stream(InetAddress.getAllByName(host)).map(InetAddress::getHostAddress).toArray(String[]::new);
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String[] getHostNameAndHostAddress() {
        return new String[]{JvmProcess.HOST, JvmProcess.HOST_ADDRESS};
    }

    @Override
    public long eventLogRecordCount() {
        if (this.getEventCountMessageProducer == null) {
            getEventCountMessageProducer = new ProtobufMessageProducer<>(
                    vertx.eventBus(),
                    EventBusAddress.eventBusAddress(EventLogRepository.VERTICLE_ID, GetEventCount.class),
                    new ProtobufMessageCodec<>(GetEventCount.Request.getDefaultInstance()),
                    metricRegistry
            );
        }

        try {
            final CompletableFuture<GetEventCount.Response> getEventCountFuture = new CompletableFuture<>();
            getEventCountMessageProducer.send(GetEventCount.Request.getDefaultInstance(), responseHandler(getEventCountFuture, GetEventCount.Response.class));
            final GetEventCount.Response response = getEventCountFuture.get(2, TimeUnit.SECONDS);
            return response.getCount();
        } catch (final InterruptedException | ExecutionException | TimeoutException ex) {
            log.logp(SEVERE, getClass().getName(), "getEventLogRecordCount", "failed", ex);
            throw new RuntimeException("Failed to get event log record count: " + ex.getMessage());
        }
    }

    @Override
    public String createEventLogRecord(final String event) {
        Preconditions.checkArgument(isNotBlank(event));
        final CompletableFuture<CreateEvent.Response> createEventFuture = new CompletableFuture<>();
        vertx.eventBus().send(
                EventBusAddress.eventBusAddress(EventLogRepository.VERTICLE_ID, CreateEvent.class),
                CreateEvent.Request.newBuilder().setEvent(event).build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(createEventFuture, CreateEvent.Response.class)
        );
        try {
            final CreateEvent.Response createEventResponse = createEventFuture.get(2, TimeUnit.SECONDS);
            return ProtobufUtils.protobuMessageToJson(createEventResponse.getId()).toString();
        } catch (final InterruptedException | ExecutionException | TimeoutException ex) {
            log.logp(SEVERE, getClass().getName(), "createEventLogRecord", "failed", ex);
            throw new RuntimeException("Failed to create event log record : " + ex.getMessage());
        }
    }

    @Override
    public String browseEventLogRecords(int skip, int limit) {
        final CompletableFuture<GetEvents.Response> future = new CompletableFuture<>();
        vertx.eventBus().send(
                EventBusAddress.eventBusAddress(EventLogRepository.VERTICLE_ID, GetEvents.class),
                GetEvents.Request.newBuilder().setSkip(skip).setLimit(limit).build(),
                new DeliveryOptions().setSendTimeout(2000L),
                responseHandler(future, GetEvents.Response.class)
        );
        try {
            final GetEvents.Response response = future.get(2, TimeUnit.SECONDS);
            return JsonUtils.toVertxJsonObject(Json.createObjectBuilder()
                    .add("count", response.getEventsCount())
                    .add("records", ProtobufUtils.protobuMessageToJson(response))
                    .build())
                    .encodePrettily();
        } catch (final InterruptedException | ExecutionException | TimeoutException ex) {
            log.logp(SEVERE, getClass().getName(), "createEventLogRecord", "failed", ex);
            throw new RuntimeException("Failed to create event log record : " + ex.getMessage());
        }
    }

    @Override
    public boolean eventLogRepositoryExists() {
        return new ODatabaseDocumentTx(eventLogRepoDBUrl).exists();
    }

    @Override
    public boolean createEventLogRepository() {
        final ODatabaseDocumentTx db = new ODatabaseDocumentTx(eventLogRepoDBUrl);
        if (!db.exists()) {
            db.create();
            log.logp(INFO, getClass().getName(), "createEventLogRepository", String.format("created db = %s", eventLogRepoDBUrl));
            return true;
        } else {
            return false;
        }
    }

}
