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
package co.runrightfast.vertx.core.application;

import co.runrightfast.core.JsonRepresentation;
import static co.runrightfast.core.utils.ConfigUtils.configPath;
import com.typesafe.config.Config;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@Builder
@Data
public final class ApplicationId implements JsonRepresentation {

    private final String group;

    private final String name;

    private final String version;

    /**
     * Config schema:
     *
     * <code>
     * {
     *    app {
     *       group = co.runrightfast
     *       name = security-service
     *       version = 1.0.0
     *    }
     * }
     * </code>
     *
     * @param config Config
     * @return ApplicatioId
     */
    public static ApplicationId fromConfig(@NonNull final Config config) {
        return builder()
                .group(config.getString(configPath("app", "group")))
                .name(config.getString(configPath("app", "name")))
                .version(config.getString(configPath("app", "version")))
                .build();
    }

    @Override
    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("group", group)
                .add("name", name)
                .add("version", version)
                .build();
    }

}
