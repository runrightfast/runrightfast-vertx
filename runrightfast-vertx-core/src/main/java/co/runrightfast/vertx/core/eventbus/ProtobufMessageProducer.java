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

import static co.runrightfast.vertx.core.utils.PreconditionsUtils.isNotBlank;
import com.google.protobuf.Message;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 * @param <A> message type
 */
public final class ProtobufMessageProducer<A extends Message> {

    private final EventBus eventBus;

    @Getter
    private final String address;

    public ProtobufMessageProducer(@NonNull final EventBus eventBus, final String address, @NonNull final A defaultInstance) {
        isNotBlank(address);
        this.eventBus = eventBus;
        this.address = address;

        registerMessageCodec(defaultInstance);
    }

    public void send(@NonNull final A msg) {

    }

    public void send(@NonNull final A msg, @NonNull final DeliveryOptions options) {

    }

    public void publish(@NonNull final A msg) {

    }

    public void publish(@NonNull final A msg, @NonNull final DeliveryOptions options) {

    }

    private void registerMessageCodec(final A defaultInstance) {
        final MessageCodec codec = new ProtobufMessageCodec(defaultInstance);
        eventBus.registerDefaultCodec(defaultInstance.getClass(), codec);
    }

}
