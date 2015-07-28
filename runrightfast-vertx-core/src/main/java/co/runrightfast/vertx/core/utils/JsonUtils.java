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

import static com.google.common.base.Preconditions.checkArgument;
import io.vertx.core.json.JsonObject;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonReader;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public interface JsonUtils {

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

}
