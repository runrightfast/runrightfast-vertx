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

import static com.google.common.base.Preconditions.checkState;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 * @param <REQUEST> Request message payload type
 * @param <RESPONSE> Response message payload type
 */
@Builder
@EqualsAndHashCode(of = {"addressMessageMapping"})
public final class MessageConsumerConfig<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

    @Getter
    @NonNull
    private final EventBusAddressMessageMapping addressMessageMapping;

    @Getter
    private final boolean local;

    /**
     * the maximum number of messages that can be buffered when this stream is paused
     */
    @Getter
    private int maxBufferedMessages;

    @Getter
    private final Handler<Message<REQUEST>> handler;

    private Optional<Handler<AsyncResult<Void>>> completionHandler = Optional.empty();

    private Optional<Handler<Void>> endHandler = Optional.empty();

    private Optional<Handler<Throwable>> exceptionHandler = Optional.empty();

    public Optional<Handler<AsyncResult<Void>>> getCompletionHandler() {
        return completionHandler != null ? completionHandler : Optional.empty();
    }

    public Optional<Handler<Void>> getEndHandler() {
        return endHandler != null ? endHandler : Optional.empty();
    }

    public Optional<Handler<Throwable>> getExceptionHandler() {
        return exceptionHandler != null ? exceptionHandler : Optional.empty();
    }

    public void validate() {
        addressMessageMapping.validate();
        checkState(maxBufferedMessages > 0);
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
