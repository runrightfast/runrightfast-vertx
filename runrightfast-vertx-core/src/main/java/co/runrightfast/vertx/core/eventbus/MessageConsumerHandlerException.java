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

import co.runrightfast.core.ApplicationException;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import lombok.Getter;

/**
 *
 * @author alfio
 */
public class MessageConsumerHandlerException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final io.vertx.core.eventbus.Message<? extends Message> failedMessage;

    public MessageConsumerHandlerException(final io.vertx.core.eventbus.Message<? extends Message> failedMessage, final Throwable cause) {
        super(cause);
        checkNotNull(failedMessage);
        this.failedMessage = failedMessage;
    }

}
