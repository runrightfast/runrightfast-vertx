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
package co.runrightfast.vertx.core.app;

import static co.runrightfast.vertx.core.VertxConstants.CONFIG_ROOT;
import static co.runrightfast.vertx.core.VertxConstants.METRIC_REGISTRY_NAME;
import co.runrightfast.vertx.core.VertxFactory;
import static co.runrightfast.vertx.core.utils.ConfigUtils.configPath;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import io.vertx.core.Vertx;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;

/**
 *
 * @author alfio
 */
public final class VertxApplication {

    private static final Logger LOG = Logger.getLogger(VertxApplication.class.getName());

    public static MetricRegistry metricRegistry() {
        return SharedMetricRegistries.getOrCreate(METRIC_REGISTRY_NAME);
    }

    public static void main(final String[] args) {
        Vertx vertx = null;
        try {
            final Config config = ConfigFactory.load();
            final VertxAppConfig vertxAppConfig = ConfigBeanFactory.create(config.getConfig(configPath(CONFIG_ROOT, "VertxAppConfig")), VertxAppConfig.class);
            final VertxFactory vertxFactory = vertxFactory(vertxAppConfig);

            vertx = vertxFactory.get();

        } catch (final Throwable t) {
            LOG.logp(SEVERE, VertxApplication.class.getName(), "main", "Unexpected exception", t);
        } finally {
            if (vertx != null) {
                final CountDownLatch latch = new CountDownLatch(1);
                vertx.close(result -> {
                    try {
                        if (result.succeeded()) {
                            LOG.logp(INFO, VertxApplication.class.getName(), "main", "Vertx has been closed successfully");
                        } else {
                            LOG.logp(SEVERE, VertxApplication.class.getName(), "main", "Vertx.close() failed", result.cause());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
                try {
                    while (!latch.await(5, TimeUnit.SECONDS)) {
                        LOG.logp(INFO, VertxApplication.class.getName(), "main", "Waiting for Vertx.close() to complete");
                    }
                } catch (final InterruptedException ex) {
                    LOG.logp(WARNING, VertxApplication.class.getName(), "main", "Interrupted whilr Waiting for Vertx.close() to complete", ex);
                }
            }
        }
    }

    private static VertxFactory vertxFactory(final VertxAppConfig vertxAppConfig) throws Exception {
        vertxAppConfig.validate();
        final ClassLoader cl = VertxApplication.class.getClassLoader();
        return (VertxFactory) cl.loadClass(vertxAppConfig.getVertxFactoryClassName()).newInstance();
    }

}
