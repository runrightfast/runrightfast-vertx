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

import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig.Failure;
import co.runrightfast.core.utils.JsonUtils;
import io.vertx.core.eventbus.Message;
import java.time.Instant;
import java.util.Optional;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alfio
 */
public enum MessageHeader {

    MESSAGE_ID("rrf-msg-id"),
    MESSAGE_CORRELATION_ID("rrf-msg-correlation"),
    MESSAGE_TIMESTAMP("rrf-msg-ts"),
    /**
     * event bus address
     */
    REPLY_TO_ADDRESS("rrf-reply-address"),
    /**
     * event bus address
     */
    FROM_ADDRESS("rrf-from-address"),
    /**
     * JVM ID
     */
    FROM_JVM("rrf-from-jvm"),
    /**
     * Verticle deployment id
     */
    FROM_VERTICLE("rrf-verticle-deployment-id"),
    /**
     * if set, then this indicates the message failed to process.
     *
     * The message header will contain the Json Representation for {@link Failure}.
     *
     * The message body will contain a {@link co.runrightfast.vertx.core.messages.Void} message.
     */
    FAILURE("rrf-failure");

    public final String header;

    private MessageHeader(final String header) {
        this.header = header;
    }

    public static Optional<String> getMessageId(@NonNull final Message message) {
        return Optional.ofNullable(message.headers().get(MESSAGE_ID.header));
    }

    public static Optional<String> getCorrelationId(@NonNull final Message message) {
        return Optional.ofNullable(message.headers().get(MESSAGE_CORRELATION_ID.header));
    }

    public static Optional<Instant> getMessageTimestamp(@NonNull final Message message) {
        return Optional.ofNullable(message.headers().get(MESSAGE_TIMESTAMP.header)).map(Instant::parse);
    }

    public static Optional<String> getReplyToAddress(@NonNull final Message message) {
        return Optional.ofNullable(message.headers().get(REPLY_TO_ADDRESS.header));
    }

    public static Optional<String> getFromAddress(@NonNull final Message message) {
        return Optional.ofNullable(message.headers().get(FROM_ADDRESS.header));
    }

    public static Optional<Failure> getFailure(@NonNull final Message message) {
        final String failureJson = message.headers().get(FAILURE.header);
        if (StringUtils.isNotBlank(failureJson)) {
            return Optional.of(new Failure(JsonUtils.parse(failureJson)));
        }

        return Optional.empty();
    }

}
