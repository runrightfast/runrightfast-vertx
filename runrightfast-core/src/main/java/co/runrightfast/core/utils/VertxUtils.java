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

import static co.runrightfast.core.utils.JsonUtils.EMPTY_OBJECT;
import io.vertx.core.MultiMap;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public interface VertxUtils {

    static JsonObject toJsonObject(@NonNull final MultiMap map) {
        if (map.isEmpty()) {
            return EMPTY_OBJECT;
        }

        final JsonObjectBuilder json = Json.createObjectBuilder();
        map.names().stream().forEach(name -> {
            final List<String> values = map.getAll(name);
            if (values.size() == 1) {
                json.add(name, values.get(0));
            } else {
                json.add(name, JsonUtils.toJsonArray(values));
            }
        });
        return json.build();
    }

}
