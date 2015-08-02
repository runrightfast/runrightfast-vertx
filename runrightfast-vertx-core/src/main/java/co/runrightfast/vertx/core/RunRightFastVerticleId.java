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
package co.runrightfast.vertx.core;

import co.runrightfast.vertx.core.verticles.verticleManager.messages.VerticleId;
import static com.google.common.base.Preconditions.checkArgument;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Data
@Builder
public final class RunRightFastVerticleId {

    public static final String RUNRIGHTFAST_GROUP = "co.runrightfast";

    private final String group;

    private final String name;

    private final String version;

    public RunRightFastVerticleId(final String group, final String name, final String version) {
        checkArgument(isNotBlank(group), "group");
        checkArgument(isNotBlank(name), "name");
        checkArgument(isNotBlank(version), "version");
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public RunRightFastVerticleId(@NonNull final JsonObject json) {
        this(
                json.getString("group"),
                json.getString("name"),
                json.getString("version")
        );
    }

    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("group", group)
                .add("name", name)
                .add("version", version)
                .build();
    }

    /**
     *
     * @return JSON representation
     */
    @Override
    public String toString() {
        return toJson().toString();
    }

    public boolean equalsVerticleId(final VerticleId id) {
        if (id == null) {
            return false;
        }

        return id.getGroup().equals(group) && id.getName().equals(name) && id.getVersion().equals(version);
    }

}
