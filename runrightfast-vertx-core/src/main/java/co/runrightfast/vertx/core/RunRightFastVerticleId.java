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

import static com.google.common.base.Preconditions.checkArgument;
import lombok.Builder;
import lombok.Data;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Data
@Builder
public final class RunRightFastVerticleId {

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

}
