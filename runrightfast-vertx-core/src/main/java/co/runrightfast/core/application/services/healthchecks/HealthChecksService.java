/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core.application.services.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author alfio
 */
public interface HealthChecksService {

    Set<String> getRegistryNames();

    Set<String> getHealthCheckNames(final String registry);

    HealthCheck.Result runHealthCheck(final String registry, final String name);

    Collection<HealthCheck.Result> runHealthChecks();

    CompletableFuture<Collection<HealthCheck.Result>> runHealthChecksAsync();

}
