/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core.application.services.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import java.beans.ConstructorProperties;
import javax.management.MXBean;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 *
 * @author alfio
 */
@MXBean
public interface HealthChecksMXBean {

    public static final class HealthCheckResult {

        @Getter
        private final String registryName;
        @Getter
        private final String name;
        @Getter
        private final boolean healthy;
        @Getter
        private final String message;
        @Getter
        private final String exception;

        @ConstructorProperties({
            "registryName", "name", "healthy", "message", "exception"
        })
        public HealthCheckResult(final String registryName, final String name, final boolean healthy, final String message, final String exception) {
            this.registryName = registryName;
            this.name = name;
            this.healthy = healthy;
            this.message = message;
            this.exception = exception;
        }

        public HealthCheckResult(final RunRightFastHealthCheckResult result) {
            this.registryName = result.getConfig().getRegistryName();
            this.name = result.getConfig().getName();
            this.healthy = result.getResult().isHealthy();
            this.message = result.getResult().getMessage();
            if (result.getResult().getError() != null) {
                this.exception = ExceptionUtils.getStackTrace(result.getResult().getError());
            } else {
                this.exception = null;
            }
        }

        public HealthCheckResult(final String registryName, final String name, final HealthCheck.Result result) {
            this.registryName = registryName;
            this.name = name;
            this.healthy = result.isHealthy();
            this.message = result.getMessage();
            if (result.getError() != null) {
                this.exception = ExceptionUtils.getStackTrace(result.getError());
            } else {
                this.exception = null;
            }
        }

    }

    @Data
    public static final class HealthCheckId {

        @NonNull
        private final String registry;

        @NonNull
        private final String name;

        @ConstructorProperties({
            "registryName", "name"
        })
        public HealthCheckId(final String registryName, final String name) {
            this.registry = registryName;
            this.name = name;
        }
    }

    String[] getHealthCheckRegistryNames();

    String[] getHealthCheckNames(final String registry);

    HealthCheckId[] getHealthCheckIds();

    HealthCheckResult check(final String registry, final String name);

    HealthCheckResult[] checkAll();

}
