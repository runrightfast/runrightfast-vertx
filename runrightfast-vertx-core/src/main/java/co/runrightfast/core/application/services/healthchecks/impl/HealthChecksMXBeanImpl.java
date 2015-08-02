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
package co.runrightfast.core.application.services.healthchecks.impl;

import co.runrightfast.core.application.services.healthchecks.HealthChecksMXBean;
import co.runrightfast.core.application.services.healthchecks.HealthChecksService;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;

/**
 *
 * @author alfio
 */
public final class HealthChecksMXBeanImpl implements HealthChecksMXBean {

    private final HealthChecksService healthCheckService;

    HealthChecksMXBeanImpl(final HealthChecksService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @Override
    public String[] getHealthCheckRegistryNames() {
        return healthCheckService.getRegistryNames().stream().toArray(String[]::new);
    }

    @Override
    public String[] getHealthCheckNames(final String registry) {
        return healthCheckService.getHealthCheckNames(registry).stream().toArray(String[]::new);
    }

    @Override
    public HealthCheckResult check(final String registry, final String name) {
        final HealthCheck.Result result = healthCheckService.runHealthCheck(registry, name);
        return new HealthCheckResult(registry, name, result);
    }

    @Override
    public HealthCheckResult[] checkAll() {
        final ImmutableList.Builder<HealthCheckResult> results = ImmutableList.builder();
        healthCheckService.getRegistryNames().stream().forEach(registry -> {
            healthCheckService.getHealthCheckNames(registry).stream().forEach(name -> {
                final HealthCheck.Result result = healthCheckService.runHealthCheck(registry, name);
                results.add(new HealthCheckResult(registry, name, result));
            });
        });

        return results.build().stream().toArray(HealthCheckResult[]::new);
    }

    @Override
    public HealthCheckId[] getHealthCheckIds() {
        final ImmutableList.Builder<HealthCheckId> results = ImmutableList.builder();
        healthCheckService.getRegistryNames().stream().forEach(registry -> {
            healthCheckService.getHealthCheckNames(registry).stream().forEach(name -> {
                results.add(new HealthCheckId(registry, name));
            });
        });

        return results.build().stream().toArray(HealthCheckId[]::new);
    }

}
