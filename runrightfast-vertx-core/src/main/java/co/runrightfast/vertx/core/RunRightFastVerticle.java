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
package co.runrightfast.vertx.core;

import co.runrightfast.core.application.services.healthchecks.HealthCheckConfig;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Counters.MESSAGE_CONSUMER_MESSAGE_FAILURE;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Counters.MESSAGE_CONSUMER_MESSAGE_PROCESSING;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Counters.MESSAGE_CONSUMER_MESSAGE_SUCCESS;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Timers.MESSAGE_CONSUMER_HANDLER;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.eventbus.EventBusUtils;
import static co.runrightfast.vertx.core.eventbus.EventBusUtils.responseDeliveryOptions;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig.Failure;
import co.runrightfast.vertx.core.eventbus.MessageConsumerHandlerException;
import co.runrightfast.vertx.core.eventbus.MessageConsumerRegistration;
import co.runrightfast.vertx.core.eventbus.MessageHeader;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.getReplyToAddress;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageCodec;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer;
import static co.runrightfast.vertx.core.utils.JsonUtils.toJsonObject;
import co.runrightfast.vertx.core.utils.LoggingUtils;
import static co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog.newErrorLog;
import static co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog.newInfoLog;
import static co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog.newWarningLog;
import static co.runrightfast.vertx.core.utils.ProtobufUtils.protobuMessageToJson;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Base class for verticles, which provides support for :
 * <ol>
 * <li>logging
 * <li>config
 * <li>metrics
 * <li>healthchecks
 * </ol>
 *
 * Each verticle has its own scoped MetricRegistry and HealthCheckRegistry using the verticle's deployment id. Thus, if there are multiple instances of a
 * verticle running, they will share the same deployment id.
 *
 * Subclasses must override the {@link #startUp()} and {@link #shutDown()} methods, which are invoked by the corresponding {@link #start()} and {@link #stop()}
 * methods.
 *
 * Each verticle defines its RunRightFastVerticleId and manages its own versioning.
 *
 * A verticle must have only one MessageConsumer registered per EventBus address.
 *
 * @author alfio
 */
public abstract class RunRightFastVerticle extends AbstractVerticle {

    private static final AtomicInteger instanceSequence = new AtomicInteger(0);

    protected final String CLASS_NAME = getClass().getName();
    protected final Logger log = Logger.getLogger(CLASS_NAME);

    protected final LoggingUtils.JsonLog info = newInfoLog(log, CLASS_NAME);
    protected final LoggingUtils.JsonLog warning = newWarningLog(log, CLASS_NAME);
    protected final LoggingUtils.JsonLog error = newErrorLog(log, CLASS_NAME);

    @Getter
    protected MetricRegistry metricRegistry;

    @Getter
    protected HealthCheckRegistry healthCheckRegistry;

    // EventBus address -> MessageConsumerRegistration
    protected ImmutableMap<String, MessageConsumerRegistration<?, ?>> messageConsumerRegistrations = ImmutableMap.of();

    protected int instanceId;

    /**
     * Performs the following:
     *
     * <ol>
     * <li>initializes the metric registry
     * <li>initializes the healthcheck regsistry
     * </ol>
     *
     * @param vertx Vertx
     * @param context Context
     */
    @Override
    public final void init(final Vertx vertx, final Context context) {
        super.init(vertx, context);
        this.metricRegistry = SharedMetricRegistries.getOrCreate(getRunRightFastVerticleId().toString());
        this.healthCheckRegistry = SharedHealthCheckRegistries.getOrCreate(getRunRightFastVerticleId().toString());
        this.instanceId = instanceSequence.incrementAndGet();
        info.log("init", () -> lifeCycleMsg("initialized"));
    }

    @Override
    public final void start() throws Exception {
        info.log("start", () -> lifeCycleMsg("starting"));
        try {
            metricRegistry.counter(RunRightFastVerticleMetrics.Counters.INSTANCE_STARTED.metricName).inc();
            startUp();
            registerHealthChecks();
        } finally {
            info.log("start", () -> lifeCycleMsg("started"));
        }
    }

    @Override
    public final void stop() throws Exception {
        info.log("stop", () -> lifeCycleMsg("stopping"));
        try {
            unregisterhealthChecks();
            shutDown();
        } finally {
            metricRegistry.counter(RunRightFastVerticleMetrics.Counters.INSTANCE_STARTED.metricName).dec();
            info.log("stop", () -> lifeCycleMsg("stopped"));
        }
    }

    private void registerHealthChecks() {
        getHealthChecks().stream().forEach(healthCheck -> healthCheckRegistry.register(healthCheck.getConfig().getName(), healthCheck.getHealthCheck()));
    }

    private void unregisterhealthChecks() {
        getHealthChecks().stream().forEach(healthCheck -> healthCheckRegistry.unregister(healthCheck.getConfig().getName()));
    }

    private JsonObject lifeCycleMsg(final String state) {
        return Json.createObjectBuilder()
                .add("verticleId", getRunRightFastVerticleId().toJson())
                .add("instanceId", instanceId)
                .add("state", state)
                .build();
    }

    /**
     *
     * @param <REQ>
     * @param <RESP>
     * @param config
     * @return MessageConsumer
     */
    protected <REQ extends Message, RESP extends Message> MessageConsumerRegistration<REQ, RESP> registerMessageConsumer(@NonNull final MessageConsumerConfig<REQ, RESP> config) {
        Preconditions.checkState(!messageConsumerRegistrations.containsKey(config.getAddressMessageMapping().getAddress()));
        final EventBus eventBus = vertx.eventBus();
        registerMessageCodecs(config);

        final String address = config.getAddressMessageMapping().getAddress();
        final MessageConsumer<REQ> consumer = config.isLocal() ? eventBus.localConsumer(address) : eventBus.consumer(address);
        consumer.completionHandler(config.getCompletionHandler().map(handler -> messageConsumerCompletionHandler(address, Optional.of(handler), config))
                .orElseGet(() -> messageConsumerCompletionHandler(address, Optional.empty(), config)));
        consumer.endHandler(config.getEndHandler().map(handler -> messageConsumerEndHandler(address, Optional.of(handler), config))
                .orElseGet(() -> messageConsumerEndHandler(address, Optional.empty(), config)));
        config.getExceptionHandler().ifPresent(consumer::exceptionHandler);
        consumer.handler(messageConsumerHandler(config));

        final String processSpecificAddress = config.getAddressMessageMapping().getProcessSpecificAddress();
        final MessageConsumer<REQ> processSpecificConsumer = config.isLocal() ? eventBus.localConsumer(processSpecificAddress) : eventBus.consumer(processSpecificAddress);
        processSpecificConsumer.completionHandler(config.getCompletionHandler().map(handler -> messageConsumerCompletionHandler(processSpecificAddress, Optional.of(handler), config))
                .orElseGet(() -> messageConsumerCompletionHandler(processSpecificAddress, Optional.empty(), config)));
        processSpecificConsumer.endHandler(config.getEndHandler().map(handler -> messageConsumerEndHandler(processSpecificAddress, Optional.of(handler), config))
                .orElseGet(() -> messageConsumerEndHandler(processSpecificAddress, Optional.empty(), config)));
        config.getExceptionHandler().ifPresent(processSpecificConsumer::exceptionHandler);
        processSpecificConsumer.handler(messageConsumerHandler(config));

        final MessageConsumerRegistration<REQ, RESP> messageConsumerRegistration = MessageConsumerRegistration.<REQ, RESP>builder()
                .messageConsumer(consumer)
                .processSpecificMessageConsumer(processSpecificConsumer)
                .config(config)
                .build();
        messageConsumerRegistrations = ImmutableMap.<String, MessageConsumerRegistration<?, ?>>builder().putAll(messageConsumerRegistrations).put(
                config.address(),
                messageConsumerRegistration
        ).build();
        return messageConsumerRegistration;
    }

    /**
     * An IllegalStateException is thrown if a codec is already registered with the same name. Ignore the exception.
     *
     * @param <REQ>
     * @param <RESP>
     * @param config
     */
    private <REQ extends Message, RESP extends Message> void registerMessageCodecs(final MessageConsumerConfig<REQ, RESP> config) {
        final EventBus eventBus = vertx.eventBus();
        try {
            final MessageCodec codec = new ProtobufMessageCodec(config.getAddressMessageMapping().getRequestDefaultInstance());
            eventBus.registerDefaultCodec(config.getAddressMessageMapping().getRequestDefaultInstance().getClass(), codec);
        } catch (final IllegalStateException e) {
            log.logp(FINE, CLASS_NAME, "registerMessageCodecs", "failed to register codec for request message", e.getCause());
        }

        config.getAddressMessageMapping().getResponseDefaultInstance().ifPresent(responseDefaultInstance -> {
            try {
                // TODO: Investigate why Optional type is lost - forced to cast responseDefaultInstance to Message
                final MessageCodec codec = new ProtobufMessageCodec((Message) responseDefaultInstance);
                eventBus.registerDefaultCodec(responseDefaultInstance.getClass(), codec);
            } catch (final IllegalStateException e) {
                log.logp(FINE, CLASS_NAME, "registerMessageCodecs", "failed to register codec for response message", e.getCause());
            }
        });
    }

    private <REQ extends Message, RESP extends Message> JsonObject messageConsumerLogInfo(final MessageConsumerConfig<REQ, RESP> config) {
        return messageConsumerLogInfoAsJson(config).build();
    }

    private <REQ extends Message, RESP extends Message> JsonObjectBuilder messageConsumerLogInfoAsJson(final MessageConsumerConfig<REQ, RESP> config) {
        return Json.createObjectBuilder()
                .add("config", config.toJson());
    }

    private <REQ extends Message, RESP extends Message> Handler<AsyncResult<Void>> messageConsumerCompletionHandler(final String address, final Optional<Handler<AsyncResult<Void>>> handler, final MessageConsumerConfig<REQ, RESP> config) {
        return (AsyncResult<Void> result) -> {
            if (result.succeeded()) {
                info.log("messageConsumerCompletionHandler.succeeded", () -> messageConsumerLogInfo(config));
            } else {
                error.log("messageConsumerCompletionHandler.failed", () -> messageConsumerLogInfo(config), result.cause());
            }
            handler.ifPresent(h -> h.handle(result));
        };
    }

    private <REQ extends Message, RESP extends Message> Handler<Void> messageConsumerEndHandler(final String address, final Optional<Handler<Void>> handler, final MessageConsumerConfig<REQ, RESP> config) {
        final Handler<Void> defaultHandler = result -> {
            info.log("messageConsumerEndHandler", () -> messageConsumerLogInfo(config));
        };

        return handler.map(h -> {
            final Handler<Void> endHandler = result -> {
                defaultHandler.handle(result);
                h.handle(result);
            };
            return endHandler;
        }).orElse(defaultHandler);
    }

    private void logMessageConsumerException(final Throwable exception, final String address, final MessageConsumerConfig config) {
        if (exception instanceof MessageConsumerHandlerException) {
            final MessageConsumerHandlerException messageConsumerHandlerException = (MessageConsumerHandlerException) exception;
            final io.vertx.core.eventbus.Message<? extends Message> failedMessage = messageConsumerHandlerException.getFailedMessage();

            final JsonObject message = Json.createObjectBuilder()
                    .add("headers", toJsonObject(failedMessage.headers()))
                    .add("body", protobuMessageToJson(failedMessage.body()))
                    .build();

            error.log("logMessageConsumerException", () -> messageConsumerLogInfoAsJson(config).add("message", message).build(), exception);
        } else {
            error.log("logMessageConsumerException", () -> messageConsumerLogInfo(config), exception);
        }
    }

    private void replyWithFailure(final io.vertx.core.eventbus.Message<?> message, final Throwable exception, final MessageConsumerConfig config) {
        final Failure failure = config.toFailure(exception);
        message.fail(failure.getCode(), failure.getMessage());
    }

    /**
     * Wraps the handler to perform the following:
     * <ul>
     * <li>Collects metrics
     * <li>Logs info about the message
     * </ul>
     *
     * @param <REQ> Request message type
     * @param <RESP> Response message type
     * @param config MessageConsumerConfig
     * @return handler
     */
    private <REQ extends Message, RESP extends Message> Handler<io.vertx.core.eventbus.Message<REQ>> messageConsumerHandler(final MessageConsumerConfig<REQ, RESP> config) {
        final Counter messageProcessingCounter = metricRegistry.counter(String.format("%s::%s", MESSAGE_CONSUMER_MESSAGE_PROCESSING.metricName, config.address()));
        final Counter messageSuccessCounter = metricRegistry.counter(String.format("%s::%s", MESSAGE_CONSUMER_MESSAGE_SUCCESS.metricName, config.address()));
        final Counter messageFailureCounter = metricRegistry.counter(String.format("%s::%s", MESSAGE_CONSUMER_MESSAGE_FAILURE.metricName, config.address()));
        final Timer timer = metricRegistry.timer(String.format("%s::%s", MESSAGE_CONSUMER_HANDLER.metricName, config.getAddressMessageMapping().getAddress()));
        final Handler<io.vertx.core.eventbus.Message<REQ>> handler = config.getHandler();
        return msg -> {
            messageProcessingCounter.inc();
            log.logp(INFO, CLASS_NAME, "messageConsumerHandler", config::address);
            final Timer.Context timerCtx = timer.time();
            try {
                handler.handle(msg);
                messageSuccessCounter.inc();
            } catch (final Throwable t) {
                messageFailureCounter.inc();
                logMessageConsumerException(t, config.address(), config);
                replyWithFailure(msg, t, config);
            } finally {
                timerCtx.stop();
                messageProcessingCounter.dec();
            }
        };

    }

    /**
     * Sets the standard headers.
     *
     * @param request
     * @param response
     * @see EventBusUtils#responseDeliveryOptions(io.vertx.core.eventbus.Message)
     */
    protected void reply(@NonNull final io.vertx.core.eventbus.Message request, @NonNull final Object response) {
        reply(request, response, responseDeliveryOptions(request));
    }

    /**
     * If the message has a {@link MessageHeader#REPLY_TO_ADDRESS} header, then send the reply back to that address. Otherwise, reply back using the built in
     * Vertx Message reply mechanism.
     *
     * @param request
     * @param response
     * @param options
     */
    protected void reply(@NonNull final io.vertx.core.eventbus.Message request, @NonNull final Object response, @NonNull final DeliveryOptions options) {
        final Optional<String> replyTo = getReplyToAddress(request);
        if (replyTo.isPresent()) {
            vertx.eventBus().send(replyTo.get(), response, options);
        } else {
            request.reply(response, options);
        }
    }

    /**
     *
     *
     * @param path base path
     * @param paths rest of the path
     * @return <code>/${runRightFastVerticleId.group}/${runRightFastVerticleId.name}/$path/...</code>
     */
    protected String eventBusAddress(final String path, final String... paths) {
        return EventBusAddress.eventBusAddress(getRunRightFastVerticleId(), path, paths);
    }

    protected HealthCheckConfig.HealthCheckConfigBuilder healthCheckConfigBuilder() {
        return HealthCheckConfig.builder().registryName(getRunRightFastVerticleId().toJson().toString());
    }

    protected <A extends Message> ProtobufMessageProducer<A> newProtobufMessageProducer(final String address, final A messageDefaultInstance) {
        return new ProtobufMessageProducer<>(vertx.eventBus(), address, messageDefaultInstance, metricRegistry);
    }

    @Override
    public int hashCode() {
        return getRunRightFastVerticleId().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RunRightFastVerticle other = (RunRightFastVerticle) obj;
        return getRunRightFastVerticleId().equals(other.getRunRightFastVerticleId());
    }

    /**
     * Used to initialize the {@link #getRunRightFastVerticleId()} field.
     *
     * @return RunRightFastVerticleId
     */
    public abstract RunRightFastVerticleId getRunRightFastVerticleId();

    /**
     * Verticle specific start up
     */
    protected abstract void startUp();

    /**
     * Verticle specific start up shutdown
     */
    protected abstract void shutDown();

    protected abstract Set<RunRightFastHealthCheck> getHealthChecks();

}
