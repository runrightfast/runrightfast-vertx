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
package co.runrightfast.vertx.demo.verticles;

import static co.runrightfast.core.application.services.healthchecks.HealthCheckConfig.FailureSeverity.FATAL;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Getter;

/**
 *
 * @author alfio
 */
public final class TestVerticle extends RunRightFastVerticle {

    @Getter
    private final RunRightFastVerticleId runRightFastVerticleId
            = RunRightFastVerticleId.builder()
            .group(RunRightFastVerticleId.RUNRIGHTFAST_GROUP)
            .name(getClass().getSimpleName())
            .version("1.0.0")
            .build();

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
    }

    @Override
    protected Set<RunRightFastHealthCheck> getHealthChecks() {
        return ImmutableSet.of(
                healthCheck1()
        );
    }

    private RunRightFastHealthCheck healthCheck1() {
        return RunRightFastHealthCheck.builder()
                .config(healthCheckConfigBuilder()
                        .name("healthcheck-1")
                        .severity(FATAL)
                        .build()
                )
                .healthCheck(new HealthCheck() {

                    @Override
                    protected HealthCheck.Result check() throws Exception {
                        return HealthCheck.Result.healthy();
                    }
                })
                .build();
    }

}
