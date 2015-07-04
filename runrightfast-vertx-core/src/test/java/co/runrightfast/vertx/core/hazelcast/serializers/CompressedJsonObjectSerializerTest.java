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

import static com.google.common.base.Charsets.UTF_8;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
public class CompressedJsonObjectSerializerTest {

    private final CompressedJsonObjectSerializer serializer = new CompressedJsonObjectSerializer();

    private static final Logger log = Logger.getLogger(CompressedJsonObjectSerializerTest.class.getSimpleName());

    /**
     * Test of write method, of class CompressedJsonObjectSerializer.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test() throws Exception {
        final JsonObject json = Json.createObjectBuilder().add("a", 1).build();
        final byte[] bytes = serializer.write(json);
        log.log(INFO, "bytes.length = {0}", bytes.length);
        log.log(INFO, "uncompressed bytes.length = {0}", json.toString().getBytes(UTF_8).length);
        final JsonObject json2 = serializer.read(bytes);
        assertThat(json.getInt("a"), is(1));
        assertThat(json.getInt("a"), is(json2.getInt("a")));
    }

}
