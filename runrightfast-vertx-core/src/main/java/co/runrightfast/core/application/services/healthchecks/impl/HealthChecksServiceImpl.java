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

import co.runrightfast.core.application.event.AppEvent;
import co.runrightfast.core.application.event.AppEvent.AppEventLevel;
import static co.runrightfast.core.application.event.AppEvent.AppEventLevel.ALERT;
import static co.runrightfast.core.application.event.AppEvent.AppEventLevel.INFO;
import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.ApplicationEvents;
import co.runrightfast.core.application.services.healthchecks.HealthChecksMXBean;
import co.runrightfast.core.application.services.healthchecks.HealthChecksService;
import static co.runrightfast.vertx.core.utils.JmxUtils.registerApplicationMBean;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Singleton
public final class HealthChecksServiceImpl implements HealthChecksService {

    private final AppEventLogger appEventLogger;

    @Inject
    public HealthChecksServiceImpl(@NonNull final AppEventLogger appEventLogger, final String jmxDomain) {
        this.appEventLogger = appEventLogger;
        registerApplicationMBean(jmxDomain, new HealthChecksMXBeanImpl(this), HealthChecksMXBean.class);
    }

    @Override
    public Set<String> getRegistryNames() {
        return SharedHealthCheckRegistries.names();
    }

    @Override
    public Set<String> getHealthCheckNames(final String registryName) {
        checkArgument(isNotBlank(registryName));
        if (!SharedHealthCheckRegistries.names().contains(registryName)) {
            return Collections.emptySet();
        }
        return SharedHealthCheckRegistries.getOrCreate(registryName).getNames();
    }

    @Override
    public HealthCheck.Result runHealthCheck(final String registryName, final String healthCheckName) {
        checkArgument(isNotBlank(registryName));
        checkArgument(isNotBlank(healthCheckName));
        checkArgument(SharedHealthCheckRegistries.names().contains(registryName));

        final HealthCheckRegistry registry = SharedHealthCheckRegistries.getOrCreate(registryName);
        checkArgument(registry.getNames().contains(healthCheckName));
        final HealthCheck.Result result = registry.runHealthCheck(healthCheckName);
        logAppEvent(result, registryName, healthCheckName);
        return result;
    }

    @Override
    public Collection<HealthCheck.Result> runHealthChecks() {
        final ImmutableList.Builder<HealthCheck.Result> results = ImmutableList.builder();
        getRegistryNames().stream()
                .forEach(registryName -> {
                    getHealthCheckNames(registryName).forEach(healthCheckName -> results.add(runHealthCheck(registryName, healthCheckName)));
                });
        return results.build();
    }

    @Override
    public CompletableFuture<Collection<HealthCheck.Result>> runHealthChecksAsync() {
        return CompletableFuture.supplyAsync(this::runHealthChecks);
    }

    private void logAppEvent(final HealthCheck.Result result, final String registryName, final String healthCheckName) {
        final AppEventLevel eventLevel = result.isHealthy() ? INFO : ALERT;
        appEventLogger.accept(AppEvent.builder(String.format("healthcheck/%s/%s", registryName, healthCheckName), eventLevel)
                .setException(result.getError())
                .setMessage(result.getMessage())
                .setData(new ApplicationEvents.HealthCheckResult(result))
                .build()
        );
    }

}
