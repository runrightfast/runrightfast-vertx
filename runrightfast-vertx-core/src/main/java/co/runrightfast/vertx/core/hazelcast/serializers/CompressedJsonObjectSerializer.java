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
package co.runrightfast.vertx.core.hazelcast.serializers;

import com.hazelcast.nio.serialization.ByteArraySerializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 *
 * @author alfio
 */
public final class CompressedJsonObjectSerializer implements ByteArraySerializer<JsonObject> {

    public static final int DEFAULT_SERIALIZER_ID = 1;

    private final int typeId;

    public CompressedJsonObjectSerializer() {
        this(DEFAULT_SERIALIZER_ID);
    }

    public CompressedJsonObjectSerializer(final int typeId) {
        this.typeId = typeId;
    }

    @Override
    public byte[] write(final JsonObject json) throws IOException {
        final byte[] bytes = json.toString().getBytes(UTF_8);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        try (final GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(bytes);
        }
        return bos.toByteArray();
    }

    @Override
    public JsonObject read(final byte[] bytes) throws IOException {
        try (final JsonReader reader = Json.createReader(new GZIPInputStream(new ByteArrayInputStream(bytes)))) {
            return reader.readObject();
        }
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {
    }

}
