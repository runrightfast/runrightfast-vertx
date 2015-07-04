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
package co.runrightfast.vertx.core.utils;

import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author alfio
 */
public class ConfigUtilsTest {

    private static final Logger log = Logger.getLogger(ConfigUtilsTest.class.getSimpleName());

    private static final Config config;

    static {
        ConfigFactory.invalidateCaches();
        try {
            config = ConfigFactory.load(String.format("%s.conf", ConfigUtilsTest.class.getSimpleName()));
        } catch (final Exception e) {
            log.log(Level.SEVERE, "failed to load config", e);
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testLoadConfig_string() {
        System.setProperty("config.resource", String.format("%s.conf", ConfigUtilsTest.class.getSimpleName()));
        final Config config = ConfigUtils.loadConfig(true);

        String path = CONFIG_NAMESPACE + ".app.name";
        boolean expResult = true;
        boolean result = ConfigUtils.hasPath(config, path);
        assertEquals(expResult, result);

        assertThat(ConfigUtils.hasPath(config, UUID.randomUUID().toString()), is(false));

        log.info(ConfigUtils.renderConfig(config));
        log.info(ConfigUtils.toJsonObject(config).toString());
    }

    @Test
    public void test_renderConfig() {
        final String configHconf = ConfigUtils.renderConfig(config);
        log.info(configHconf);
        ConfigFactory.parseReader(new StringReader(configHconf));
    }

    @Test
    public void toJsonObject() {
        final JsonObject configJsonObject = ConfigUtils.toJsonObject(config);
        log.info(configJsonObject.toString());
        assertThat(configJsonObject.getString("a"), is("A"));
    }

    @Test
    public void testLoadConfig() {
        System.setProperty("config.resource", String.format("%s.conf", ConfigUtilsTest.class.getSimpleName()));
        final Config config = ConfigUtils.loadConfig();

        String path = CONFIG_NAMESPACE + ".app.name";
        boolean expResult = true;
        boolean result = ConfigUtils.hasPath(config, path);
        assertEquals(expResult, result);

        assertThat(ConfigUtils.hasPath(config, UUID.randomUUID().toString()), is(false));
    }

    /**
     * Test of hasPath method, of class ConfigUtils.
     */
    @Test
    public void testHasPath() {
        System.out.println("hasPath");
        String path = CONFIG_NAMESPACE + ".app.name";
        boolean expResult = true;
        boolean result = ConfigUtils.hasPath(config, path);
        assertEquals(expResult, result);

        assertThat(ConfigUtils.hasPath(config, UUID.randomUUID().toString()), is(false));
    }

    @Test
    public void testConfigPath() {
        assertThat(ConfigUtils.configPath("a", "b", "c"), is("a.b.c"));
    }

    /**
     * Test of getConfig method, of class ConfigUtils.
     */
    @Test
    public void testGetConfig_Config_String() {
        System.out.println("getConfig");
        String path = CONFIG_NAMESPACE;
        Optional<Config> result = ConfigUtils.getConfig(config, path);
        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getString("app.name"), is(config.getString(CONFIG_NAMESPACE + ".app.name")));

        assertThat(ConfigUtils.getConfig(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getConfigList method, of class ConfigUtils.
     */
    @Test
    public void testGetConfigList_Config_String() {
        System.out.println("getConfigList");
        String path = "config-list";
        Optional<List<? extends Config>> result = ConfigUtils.getConfigList(config, path);
        assertThat(result.isPresent(), is(true));
        assertThat(result.get().size(), is(3));

        assertThat(ConfigUtils.getConfigList(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getBoolean method, of class ConfigUtils.
     */
    @Test
    public void testGetBoolean_Config_String() {
        System.out.println("getBoolean");
        String path = "bool";
        Optional<Boolean> result = ConfigUtils.getBoolean(config, path);
        assertThat(result.get(), is(true));

        assertThat(ConfigUtils.getBoolean(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getDouble method, of class ConfigUtils.
     */
    @Test
    public void testGetDouble_Config_String() {
        System.out.println("getDouble");
        String path = "double";
        Optional<Double> result = ConfigUtils.getDouble(config, path);
        assertThat(result.get(), is(2.5d));

        assertThat(ConfigUtils.getDouble(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getDuration method, of class ConfigUtils.
     */
    @Test
    public void testGetDuration_3args() {
        System.out.println("getDuration");

        String path = "timeout";
        TimeUnit unit = TimeUnit.SECONDS;
        Optional<Long> result = ConfigUtils.getDuration(config, unit, path);
        assertThat(result.get(), is(5L));

        assertThat(ConfigUtils.getDuration(config, unit, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getInt method, of class ConfigUtils.
     */
    @Test
    public void testGetInt_Config_String() {
        System.out.println("getInt");

        String path = "int";
        Optional<Integer> result = ConfigUtils.getInt(config, path);
        assertThat(result.get(), is(3));
        assertThat(ConfigUtils.getInt(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getLong method, of class ConfigUtils.
     */
    @Test
    public void testGetLong_Config_String() {
        System.out.println("getLong");

        String path = "int";
        Optional<Long> result = ConfigUtils.getLong(config, path);
        assertThat(result.get(), is(3L));
        assertThat(ConfigUtils.getLong(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getNumber method, of class ConfigUtils.
     */
    @Test
    public void testGetNumber_Config_String() {
        System.out.println("getNumber");

        String path = "int";
        Optional<Number> result = ConfigUtils.getNumber(config, path);
        assertThat(result.isPresent(), is(true));
        assertThat(ConfigUtils.getNumber(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getObject method, of class ConfigUtils.
     */
    @Test
    public void testGetObject_Config_String() {
        System.out.println("getObject");

        String path = CONFIG_NAMESPACE;
        Optional<ConfigObject> result = ConfigUtils.getObject(config, path);
        assertThat(result.isPresent(), is(true));
        assertThat(ConfigUtils.getObject(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getObjectList method, of class ConfigUtils.
     */
    @Test
    public void testGetObjectList_Config_String() {
        System.out.println("getObjectList");

        String path = "config-list";
        Optional<List<? extends ConfigObject>> result = ConfigUtils.getObjectList(config, path);
        assertThat(result.get().size(), is(3));
        assertThat(ConfigUtils.getObjectList(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getString method, of class ConfigUtils.
     */
    @Test
    public void testGetString_Config_String() {
        System.out.println("getString");

        String path = "d";
        Optional<String> result = ConfigUtils.getString(config, path);
        assertThat(result.get(), is("D"));
        assertThat(ConfigUtils.getString(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getStringList method, of class ConfigUtils.
     */
    @Test
    public void testGetStringList_Config_String() {
        System.out.println("getStringList");

        String path = "string-list";
        Optional<List<String>> result = ConfigUtils.getStringList(config, path);
        assertThat(result.get().size(), is(3));
        assertThat(ConfigUtils.getStringList(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of getValue method, of class ConfigUtils.
     */
    @Test
    public void testGetValue_Config_String() {
        System.out.println("getValue");

        String path = CONFIG_NAMESPACE;
        Optional<ConfigValue> result = ConfigUtils.getValue(config, path);
        assertThat(result.isPresent(), is(true));
        assertThat(ConfigUtils.getValue(config, UUID.randomUUID().toString()).isPresent(), is(false));
    }

    /**
     * Test of renderConfig method, of class ConfigUtils.
     */
    @Test
    public void testRenderConfig_4args() {
        System.out.println("renderConfig");

        boolean comments = false;
        boolean orginComments = false;
        boolean json = false;
        ConfigUtils.renderConfig(config, comments, orginComments, json);
    }

    /**
     * Test of toJsonObject method, of class ConfigUtils.
     */
    @Test
    public void testToJsonObject() {
        System.out.println("toJsonObject");

        JsonObject result = ConfigUtils.toJsonObject(config);
        log.info(result.toString());
    }

    /**
     * Test of renderConfigAsJson method, of class ConfigUtils.
     */
    @Test
    public void testRenderConfigAsJson_Config_boolean() {
        System.out.println("renderConfigAsJson");

        boolean formatted = false;
        log.info(ConfigUtils.renderConfigAsJson(config, formatted));
    }

    @Test
    public void testToProperties() throws IOException {

        config.entrySet().stream().forEach(entry -> {
            log.log(Level.INFO, "{0} -> {1}", new Object[]{entry.getKey(), entry.getValue().unwrapped()});
        });

        final StringWriter sw = new StringWriter();
        ConfigUtils.toProperties(config).store(sw, CONFIG_NAMESPACE);
        log.info(sw.toString());
    }

}
