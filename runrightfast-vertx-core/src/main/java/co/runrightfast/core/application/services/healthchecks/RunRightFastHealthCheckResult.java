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

import com.codahale.metrics.health.HealthCheck;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@Builder
public final class RunRightFastHealthCheckResult {

    @NonNull
    @Getter
    private final HealthCheckConfig config;

    @NonNull
    @Getter
    private final HealthCheck.Result result;
}
