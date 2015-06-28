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

import io.vertx.core.VertxOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import java.util.function.Supplier;

/**
 *
 * @author alfio
 */
@FunctionalInterface
public interface VertxOptionsFactory extends Supplier<VertxOptions> {

    /**
     * <ul>
     * <li>Enables metrics using DropwizardMetrics.
     * <li>Exposes metrics via JMX
     * <li>Sets the metric registry name to {@link VertxConstants#METRIC_REGISTRY_NAME }
     * </ul>
     *
     * @param options VertxOptions
     * @return VertxOptions
     */
    static VertxOptions configureMetricsOptions(final VertxOptions options) {
        return options.setMetricsOptions(new DropwizardMetricsOptions()
                .setEnabled(true)
                .setJmxEnabled(true)
                .setRegistryName(VertxConstants.METRIC_REGISTRY_NAME)
        );
    }

    static VertxOptions configureMetricsOptions(final VertxOptions options, boolean jmxEnabled) {
        return options.setMetricsOptions(new DropwizardMetricsOptions()
                .setEnabled(true)
                .setJmxEnabled(jmxEnabled)
                .setRegistryName(VertxConstants.METRIC_REGISTRY_NAME)
        );
    }
}
