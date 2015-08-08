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
package co.runrightfast.vertx.core.eventbus;

import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Gauges.MESSAGE_LAST_PUBLISHED_TS;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Gauges.MESSAGE_LAST_SENT_TS;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Meters.MESSAGE_PUBLISHED;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.Meters.MESSAGE_SENT;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.gaugeName;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.meterName;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.MESSAGE_ID;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.MESSAGE_TIMESTAMP;
import static co.runrightfast.vertx.core.utils.PreconditionsUtils.isNotBlank;
import static co.runrightfast.vertx.core.utils.UUIDUtils.uuid;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.Message;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import static java.util.logging.Level.FINE;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 * @param <A> message type
 */
@Log
public final class ProtobufMessageProducer<A extends Message> {

    private final EventBus eventBus;

    @Getter
    private final String address;

    private final Meter messageSent;

    private final Meter messagePublished;

    private Instant messageLastSent;

    private Instant messageLastPublished;

    /**
     * Collects metrics on messages that are sent the following meters:
     *
     * <ol>
     * <li>
     * </ol>
     *
     * @param eventBus
     * @param address
     * @param defaultInstance used to register the message codec - {@link ProtobufMessageCodec}
     * @param metricRegistry used to register the 2 meters described above
     */
    public ProtobufMessageProducer(@NonNull final EventBus eventBus, final String address, @NonNull final A defaultInstance, @NonNull final MetricRegistry metricRegistry) {
        isNotBlank(address);
        this.eventBus = eventBus;
        this.address = address;

        registerMessageCodec(defaultInstance);

        this.messageSent = metricRegistry.meter(meterName(String.format("%s::%s", MESSAGE_SENT.metricName, address)));
        this.messagePublished = metricRegistry.meter(meterName(String.format("%s::%s", MESSAGE_PUBLISHED.metricName, address)));
        metricRegistry.register(gaugeName(String.format("%s::%s", MESSAGE_LAST_SENT_TS.metricName, address)), new Gauge<String>() {
            @Override
            public String getValue() {
                return messageLastSent != null ? DateTimeFormatter.ISO_INSTANT.format(messageLastSent) : null;
            }
        });

        metricRegistry.register(gaugeName(String.format("%s::%s", MESSAGE_LAST_PUBLISHED_TS.metricName, address)), new Gauge<String>() {
            @Override
            public String getValue() {
                return messageLastPublished != null ? DateTimeFormatter.ISO_INSTANT.format(messageLastPublished) : null;
            }
        });
    }

    public void send(@NonNull final A msg) {
        eventBus.send(address, msg, addRunRightFastHeaders(new DeliveryOptions()));
        this.messageSent.mark();
        this.messageLastSent = Instant.now();
    }

    public void send(@NonNull final A msg, @NonNull final DeliveryOptions options) {
        eventBus.send(address, msg, addRunRightFastHeaders(options));
        this.messageSent.mark();
        this.messageLastSent = Instant.now();
    }

    public void send(@NonNull final A msg, @NonNull final Handler<AsyncResult<io.vertx.core.eventbus.Message<A>>> handler) {
        eventBus.send(address, msg, addRunRightFastHeaders(new DeliveryOptions()), handler);
        this.messageSent.mark();
        this.messageLastSent = Instant.now();
    }

    public void send(@NonNull final A msg, @NonNull final DeliveryOptions options, @NonNull final Handler<AsyncResult<io.vertx.core.eventbus.Message<A>>> handler) {
        eventBus.send(address, msg, addRunRightFastHeaders(options), handler);
        this.messageSent.mark();
        this.messageLastSent = Instant.now();
    }

    public void publish(@NonNull final A msg) {
        eventBus.publish(address, msg, addRunRightFastHeaders(new DeliveryOptions()));
        this.messagePublished.mark();
        this.messageLastPublished = Instant.now();
    }

    public void publish(@NonNull final A msg, @NonNull final DeliveryOptions options) {
        eventBus.publish(address, msg, addRunRightFastHeaders(options));
        this.messagePublished.mark();
        this.messageLastPublished = Instant.now();
    }

    private DeliveryOptions addRunRightFastHeaders(final DeliveryOptions options) {
        final MultiMap headers = options.getHeaders();
        if (headers == null) {
            options.addHeader(MESSAGE_ID.header, uuid());
            options.addHeader(MESSAGE_TIMESTAMP.header, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            return options;
        }

        if (!headers.contains(MESSAGE_ID.header)) {
            headers.add(MESSAGE_ID.header, uuid());
        }

        if (!headers.contains(MESSAGE_TIMESTAMP.header)) {
            headers.add(MESSAGE_TIMESTAMP.header, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        }
        return options;
    }

    private void registerMessageCodec(final A defaultInstance) {
        try {
            final MessageCodec codec = new ProtobufMessageCodec(defaultInstance);
            eventBus.registerDefaultCodec(defaultInstance.getClass(), codec);
        } catch (final IllegalStateException e) {
            log.logp(FINE, "ProtobufMessageProducer", "registerMessageCodec", "failed to register codec for request message", e.getCause());
        }
    }

}
