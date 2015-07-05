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
package co.runrightfast.vertx.core.hazelcast.impl;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigUtil;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class TypesafeHazelcastConfigTest {

    private static com.typesafe.config.Config config;

    @BeforeClass
    public static void setUpClass() {
        ConfigFactory.invalidateCaches();
        config = ConfigFactory.load(String.format("%s.conf", TypesafeHazelcastConfigTest.class.getSimpleName()));
    }

    @After
    public void afterTest() {
        Hazelcast.shutdownAll();
    }

    /**
     * Test of getHazelcastConfig method, of class TypesafeHazelcastConfig.
     */
    @Test
    public void testGetHazelcastConfig() {
        System.out.println("getConfig");
        final TypesafeHazelcastConfig typesafeHazelcastConfig = new TypesafeHazelcastConfig("testGetConfig", config.getConfig(ConfigUtil.joinPath("Hazelcast", "application-1")));
        final Config hazelcastConfig = typesafeHazelcastConfig.getHazelcastConfig();
        assertThat(hazelcastConfig, is(notNullValue()));
        assertThat(hazelcastConfig.getInstanceName(), is("testGetConfig"));
    }

    /**
     * Test of getHazelcastConfig method, of class TypesafeHazelcastConfig.
     */
    @Test(expected = NullPointerException.class)
    public void testGetHazelcastConfig_null_arg() {
        new TypesafeHazelcastConfig("testGetConfig", null);
    }

}
