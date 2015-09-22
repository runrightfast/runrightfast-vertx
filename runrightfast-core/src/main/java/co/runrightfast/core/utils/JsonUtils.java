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
package co.runrightfast.core.utils;

import static com.google.common.base.Preconditions.checkArgument;
import io.vertx.core.json.JsonObject;
import java.io.StringReader;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonReader;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public interface JsonUtils {

    static final javax.json.JsonObject EMPTY_OBJECT = Json.createObjectBuilder().build();
    static final javax.json.JsonArray EMPTY_ARRAY = Json.createArrayBuilder().build();

    static JsonObject toVertxJsonObject(@NonNull final javax.json.JsonObject json) {
        return new JsonObject(json.toString());
    }

    static javax.json.JsonObject toJsonObject(@NonNull final JsonObject json) {
        try (final JsonReader reader = Json.createReader(new StringReader(json.encode()))) {
            return reader.readObject();
        }
    }

    static javax.json.JsonObject parse(final String json) {
        checkArgument(isNotBlank(json));
        try (final JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    static javax.json.JsonArray toJsonArray(final List<String> stringList) {
        if (CollectionUtils.isEmpty(stringList)) {
            return EMPTY_ARRAY;
        }

        final JsonArrayBuilder json = Json.createArrayBuilder();
        stringList.forEach(json::add);
        return json.build();
    }

    static JsonArray toJsonArray(final String[] stringList) {
        if (ArrayUtils.isEmpty(stringList)) {
            return EMPTY_ARRAY;
        }

        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (int i = 0; i < stringList.length; i++) {
            builder.add(stringList[i]);
        }
        return builder.build();
    }

}
