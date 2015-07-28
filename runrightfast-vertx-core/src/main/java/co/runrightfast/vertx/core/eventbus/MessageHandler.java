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

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import java.util.Arrays;
import java.util.function.Function;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;

/**
 * The purpose of this functional interface is to provide the ability to compose message handler from other message handlers.
 *
 *
 * @author alfio
 * @param <MSG> message payload type
 */
@FunctionalInterface
public interface MessageHandler<MSG extends com.google.protobuf.Message> extends Function<Message<MSG>, Message<MSG>> {

    /**
     *
     * @param <MSG> Message payload type
     * @param messageProcessor1 the first processor in the chain
     * @param messageProcessor2 the second processor in the chain
     * @param chain optional - the rest of the chain
     * @return handler
     */
    static <MSG extends com.google.protobuf.Message> Handler<Message<MSG>> composeHandler(@NonNull final MessageHandler<MSG> messageProcessor1, @NonNull final MessageHandler<MSG> messageProcessor2, final MessageHandler<MSG>... chain) {
        if (ArrayUtils.isEmpty(chain)) {
            return message -> messageProcessor1.andThen(messageProcessor2).apply(message);
        }

        final Function<Message<MSG>, Message<MSG>> composedProcessor = Arrays.stream(chain).sequential().reduce(
                messageProcessor1.andThen(messageProcessor2),
                (p1, p2) -> p1.andThen(p2),
                (p1, p2) -> p1.andThen(p2)
        );
        return message -> composedProcessor.apply(message);
    }

}
