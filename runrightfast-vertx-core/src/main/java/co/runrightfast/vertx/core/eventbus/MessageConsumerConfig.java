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

import co.runrightfast.core.crypto.CipherFunctions;
import static co.runrightfast.vertx.core.eventbus.MessageConsumerConfig.ExecutionMode.EVENT_LOOP;
import static co.runrightfast.vertx.core.eventbus.MessageConsumerConfig.Failure.INTERNAL_SERVER_ERROR;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import java.util.Map;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 * @param <REQUEST> Request message payload type
 * @param <RESPONSE> Response message payload type
 */
@EqualsAndHashCode(of = {"addressMessageMapping"})
public final class MessageConsumerConfig<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

    /**
     * Automatically maps the following exceptions
     *
     * <ol>
     * <li>{@link InvalidMessageException} -&gt; {@link Failure#BAD_REQUEST}
     * <li>{@link UnauthorizedException} -&gt; {@link Failure#UNAUTHORIZED}
     * <li>{@link ForbiddenException} -&gt; {@link Failure#FORBIDDEN}
     * <li>{@link ResourceNotFoundException} -&gt; {@link Failure#NOT_FOUND}
     * <li>{@link RequestTimeoutException} -&gt; {@link Failure#REQUEST_TIMEOUT}
     * <li>{@link ResourceConflictException} -&gt; {@link Failure#CONFLICT}
     * <li>{@link PreconditionFailedException} -&gt; {@link Failure#REQUEST_ENTITY_TOO_LARGE}
     * <li>{@link MessageTooLargeException} -&gt; {@link Failure#BAD_REQUEST}
     * <li>{@link NotImplementedException} -&gt; {@link Failure#NOT_IMPLEMENTED}
     * <li>{@link ServiceNotAvailableException} -&gt; {@link Failure#SERVICE_UNAVAILABLE}
     * </ol>
     *
     * Any unmapped exception is mapped to {@link Failure#INTERNAL_SERVER_ERROR}
     *
     * @param <REQUEST>
     * @param <RESPONSE>
     */
    public static final class Builder<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

        private final MessageConsumerConfig config = new MessageConsumerConfig();
        private ImmutableMap.Builder<Class<? extends Throwable>, Failure> exceptionFailureMap = ImmutableMap.<Class<? extends Throwable>, Failure>builder()
                .put(InvalidMessageException.class, Failure.BAD_REQUEST)
                .put(UnauthorizedException.class, Failure.UNAUTHORIZED)
                .put(ForbiddenException.class, Failure.FORBIDDEN)
                .put(ResourceNotFoundException.class, Failure.NOT_FOUND)
                .put(RequestTimeoutException.class, Failure.REQUEST_TIMEOUT)
                .put(ResourceConflictException.class, Failure.CONFLICT)
                .put(PreconditionFailedException.class, Failure.PRECONDITION_FAILED)
                .put(MessageTooLargeException.class, Failure.REQUEST_ENTITY_TOO_LARGE)
                .put(NotImplementedException.class, Failure.NOT_IMPLEMENTED)
                .put(ServiceNotAvailableException.class, Failure.SERVICE_UNAVAILABLE);

        private Builder() {
        }

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

        public Builder<REQUEST, RESPONSE> handler(@NonNull final Handler<Message<REQUEST>> handler) {
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

        public Builder<REQUEST, RESPONSE> addExceptionFailureMapping(@NonNull final Class<? extends Throwable> exceptionClass, @NonNull final Failure failure) {
            this.exceptionFailureMap.put(exceptionClass, failure);
            return this;
        }

        public Builder<REQUEST, RESPONSE> ciphers(@NonNull final CipherFunctions ciphers) {
            this.config.ciphers = ciphers;
            return this;
        }

        public Builder<REQUEST, RESPONSE> executionMode(@NonNull final ExecutionMode executionMode) {
            this.config.executionMode = executionMode;
            return this;
        }

        public MessageConsumerConfig build() {
            config.exceptionFailureMap = this.exceptionFailureMap.build();
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

    @Getter
    private Map<Class<? extends Throwable>, Failure> exceptionFailureMap = ImmutableMap.of();

    @Getter
    private CipherFunctions ciphers;

    @Getter
    private ExecutionMode executionMode = EVENT_LOOP;

    private MessageConsumerConfig() {
    }

    public void validate() {
        checkNotNull(addressMessageMapping);
        checkNotNull(handler);
        checkNotNull(ciphers);
        checkState(maxBufferedMessages >= 0);
    }

    public String address() {
        return addressMessageMapping.getAddress();
    }

    public Failure toFailure(@NonNull final Throwable t) {
        final Class<? extends Throwable> clazz = t.getClass();
        final Failure failure = exceptionFailureMap.get(clazz);
        if (failure != null) {
            return new Failure(failure, t);
        }

        return new Failure(exceptionFailureMap.entrySet().stream()
                .filter(entry -> entry.getClass().isAssignableFrom(clazz))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(INTERNAL_SERVER_ERROR), t);

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

    public static enum ExecutionMode {

        /**
         * The Golden Rule - Don’t Block the Event Loop. If you do that, then that event loop will not be able to do anything else while it’s blocked. If you
         * block all of the event loops in Vertx instance then your application will grind to a complete halt!
         */
        EVENT_LOOP,
        /**
         * Process messages serially using the worker pool
         */
        WORKER_POOL_SERIAL,
        /**
         * Process messages in parallel using the worker pool
         */
        WORKER_POOL_PARALLEL
    }

    @lombok.Builder
    public static final class Failure {

        public static final Failure BAD_REQUEST = new Failure(400, "Invalid message");
        public static final Failure UNAUTHORIZED = new Failure(401, "Unauthorized");
        public static final Failure FORBIDDEN = new Failure(403, "Forbidden");
        public static final Failure NOT_FOUND = new Failure(404, "Not found");
        public static final Failure REQUEST_TIMEOUT = new Failure(408, "Timeout");
        public static final Failure CONFLICT = new Failure(409, "Conflict");
        public static final Failure PRECONDITION_FAILED = new Failure(412, "Precondition failed");
        public static final Failure REQUEST_ENTITY_TOO_LARGE = new Failure(413, "Message too large");

        public static final Failure INTERNAL_SERVER_ERROR = new Failure(500, "Unexpected server error");
        public static final Failure NOT_IMPLEMENTED = new Failure(501, "Not supported");
        public static final Failure SERVICE_UNAVAILABLE = new Failure(503, "Service unavailable");

        @Getter
        private final int code;

        @Getter
        private final String message;

        public Failure(final int code, final String message) {
            checkArgument(isNotBlank(message));
            this.code = code;
            this.message = message;
        }

        public Failure(@NonNull final Failure failure, @NonNull final Throwable t) {
            this.code = failure.code;
            if (StringUtils.isNotBlank(t.getMessage())) {
                final String exceptionMessage = t.getMessage();
                this.message = new StringBuilder(failure.message.length() + t.getMessage().length() + 3)
                        .append(failure.message)
                        .append(" : ")
                        .append(exceptionMessage)
                        .toString();
            } else {
                this.message = failure.message;
            }
        }

    }

}
