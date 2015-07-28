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

import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Counters.MESSAGE_CONSUMER_EXCEPTION;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Counters.MESSAGE_CONSUMER_MESSAGE_PROCESSING;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Counters.MESSAGE_CONSUMER_MESSAGE_TOTAL;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Timers.MESSAGE_CONSUMER_HANDLER;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import co.runrightfast.vertx.core.eventbus.MessageConsumerRegistration;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageCodec;
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
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import javax.json.Json;
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

    protected MetricRegistry metricRegistry;
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
        this.metricRegistry = SharedMetricRegistries.getOrCreate(context.deploymentID());
        this.healthCheckRegistry = SharedHealthCheckRegistries.getOrCreate(context.deploymentID());
        this.instanceId = instanceSequence.incrementAndGet();
        log.logp(INFO, CLASS_NAME, "init", () -> lifeCycleMsg("initialized"));
    }

    @Override
    public final void start() throws Exception {
        log.logp(INFO, CLASS_NAME, "start", () -> lifeCycleMsg("starting"));
        try {
            metricRegistry.counter(RunRightFastVerticleMetrics.Counters.INSTANCE_STARTED.metricName).inc();
            startUp();
        } finally {
            log.logp(INFO, CLASS_NAME, "start", () -> lifeCycleMsg("started"));
        }
    }

    @Override
    public final void stop() throws Exception {
        log.logp(INFO, CLASS_NAME, "stop", () -> lifeCycleMsg("stopping"));
        try {
            shutDown();
        } finally {
            metricRegistry.counter(RunRightFastVerticleMetrics.Counters.INSTANCE_STARTED.metricName).dec();
            log.logp(INFO, CLASS_NAME, "stop", () -> lifeCycleMsg("stopped"));
        }
    }

    private String lifeCycleMsg(final String state) {
        return Json.createObjectBuilder()
                .add("verticleId", getRunRightFastVerticleId().toJson())
                .add("instanceId", instanceId)
                .add("state", state)
                .build()
                .toString();
    }

    protected <REQ extends Message, RESP extends Message> MessageConsumer<REQ> registerMessageConsumer(@NonNull final MessageConsumerConfig<REQ, RESP> config) {
        Preconditions.checkState(!messageConsumerRegistrations.containsKey(config.getAddressMessageMapping().getAddress()));
        final EventBus eventBus = vertx.eventBus();
        registerMessageCodecs(config);
        final MessageConsumer<REQ> consumer = config.isLocal() ? eventBus.localConsumer(config.getAddressMessageMapping().getAddress()) : eventBus.consumer(config.getAddressMessageMapping().getAddress());
        consumer.completionHandler(config.getCompletionHandler().map(handler -> messageConsumerCompletionHandler(Optional.of(handler), config))
                .orElseGet(() -> messageConsumerCompletionHandler(Optional.empty(), config)));
        consumer.endHandler(config.getEndHandler().map(handler -> messageConsumerEndHandler(Optional.of(handler), config))
                .orElseGet(() -> messageConsumerEndHandler(Optional.empty(), config)));
        consumer.exceptionHandler(config.getExceptionHandler().map(handler -> messageConsumerExceptionHandler(Optional.of(handler), config))
                .orElseGet(() -> messageConsumerExceptionHandler(Optional.empty(), config)));
        consumer.handler(messageConsumerHandler(config));
        messageConsumerRegistrations = ImmutableMap.<String, MessageConsumerRegistration<?, ?>>builder().putAll(messageConsumerRegistrations).put(
                config.address(),
                MessageConsumerRegistration.<REQ, RESP>builder().messageConsumer(consumer).config(config).build()
        ).build();
        return consumer;
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
            // Order matters - the default codec must be registered first
            eventBus.registerDefaultCodec(config.getAddressMessageMapping().getRequestDefaultInstance().getClass(), codec);
            eventBus.registerCodec(codec);
        } catch (final IllegalStateException e) {
            log.logp(FINE, CLASS_NAME, "registerMessageCodecs", "failed to register codec for request message", e.getCause());
        }
        // TODO: Investigate why Optional type is lost - forced to cast responseDefaultInstance to Message
        config.getAddressMessageMapping().getResponseDefaultInstance().ifPresent(responseDefaultInstance -> {
            try {
                final MessageCodec codec = new ProtobufMessageCodec((Message) responseDefaultInstance);
                // Order matters - the default codec must be registered first
                eventBus.registerDefaultCodec(responseDefaultInstance.getClass(), codec);
                eventBus.registerCodec(codec);
            } catch (final IllegalStateException e) {
                log.logp(FINE, CLASS_NAME, "registerMessageCodecs", "failed to register codec for response message", e.getCause());
            }
        });
    }

    private <REQ extends Message, RESP extends Message> Handler<AsyncResult<Void>> messageConsumerCompletionHandler(final Optional<Handler<AsyncResult<Void>>> handler, final MessageConsumerConfig<REQ, RESP> config) {
        return (AsyncResult<Void> result) -> {
            if (result.succeeded()) {
                log.logp(INFO, CLASS_NAME, "messageConsumerCompletionHandler.succeeded", config::toString);
            } else {
                log.logp(SEVERE, CLASS_NAME, "messageConsumerCompletionHandler.failed", config.toString(), result.cause());
            }
            handler.ifPresent(h -> h.handle(result));
        };
    }

    private <REQ extends Message, RESP extends Message> Handler<Void> messageConsumerEndHandler(final Optional<Handler<Void>> handler, final MessageConsumerConfig<REQ, RESP> config) {
        final Handler<Void> defaultHandler = result -> {
            log.logp(INFO, CLASS_NAME, "messageConsumerEndHandler", config::toString);
        };

        return handler.map(h -> {
            final Handler<Void> endHandler = result -> {
                defaultHandler.handle(result);
                h.handle(result);
            };
            return endHandler;
        }).orElse(defaultHandler);
    }

    private <REQ extends Message, RESP extends Message> Handler<Throwable> messageConsumerExceptionHandler(final Optional<Handler<Throwable>> handler, final MessageConsumerConfig<REQ, RESP> config) {
        final Handler<Throwable> defaultHandler = result -> {
            metricRegistry.counter(String.format("%s::%s", MESSAGE_CONSUMER_EXCEPTION.metricName, config.getAddressMessageMapping().getAddress())).inc();
            log.logp(INFO, CLASS_NAME, "messageConsumerEndHandler", config.toString(), result);
        };

        return handler.map(h -> {
            final Handler<Throwable> endHandler = result -> {
                defaultHandler.handle(result);
                h.handle(result);
            };
            return endHandler;
        }).orElse(defaultHandler);
    }

    private <REQ extends Message, RESP extends Message> Handler<io.vertx.core.eventbus.Message<REQ>> messageConsumerHandler(final MessageConsumerConfig<REQ, RESP> config) {
        final Counter messageTotalCounter = metricRegistry.counter(String.format("%s::%s", MESSAGE_CONSUMER_MESSAGE_TOTAL.metricName, config.getAddressMessageMapping().getAddress()));
        final Counter messageProcessingCounter = metricRegistry.counter(String.format("%s::%s", MESSAGE_CONSUMER_MESSAGE_PROCESSING.metricName, config.getAddressMessageMapping().getAddress()));
        final Counter messageSuccessCounter = metricRegistry.counter(String.format("%s::%s", MESSAGE_CONSUMER_MESSAGE_TOTAL.metricName, config.getAddressMessageMapping().getAddress()));
        final Timer timer = metricRegistry.timer(String.format("%s::%s", MESSAGE_CONSUMER_HANDLER.metricName, config.getAddressMessageMapping().getAddress()));
        final Handler<io.vertx.core.eventbus.Message<REQ>> handler = config.getHandler();
        return msg -> {
            messageTotalCounter.inc();
            messageProcessingCounter.inc();
            log.logp(INFO, CLASS_NAME, "messageConsumerHandler", config::address);
            final Timer.Context timerCtx = timer.time();
            try {
                handler.handle(msg);
                messageSuccessCounter.inc();
            } finally {
                timerCtx.stop();
                messageProcessingCounter.dec();
            }
        };

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

    /**
     * Used to initialize the {@link #verticleId} field.
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

}
