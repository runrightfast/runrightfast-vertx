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

import co.runrightfast.core.crypto.Decryption;
import co.runrightfast.core.crypto.Encryption;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.core.crypto.impl.EncryptionServiceImpl;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.eventbus.EventBusUtils;
import static co.runrightfast.vertx.core.eventbus.EventBusUtils.deliveryOptions;
import co.runrightfast.vertx.core.eventbus.MessageHeader;
import static co.runrightfast.vertx.core.eventbus.ProtobufMessageCodec.getProtobufMessageCodec;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.shiro.crypto.AesCipherService;

/**
 *
 * @author alfio
 */
@Log
public final class DemoMXBeanImpl implements DemoMXBean {

    private final Vertx vertx;

    private final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(DemoMXBean.class.getName());

    private final ProtobufMessageProducer getVerticleDeploymentsMessageSender;

    private final Encryption encryption;

    private final Decryption decryption;

    private final String GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS = "DemoMXBean/GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS/" + UUID.randomUUID().toString();

    private final String ECHO_ADDRESS = "DemoMXBean/ECHO";

    private final String ECHO_REPLY_TO_ADDRESS = "DemoMXBean/ECHO_REPLY_TO_ADDRESS/" + UUID.randomUUID().toString();

    private MessageConsumer<String> echoMessageConsumer;

    public DemoMXBeanImpl(@NonNull final Vertx vertx) {
        this.vertx = vertx;

        final JmxReporter jmxReporter = JmxReporter.forRegistry(this.metricRegistry)
                .inDomain(String.format("%s.metrics", DemoMXBean.class.getSimpleName()))
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build();
        jmxReporter.start();

        getVerticleDeploymentsMessageSender = new ProtobufMessageProducer(
                vertx.eventBus(),
                EventBusAddress.eventBusAddress(RunRightFastVerticleManager.VERTICLE_ID, "get-verticle-deployments"),
                getProtobufMessageCodec(GetVerticleDeployments.Request.getDefaultInstance()).get(),
                metricRegistry
        );

        final String KEY = "DEFAULT";
        final EncryptionService encryptionService = encryptionService(KEY);
        encryption = encryptionService.encryption(KEY);
        decryption = encryptionService.decryption(KEY);

        this.vertx.eventBus().consumer(GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS, this::handleGetVerticleDeploymentsResponse)
                .completionHandler(res -> {
                    if (res.succeeded()) {
                        log.log(Level.INFO, "The handler registration has reached all nodes: {0}", GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS);
                    } else {
                        log.log(Level.INFO, "Registration failed : {0}", GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS);
                    }
                });

        this.vertx.eventBus().consumer(ECHO_REPLY_TO_ADDRESS, this::handleEchoResponse).completionHandler(res -> {
            if (res.succeeded()) {
                log.log(Level.INFO, "The handler registration has reached all nodes: {0}", ECHO_REPLY_TO_ADDRESS);
            } else {
                log.log(Level.INFO, "Registration failed : {0}", ECHO_REPLY_TO_ADDRESS);
            }
        });

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

    private EncryptionService encryptionService(final String secretKey) {
        final AesCipherService aes = new AesCipherService();
        aes.setKeySize(256);
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put(secretKey, aes.generateNewKey())
                .build();
        return new EncryptionServiceImpl(aes, keys);
    }

    @Override
    public String getVerticleDeployments() {
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
        return JsonUtils.toVertxJsonObject(ProtobufUtils.protobuMessageToJson(response)).encodePrettily();
    }

    @Override
    public void getVerticleDeploymentsAcrossCluster() {
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

}
