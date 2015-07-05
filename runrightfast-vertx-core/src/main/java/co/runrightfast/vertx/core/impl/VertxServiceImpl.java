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
package co.runrightfast.vertx.core.impl;

import co.runrightfast.vertx.core.VertxConstants;
import static co.runrightfast.vertx.core.VertxConstants.VERTX_HAZELCAST_INSTANCE_ID;
import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.hazelcast.HazelcastConfigFactory.hazelcastConfigFactory;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import co.runrightfast.vertx.core.utils.JsonUtils;
import com.google.common.util.concurrent.AbstractIdleService;
import com.typesafe.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.Match;
import io.vertx.ext.dropwizard.MatchType;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static java.util.logging.Level.INFO;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public final class VertxServiceImpl extends AbstractIdleService implements VertxService {

    private final Config config;

    private Vertx vertx;

    private VertxOptions vertxOptions;

    public VertxServiceImpl(@NonNull final Config config) {
        this.config = config;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    /**
     *
     * @return a copy of the VertxOptions that was used to create the Vertx instance
     */
    @Override
    public VertxOptions getVertxOptions() {
        final VertxOptions copy = new VertxOptions(vertxOptions);
        final MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
        if (metricsOptions != null) {
            if (metricsOptions instanceof DropwizardMetricsOptions) {
                copy.setMetricsOptions(new DropwizardMetricsOptions((DropwizardMetricsOptions) metricsOptions));
            } else {
                copy.setMetricsOptions(new DropwizardMetricsOptions(metricsOptions));
            }
        }
        return copy;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.config(() -> ConfigUtils.renderConfig(config));
        this.vertxOptions = createVertxOptions();
        if (this.vertxOptions.isClustered()) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> exception = new AtomicReference<>();
            Vertx.clusteredVertx(vertxOptions, result -> {
                try {
                    if (result.succeeded()) {
                        this.vertx = result.result();
                        LOG.info("Vertx clustered instance has been created");
                    } else {
                        exception.set(result.cause());
                    }
                } finally {
                    latch.countDown();
                }
            });
            while (!latch.await(10, TimeUnit.SECONDS)) {
                LOG.info("Waiting for Vertx to start");
            }
            if (exception.get() != null) {
                throw new RuntimeException("Failed to start a clustered Vertx instance", exception.get());
            }
        } else {
            this.vertx = Vertx.vertx(vertxOptions);
            LOG.info("Vertx instance has been created");
        }
        LOG.logp(INFO, getClass().getName(), "startUp", "success");
    }

    @Override
    protected void shutDown() throws InterruptedException {
        if (vertx != null) {
            final CountDownLatch latch = new CountDownLatch(1);
            vertx.close(result -> latch.countDown());
            while (!latch.await(10, TimeUnit.SECONDS)) {
                LOG.info("Waiting for Vertx to shutdown");
            }
            LOG.info("Vertx shutdown is complete.");
            vertx = null;
            vertxOptions = null;
        }
    }

    private VertxOptions createVertxOptions() {
        final JsonObject vertxJsonObject = JsonUtils.toVertxJsonObject(ConfigUtils.toJsonObject(config.getConfig("VertxOptions")));
        vertxOptions = new VertxOptions(vertxJsonObject);

        if (vertxOptions.getMetricsOptions() != null && vertxOptions.getMetricsOptions().isEnabled()) {
            configureMetricsOptions();
        }

        if (vertxOptions.isClustered()) {
            configureClusterManager();
        }

        return vertxOptions;
    }

    /**
     * config structure:
     *
     * <code>
     * VertxOptions {
     *    metricsOptions {
     *       enabled = true
     *       jmxEnabled = true
     *       jmxDomain = co.runrightfast
     *       eventbusHandlers = [
     *          { address="/eventbus-address-1", matchType="EQUALS"}
     *          { address="/eventbus-address-2/.*", matchType="REGEX"}
     *       ]
     *       monitoredHttpServerURIs = [
     *          { uri="/verticle/log-service", matchType="EQUALS"}
     *          { uri="/verticle/log-service/.*", matchType="REGEX"}
     *       ]
     *       monitoredHttpClientURIs = [
     *          { uri="/verticle/log-service", matchType="EQUALS"}
     *          { uri="/verticle/log-service/.*", matchType="REGEX"}
     *       ]
     *    }
     * }
     * </code>
     *
     */
    private void configureMetricsOptions() {
        final DropwizardMetricsOptions metricsOptions = new DropwizardMetricsOptions()
                .setEnabled(true)
                .setJmxEnabled(ConfigUtils.getBoolean(config, "VertxOptions", "metricsOptions", "jmxEnabled").orElse(Boolean.FALSE))
                .setRegistryName(VertxConstants.VERTX_METRIC_REGISTRY_NAME)
                .setJmxDomain(ConfigUtils.getString(config, "VertxOptions", "metricsOptions", "jmxDomain").orElse("co.runrightfast"));

        ConfigUtils.getConfigList(config, "VertxOptions", "metricsOptions", "eventbusHandlers").orElse(Collections.emptyList()).stream()
                .map(eventbusHandlerMatch -> {
                    final Match match = new Match();
                    match.setValue(eventbusHandlerMatch.getString("address"));
                    match.setType(MatchType.valueOf(eventbusHandlerMatch.getString("matchType")));
                    return match;
                }).forEach(metricsOptions::addMonitoredEventBusHandler);

        ConfigUtils.getConfigList(config, "VertxOptions", "metricsOptions", "monitoredHttpServerURIs").orElse(Collections.emptyList()).stream()
                .map(eventbusHandlerMatch -> {
                    final Match match = new Match();
                    match.setValue(eventbusHandlerMatch.getString("uri"));
                    match.setType(MatchType.valueOf(eventbusHandlerMatch.getString("matchType")));
                    return match;
                }).forEach(metricsOptions::addMonitoredHttpServerUri);

        ConfigUtils.getConfigList(config, "VertxOptions", "metricsOptions", "monitoredHttpClientURIs").orElse(Collections.emptyList()).stream()
                .map(eventbusHandlerMatch -> {
                    final Match match = new Match();
                    match.setValue(eventbusHandlerMatch.getString("uri"));
                    match.setType(MatchType.valueOf(eventbusHandlerMatch.getString("matchType")));
                    return match;
                }).forEach(metricsOptions::addMonitoredHttpClientUri);

        this.vertxOptions.setMetricsOptions(metricsOptions);
    }

    private void configureClusterManager() {
        ConfigUtils.getConfig(config, "VertxOptions", "clusterManager", "hazelcast").map(c -> {
            final com.hazelcast.config.Config hazelcastConfig = hazelcastConfigFactory(VERTX_HAZELCAST_INSTANCE_ID).apply(c);
            return new HazelcastClusterManager(hazelcastConfig);
        }).ifPresent(vertxOptions::setClusterManager);
    }

}
