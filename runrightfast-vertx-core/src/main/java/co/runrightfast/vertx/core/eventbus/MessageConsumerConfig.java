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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 *
 * @author alfio
 * @param <REQUEST> Request message payload type
 * @param <RESPONSE> Response message payload type
 */
@EqualsAndHashCode(of = {"addressMessageMapping"})
public final class MessageConsumerConfig<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

    public static final class Builder<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

        private final MessageConsumerConfig config = new MessageConsumerConfig();

        public Builder<REQUEST, RESPONSE> addressMessageMapping(final EventBusAddressMessageMapping mapping) {
            this.config.addressMessageMapping = mapping;
            return this;
        }

        public Builder<REQUEST, RESPONSE> local(final boolean local) {
            this.config.local = local;
            return this;
        }

        public Builder<REQUEST, RESPONSE> maxBufferedMessages(final int maxBufferedMessages) {
            this.config.maxBufferedMessages = maxBufferedMessages;
            return this;
        }

        public Builder<REQUEST, RESPONSE> handler(final Handler<Message<REQUEST>> handler) {
            this.config.handler = handler;
            return this;
        }

        public Builder<REQUEST, RESPONSE> completionHandler(final Handler<AsyncResult<Void>> completionHandler) {
            this.config.completionHandler = Optional.ofNullable(completionHandler);
            return this;
        }

        public Builder<REQUEST, RESPONSE> endHandler(final Handler<Void> endHandler) {
            this.config.endHandler = Optional.ofNullable(endHandler);
            return this;
        }

        public Builder<REQUEST, RESPONSE> exceptionHandler(final Handler<Throwable> exceptionHandler) {
            this.config.exceptionHandler = Optional.ofNullable(exceptionHandler);
            return this;
        }

        public MessageConsumerConfig build() {
            config.validate();
            return config;
        }
    }

    public static <REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> Builder<REQUEST, RESPONSE> builder() {
        return new Builder<>();
    }

    @Getter
    private EventBusAddressMessageMapping addressMessageMapping;

    @Getter
    private boolean local;

    /**
     * the maximum number of messages that can be buffered when this stream is paused
     */
    @Getter
    private int maxBufferedMessages;

    @Getter
    private Handler<Message<REQUEST>> handler;

    @Getter
    private Optional<Handler<AsyncResult<Void>>> completionHandler = Optional.empty();

    @Getter
    private Optional<Handler<Void>> endHandler = Optional.empty();

    @Getter
    private Optional<Handler<Throwable>> exceptionHandler = Optional.empty();

    private MessageConsumerConfig() {
    }

    public void validate() {
        checkNotNull(addressMessageMapping);
        checkNotNull(handler);
        checkState(maxBufferedMessages >= 0);
    }

    public String address() {
        return addressMessageMapping.getAddress();
    }

    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("addressMessageMapping", addressMessageMapping.toJson())
                .add("local", local)
                .add("maxBufferedMessages", maxBufferedMessages)
                .build();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

}
