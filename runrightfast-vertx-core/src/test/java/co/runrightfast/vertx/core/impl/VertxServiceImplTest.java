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

import static co.runrightfast.vertx.core.VertxConstants.VERTX_METRIC_REGISTRY_NAME;
import co.runrightfast.vertx.core.VertxService;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import co.runrightfast.vertx.core.utils.JvmProcess;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class VertxServiceImplTest {

    private VertxService service;

    private static Config config;

    @BeforeClass
    public static void setUpClass() {
        config = ConfigUtils.loadConfig(String.format("%s.conf", VertxServiceImplTest.class.getSimpleName()), true);
    }

    @Before
    public void setUp() {
        ConfigFactory.invalidateCaches();
    }

    @After
    public void tearDown() {
        ServiceUtils.stop(service);
    }

    /**
     * Test of getVertx method, of class VertxServiceImpl.
     */
    @Test
    public void test_vertx_default_options() {
        log.info("getVertx");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-default")));
        ServiceUtils.start(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(false));
    }

    @Test
    public void test_vertx_metrics_options() {
        log.info("getVertx");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-with-metrics")));
        ServiceUtils.start(service);
        final Vertx vertx = service.getVertx();
        log.log(Level.INFO, "vertx.isClustered() = {0}", vertx.isClustered());
        log.log(Level.INFO, "vertx.isMetricsEnabled() = {0}", vertx.isMetricsEnabled());
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(true));

        final VertxOptions vertxOptions = service.getVertxOptions();
        final MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
        log.log(INFO, "metricsOptions class : {0}", metricsOptions.getClass().getName());
        final DropwizardMetricsOptions dropwizardMetricsOptions = (DropwizardMetricsOptions) metricsOptions;
        assertThat(dropwizardMetricsOptions.isJmxEnabled(), is(true));
        assertThat(dropwizardMetricsOptions.getJmxDomain(), is("co.runrightfast"));
        assertThat(dropwizardMetricsOptions.getRegistryName(), is(VERTX_METRIC_REGISTRY_NAME));
        assertThat(dropwizardMetricsOptions.getMonitoredEventBusHandlers().size(), is(2));
        assertThat(dropwizardMetricsOptions.getMonitoredHttpServerUris().size(), is(3));
        assertThat(dropwizardMetricsOptions.getMonitoredHttpClientUris().size(), is(4));

    }

    /**
     * Test of getVertx method, of class VertxServiceImpl.
     */
    @Test
    public void test_vertx_custom_options() {
        log.info("getVertx");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-custom-non-clustered")));
        ServiceUtils.start(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(false));

        final VertxOptions vertxOptions = service.getVertxOptions();
        assertThat(vertxOptions.getBlockedThreadCheckInterval(), is(3000L));
        assertThat(vertxOptions.getClusterHost(), is(JvmProcess.getHost()));
        assertThat(vertxOptions.getHAGroup(), is("elasticsearch"));
        assertThat(vertxOptions.getClusterPingInterval(), is(1000L));
        assertThat(vertxOptions.getClusterPort(), is(1234));
        assertThat(vertxOptions.getClusterManager(), is(nullValue()));
        assertThat(vertxOptions.getEventLoopPoolSize(), is(20));
        assertThat(vertxOptions.getInternalBlockingPoolSize(), is(2000));
        assertThat(vertxOptions.getMaxEventLoopExecuteTime(), is(4000000000L));
        assertThat(vertxOptions.getMaxWorkerExecuteTime(), is(50000000000L));
        assertThat(vertxOptions.getQuorumSize(), is(3));
        assertThat(vertxOptions.getWarningExceptionTime(), is(3500000000L));
        assertThat(vertxOptions.getWorkerPoolSize(), is(30));

    }

}
