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
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.util.concurrent.AbstractIdleService;
import com.typesafe.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
        LOG.config(() -> ConfigUtils.renderConfig(config));
        this.vertxOptions = createVertxOptions();
        if (this.vertxOptions.isClustered()) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> exception = new AtomicReference<>();
            Vertx.clusteredVertx(vertxOptions, result -> {
                try {
                    if (result.succeeded()) {
                        this.vertx = result.result();
                    } else {
                        exception.set(result.cause());
                    }
                } finally {
                    latch.countDown();
                }
            });
            while (latch.await(10, TimeUnit.SECONDS)) {
                LOG.info("Waiting for Vertx to start");
            }
            if (exception.get() != null) {
                throw new RuntimeException("Failed to start a clustered Vertx instance", exception.get());
            }
        } else {
            this.vertx = Vertx.vertx(vertxOptions);
        }
    }

    @Override
    protected void shutDown() throws InterruptedException {
        if (vertx != null) {
            final CountDownLatch latch = new CountDownLatch(1);
            vertx.close(result -> latch.countDown());
            while (latch.await(10, TimeUnit.SECONDS)) {
                LOG.info("Waiting for Vertx to shutdown");
            }
            vertx = null;
            vertxOptions = null;
        }
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
                .setJmxDomain(ConfigUtils.getString(config, "VertxOptions", "metricsOptions", "jmxDomain")
                        .orElseGet(() -> ConfigUtils.getString(config, "VertxOptions", "metricsOptions", "jmxDomain").orElse("co.runrightfast"))
                )
        );

    }

    private void configureClusterManager() {
        ConfigUtils.getConfig(config, "VertxOptions", "clusterManager", "hazelcast").map(c -> {
            final com.hazelcast.config.Config hazelcastConfig = hazelcastConfigFactory(VERTX_HAZELCAST_INSTANCE_ID).apply(c);
            return new HazelcastClusterManager(hazelcastConfig);
        }).ifPresent(vertxOptions::setClusterManager);
    }

}
