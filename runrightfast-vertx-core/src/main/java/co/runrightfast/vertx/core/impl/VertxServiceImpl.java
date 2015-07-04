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
import co.runrightfast.vertx.core.VertxService;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import co.runrightfast.vertx.core.utils.JsonUtils;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.util.concurrent.AbstractIdleService;
import com.typesafe.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;

/**
 *
 * @author alfio
 */
public final class VertxServiceImpl extends AbstractIdleService implements VertxService {

    private final Config config;

    private Vertx vertx;

    private VertxOptions vertxOptions;

    public VertxServiceImpl(final Config config) {
        checkNotNull(config);
        this.config = config;
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    VertxOptions getVertxOptions() {
        return vertxOptions;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    protected void startUp() throws Exception {
        this.vertxOptions = createVertxOptions();
    }

    @Override
    protected void shutDown() throws Exception {

    }

    private VertxOptions createVertxOptions() {
        final JsonObject vertxJsonObject = JsonUtils.toVertxJsonObject(ConfigUtils.toJsonObject(config.getConfig("VertxOptions")));
        vertxOptions = new VertxOptions(vertxJsonObject);

        if (vertxOptions.getMetricsOptions().isEnabled()) {
            configureMetricsOptions();
        }

        if (vertxOptions.isClustered()) {
            configureClusterManager();
        }

        return vertxOptions;
    }

    private void configureMetricsOptions() {
        vertxOptions.setMetricsOptions(new DropwizardMetricsOptions()
                .setEnabled(true)
                .setJmxEnabled(ConfigUtils.getBoolean(config, "VertxOptions", "metricsOptions", "jmxEnabled").orElse(Boolean.FALSE))
                .setRegistryName(VertxConstants.VERTX_METRIC_REGISTRY_NAME)
        );
    }

    private void configureClusterManager() {
        ConfigUtils.getConfig(config, "VertxOptions", "clusterManager", "hazelcast").map(c -> {
            final com.hazelcast.config.Config hazelcastConfig = new com.hazelcast.config.Config();

            return hazelcastConfig;
        });

    }

}
