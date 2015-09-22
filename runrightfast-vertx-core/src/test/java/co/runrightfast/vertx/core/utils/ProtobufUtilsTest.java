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
package co.runrightfast.vertx.core.utils;

import co.runrightfast.core.utils.JsonUtils;
import co.runrightfast.core.utils.ProtobufUtils;
import co.runrightfast.protobuf.test.ApplicationInstance;
import co.runrightfast.protobuf.test.ProtobufUtilsToJsonTestMessage;
import static co.runrightfast.core.utils.ProtobufUtils.TYPE_FIELD;
import com.google.protobuf.ByteString;
import java.util.Base64;
import javax.json.JsonObject;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class ProtobufUtilsTest {

    /**
     * Test of protobuMessageToJson method, of class ProtobufUtils.
     */
    @Test
    public void testToJson_empty_object() {
        final ProtobufUtilsToJsonTestMessage msg = ProtobufUtilsToJsonTestMessage.newBuilder().build();
        final JsonObject json = ProtobufUtils.protobuMessageToJson(msg);
        log.info(String.format("testToJson_empty_object:\n%s", JsonUtils.toVertxJsonObject(json).encodePrettily()));
        assertThat(json.getString(TYPE_FIELD), is(msg.getDescriptorForType().getFullName()));
    }

    @Test
    public void testToJson_simple_field() {
        final ProtobufUtilsToJsonTestMessage msg = ProtobufUtilsToJsonTestMessage.newBuilder()
                .setBoolField(true)
                .setBytesField(ByteString.copyFrom("TEST".getBytes()))
                .setDoubleField(23d)
                .setFloatField(46f)
                .setIntField(1)
                .setLongField(2l)
                .setMsgField(ApplicationInstance.newBuilder().setAppName("TEST_APP"))
                .build();
        final JsonObject json = ProtobufUtils.protobuMessageToJson(msg);
        log.info(String.format("testToJson_empty_object:\n%s", JsonUtils.toVertxJsonObject(json).encodePrettily()));
        assertThat(json.getString(TYPE_FIELD), is(msg.getDescriptorForType().getFullName()));
        assertThat(json.getBoolean("bool_field"), is(true));
        assertThat(json.getString("bytes_field"), is(Base64.getEncoder().encodeToString(ByteString.copyFrom("TEST".getBytes()).toByteArray())));
        final ByteString bytes = ByteString.copyFrom(Base64.getDecoder().decode(json.getString("bytes_field")));
        assertThat(bytes.toStringUtf8(), is("TEST"));
    }

    @Test
    public void testToJson_repeated_fields() {
        final ProtobufUtilsToJsonTestMessage msg = ProtobufUtilsToJsonTestMessage.newBuilder()
                .addRepeatedBoolField(true)
                .addRepeatedBytesField(ByteString.copyFrom("TEST".getBytes()))
                .addRepeatedDoubleField(23d)
                .addRepeatedFloatField(46f)
                .addRepeatedIntField(1)
                .addRepeatedLongField(2l)
                .addRepeatedMsgField(ApplicationInstance.newBuilder().setAppName("TEST_APP"))
                .build();
        final JsonObject json = ProtobufUtils.protobuMessageToJson(msg);
        log.info(String.format("testToJson_empty_object:\n%s", JsonUtils.toVertxJsonObject(json).encodePrettily()));
        assertThat(json.getString(TYPE_FIELD), is(msg.getDescriptorForType().getFullName()));
        assertThat(json.getJsonArray("repeated_bool_field").getBoolean(0), is(true));
        assertThat(json.getJsonArray("repeated_bytes_field").getString(0), is(Base64.getEncoder().encodeToString(ByteString.copyFrom("TEST".getBytes()).toByteArray())));
    }

    @Test
    public void testToJson_map_fields() {
        final ProtobufUtilsToJsonTestMessage.Builder msgBuilder = ProtobufUtilsToJsonTestMessage.newBuilder();
        msgBuilder.getMutableMapField().put("test_app", ApplicationInstance.newBuilder().setAppName("TEST_APP").build());
        final ProtobufUtilsToJsonTestMessage msg = msgBuilder.build();
        final JsonObject json = ProtobufUtils.protobuMessageToJson(msg);

        log.info(String.format("testToJson_empty_object:\n%s", JsonUtils.toVertxJsonObject(json).encodePrettily()));
        assertThat(json.getString(TYPE_FIELD), is(msg.getDescriptorForType().getFullName()));
        assertThat(json.getJsonObject("map_field").getJsonObject("test_app").getString(TYPE_FIELD), is(ApplicationInstance.getDescriptor().getFullName()));
    }
}
