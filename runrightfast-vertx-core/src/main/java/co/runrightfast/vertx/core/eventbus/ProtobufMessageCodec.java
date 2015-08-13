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
import co.runrightfast.core.crypto.CipherFunctions;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 * @param <MSG> Message payload type
 */
@EqualsAndHashCode(of = "defaultInstance")
public final class ProtobufMessageCodec<MSG extends Message> implements MessageCodec<MSG, MSG> {

    private static ImmutableMap<String, ProtobufMessageCodec> protobufMessageCodecs = ImmutableMap.of();

    public static Optional<ProtobufMessageCodec> getProtobufMessageCodec(@NonNull final Message msg) {
        return Optional.ofNullable(protobufMessageCodecs.get(msg.getDescriptorForType().getFullName()));
    }

    @Getter
    private final MSG defaultInstance;

    private final CipherFunctions ciphers;

    /**
     * Each time an instance is created, it registers itself and becomes available via {@link ProtobufMessageCodec#getProtobufMessageCodec(com.google.protobuf.Message)
     * }. If a ProtobufMessageCodec for the same {@link Message} type exists, then it will be overwritten.
     *
     *
     * @param defaultInstance
     * @param ciphers
     */
    public ProtobufMessageCodec(@NonNull final MSG defaultInstance, @NonNull final CipherFunctions ciphers) {
        this.defaultInstance = defaultInstance;
        this.ciphers = ciphers;

        final Map<String, ProtobufMessageCodec> temp = new HashMap<>(protobufMessageCodecs);
        temp.put(defaultInstance.getDescriptorForType().getFullName(), this);
        protobufMessageCodecs = ImmutableMap.copyOf(temp);
    }

    @Override
    public void encodeToWire(final Buffer buffer, final MSG msg) {
        buffer.appendBytes(ciphers.getEncryption().apply(msg.toByteArray()));
    }

    @Override
    public MSG decodeFromWire(final int pos, final Buffer buffer) {
        try {
            return (MSG) defaultInstance.getParserForType().parseFrom(ciphers.getDecryption().apply(buffer.getBytes()));
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
