/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core.application.services.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@Builder
@EqualsAndHashCode
public final class RunRightFastHealthCheck {

    @Getter
    @NonNull
    private final HealthCheckConfig config;

    @Getter
    @NonNull
    private final HealthCheck healthCheck;

}
