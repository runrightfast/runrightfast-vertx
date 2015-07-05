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

import co.runrightfast.vertx.core.VertxService;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vertx.core.Vertx;
import java.util.logging.Level;
import lombok.extern.java.Log;
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
        ConfigFactory.invalidateCaches();
        config = ConfigFactory.load(String.format("%s.conf", VertxServiceImplTest.class.getSimpleName()));
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
        log.log(Level.INFO, "vertx.isClustered() = {0}", vertx.isClustered());
        log.log(Level.INFO, "vertx.isMetricsEnabled() = {0}", vertx.isMetricsEnabled());
    }

}
