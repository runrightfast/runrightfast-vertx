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

import static co.runrightfast.vertx.core.eventbus.MessageHeader.FROM;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.FROM_ADDRESS;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.MESSAGE_CORRELATION_ID;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.MESSAGE_ID;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.MESSAGE_TIMESTAMP;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.REPLY_TO_ADDRESS;
import static co.runrightfast.vertx.core.eventbus.MessageHeader.getMessageId;
import static co.runrightfast.vertx.core.utils.JvmProcess.JVM_ID;
import static co.runrightfast.vertx.core.utils.UUIDUtils.uuid;
import static com.google.common.base.Preconditions.checkArgument;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public interface EventBusUtils {

    /**
     * Adds the following headers:
     *
     * <ul>
     * <li>{@link MessageHeader#MESSAGE_ID}
     * <li>{@link MessageHeader#MESSAGE_TIMESTAMP}
     * </ul>
     *
     * @return DeliveryOptions
     */
    static DeliveryOptions deliveryOptions() {
        final DeliveryOptions options = new DeliveryOptions();
        options.addHeader(MESSAGE_ID.header, uuid());
        options.addHeader(MESSAGE_TIMESTAMP.header, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        options.addHeader(FROM.header, JVM_ID);
        return options;
    }

    /**
     * Adds the following headers:
     *
     * <ul>
     * <li>{@link MessageHeader#MESSAGE_ID}
     * <li>{@link MessageHeader#MESSAGE_TIMESTAMP}
     * <li>If the request message has a message id, then add header <b>{@link MessageHeader#MESSAGE_CORRELATION_ID}</b> using the request message's message id.
     * <li>{@link MessageHeader#FROM_ADDRESS}
     * </ul>
     *
     * @param requestMessage
     * @return DeliveryOptions
     */
    static DeliveryOptions responseDeliveryOptions(@NonNull final Message requestMessage) {
        return withFromAddress(
                getMessageId(requestMessage).map(messageId -> withCorrelationId(deliveryOptions(), messageId)).orElseGet(EventBusUtils::deliveryOptions),
                requestMessage.address()
        );
    }

    /**
     * Adds the following headers:
     *
     * <ul>
     * <li>{@link MessageHeader#MESSAGE_ID}
     * <li>{@link MessageHeader#MESSAGE_TIMESTAMP}
     * <li>If the request message has a message id, then add header <b>{@link MessageHeader#MESSAGE_CORRELATION_ID}</b> using the request message's message id.
     * <li>{@link MessageHeader#FROM_ADDRESS}
     * <li>{@link MessageHeader#REPLY_TO_ADDRESS}
     * </ul>
     *
     * @param requestMessage
     * @param replyToAddress
     * @return DeliveryOptions
     */
    static DeliveryOptions responseDeliveryOptions(@NonNull final Message requestMessage, final String replyToAddress) {
        return withReplyToAddress(responseDeliveryOptions(requestMessage), replyToAddress);
    }

    /**
     * Adds the {@link MessageHeader#REPLY_TO_ADDRESS} header.
     *
     * @param deliveryOptions
     * @param replyToAddress
     * @return DeliveryOptions
     */
    static DeliveryOptions withReplyToAddress(final DeliveryOptions deliveryOptions, final String replyToAddress) {
        checkArgument(isNotBlank(replyToAddress));
        deliveryOptions.addHeader(REPLY_TO_ADDRESS.header, replyToAddress);
        return deliveryOptions;
    }

    /**
     * Adds the {@link MessageHeader#MESSAGE_CORRELATION_ID} header.
     *
     * @param deliveryOptions
     * @param correlationId
     * @return DeliveryOptions
     */
    static DeliveryOptions withCorrelationId(final DeliveryOptions deliveryOptions, final String correlationId) {
        checkArgument(isNotBlank(correlationId));
        deliveryOptions.addHeader(MESSAGE_CORRELATION_ID.header, correlationId);
        return deliveryOptions;
    }

    /**
     * Adds the {@link MessageHeader#FROM_ADDRESS} header.
     *
     * @param deliveryOptions
     * @param fromAddress
     * @return DeliveryOptions
     */
    static DeliveryOptions withFromAddress(final DeliveryOptions deliveryOptions, final String fromAddress) {
        checkArgument(isNotBlank(fromAddress));
        deliveryOptions.addHeader(FROM_ADDRESS.header, fromAddress);
        return deliveryOptions;
    }
}
