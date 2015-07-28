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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author alfio
 * @param <MSG> Message payload type
 */
@RequiredArgsConstructor
public class ProtobufMessageCodec<MSG extends Message> implements MessageCodec<MSG, MSG> {

    @NonNull
    @Getter
    private final MSG defaultInstance;

    @Override
    public void encodeToWire(final Buffer buffer, final MSG msg) {
        buffer.appendBytes(msg.toByteArray());
    }

    @Override
    public MSG decodeFromWire(final int pos, final Buffer buffer) {
        try {
            return (MSG) defaultInstance.getParserForType().parseFrom(buffer.getBytes());
        } catch (final InvalidProtocolBufferException ex) {
            throw new ApplicationException(ex);
        }
    }

    @Override
    public MSG transform(final MSG msg) {
        return msg;
    }

    @Override
    public String name() {
        return defaultInstance.getDescriptorForType().getFullName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

}
