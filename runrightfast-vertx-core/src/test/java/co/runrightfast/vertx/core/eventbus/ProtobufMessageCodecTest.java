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

import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.core.crypto.impl.EncryptionServiceImpl;
import co.runrightfast.vertx.core.verticles.messages.VerticleId;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.buffer.Buffer;
import java.security.Key;
import java.util.Map;
import lombok.extern.java.Log;
import org.apache.shiro.crypto.AesCipherService;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class ProtobufMessageCodecTest {

    final AesCipherService aes = new AesCipherService();
    final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
            .put(VerticleId.getDescriptor().getFullName(), aes.generateNewKey())
            .build();

    final EncryptionService encryptionService = new EncryptionServiceImpl(aes, keys);

    private final VerticleId verticleId = VerticleId.newBuilder()
            .setGroup("runrightfast")
            .setName(getClass().getSimpleName())
            .setVersion("1.0.0")
            .build();

    private final ProtobufMessageCodec<VerticleId> verticleIdMessageCodec = new ProtobufMessageCodec(VerticleId.getDefaultInstance());

    /**
     * Test of encodeToWire method, of class ProtobufMessageCodec.
     */
    @Test
    public void testEncodeDecode() {
        final Buffer buff = Buffer.buffer();
        verticleIdMessageCodec.encodeToWire(buff, verticleId);

        final VerticleId verticleId2 = verticleIdMessageCodec.decodeFromWire(0, buff);

        assertThat(verticleId, is(verticleId2));
    }

    /**
     * Test of transform method, of class ProtobufMessageCodec.
     */
    @Test
    public void testTransform() {
        assertThat(verticleIdMessageCodec.transform(verticleId), is(sameInstance(verticleId)));
    }

    /**
     * Test of name method, of class ProtobufMessageCodec.
     */
    @Test
    public void testName() {
        assertThat(verticleIdMessageCodec.name(), is(verticleId.getDescriptorForType().getFullName()));
    }

    /**
     * Test of systemCodecID method, of class ProtobufMessageCodec.
     */
    @Test
    public void testSystemCodecID() {
        assertThat(verticleIdMessageCodec.systemCodecID(), is((byte) -1));
    }

    /**
     * Test of getDefaultInstance method, of class ProtobufMessageCodec.
     */
    @Test
    public void testGetDefaultInstance() {
        assertThat(verticleIdMessageCodec.getDefaultInstance(), is(notNullValue()));
        assertThat(verticleIdMessageCodec.getDefaultInstance(), is(VerticleId.getDefaultInstance()));
    }

}
