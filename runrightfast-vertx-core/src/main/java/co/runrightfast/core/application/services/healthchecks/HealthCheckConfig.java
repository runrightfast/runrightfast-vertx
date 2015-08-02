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
package co.runrightfast.core.application.services.healthchecks;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Builder
@EqualsAndHashCode(of = {"registryName", "name"})
public final class HealthCheckConfig {

    @Getter
    private final String registryName;

    @Getter
    private final String name;

    @Getter
    private final FailureSeverity severity;

    @Getter
    @Singular
    private final Set<String> tags;

    public static enum FailureSeverity {

        FATAL(1000),
        HIGH(800),
        MEDIUM(500),
        LOW(100);

        public final int level;

        private FailureSeverity(final int level) {
            this.level = level;
        }

    }

    /* @param severity FailureSeverity
     * @param tags optional set of tags for the healthcheck that can be used to categorize the healthcheck, e.g, web, database, elasticsearch, vertx, orientdb
     */
    public HealthCheckConfig(final String registry, final String name, @NonNull final FailureSeverity severity, @NonNull final Set<String> tags) {
        checkArgument(isNotBlank(registry));
        checkArgument(isNotBlank(name));
        this.registryName = registry;
        this.name = name;
        this.severity = severity;
        this.tags = ImmutableSet.copyOf(tags);
    }

}
