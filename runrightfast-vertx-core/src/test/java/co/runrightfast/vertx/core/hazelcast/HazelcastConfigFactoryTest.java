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
package co.runrightfast.vertx.core.hazelcast;

import co.runrightfast.core.hazelcast.HazelcastConfigFactory;
import co.runrightfast.vertx.core.hazelcast.serializers.JsonObjectSerializer;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.Member;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigUtil;
import java.util.Map;
import java.util.Set;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
public class HazelcastConfigFactoryTest {

    private static final Logger LOG = Logger.getLogger(HazelcastConfigFactoryTest.class.getName());

    private static Config config;

    @BeforeClass
    public static void setUpClass() {
        ConfigFactory.invalidateCaches();
        config = ConfigFactory.load(String.format("%s.conf", HazelcastConfigFactoryTest.class.getSimpleName()));
    }

    @After
    public void afterTest() {
        Hazelcast.shutdownAll();
    }

    /**
     * Test of hazelcastConfigFactory method, of class HazelcastConfigFactory.
     */
    @Test
    public void testHazelcastConfigFactory_withSerializers() {
        LOG.info("hazelcastConfigFactory");
        String hazelcastInstanceName = "testHazelcastConfigFactory_withSerializers";
        final Set<SerializerConfig> serializerConfigs = ImmutableSet.of(
                new SerializerConfig().setImplementation(new JsonObjectSerializer()).setTypeClass(JsonObject.class)
        );
        final HazelcastConfigFactory factory1 = HazelcastConfigFactory.hazelcastConfigFactory(hazelcastInstanceName + 1, serializerConfigs);
        final com.hazelcast.config.Config hazelcastConfig1 = factory1.apply(config.getConfig(ConfigUtil.joinPath("Hazelcast", "application-1")));
        final HazelcastInstance hazelcast1 = Hazelcast.newHazelcastInstance(hazelcastConfig1);

        final HazelcastConfigFactory factory2 = HazelcastConfigFactory.hazelcastConfigFactory(hazelcastInstanceName + 2, serializerConfigs);
        final com.hazelcast.config.Config hazelcastConfig2 = factory2.apply(config.getConfig(ConfigUtil.joinPath("Hazelcast", "application-2")));
        final HazelcastInstance hazelcast2 = Hazelcast.newHazelcastInstance(hazelcastConfig2);

        test(hazelcast1, hazelcast2);

        final JsonObject json = Json.createObjectBuilder().add("a", 1).build();
        final String mapName = "testHazelcastConfigFactory_withSerializers_map";
        final Map<String, Object> map1 = hazelcast1.getMap(mapName);
        map1.put("json", json);

        final Map<String, Object> map2 = hazelcast2.getMap(mapName);
        final JsonObject json2 = (JsonObject) map2.get("json");
        assertThat(json2.getInt("a"), is(1));
    }

    private void test(final HazelcastInstance hazelcast1, final HazelcastInstance hazelcast2) {
        final IAtomicLong counterA = hazelcast1.getAtomicLong("counter-a");
        final long countA = counterA.incrementAndGet();
        LOG.log(INFO, "expecting {0} = {1}", new Object[]{counterA.get(), hazelcast1.getAtomicLong("counter-a").get()});
        assertThat(countA, is(hazelcast1.getAtomicLong("counter-a").get()));

        assertThat(hazelcast2.getAtomicLong("counter-a").get(), is(hazelcast1.getAtomicLong("counter-a").get()));

        final Member member2 = hazelcast2.getCluster().getLocalMember();
        LOG.log(INFO, member2.getAttributes().entrySet().stream()
                .map(entry -> entry.getKey() + " -> " + entry.getValue() + " : " + entry.getValue().getClass().getName())
                .collect(Collectors.joining("\n", "member 2 attributes\n", "\n"))
        );

        assertThat(member2.getStringAttribute("string"), is("hello"));
        assertThat(member2.getIntAttribute("int"), is(1));
        assertThat(member2.getBooleanAttribute("bool"), is(true));
    }

    /**
     * Test of hazelcastConfigFactory method, of class HazelcastConfigFactory.
     */
    @Test
    public void testHazelcastConfigFactory() {
        LOG.info("hazelcastConfigFactory");
        String hazelcastInstanceName = "testHazelcastConfigFactory";

        final HazelcastConfigFactory factory1 = HazelcastConfigFactory.hazelcastConfigFactory(hazelcastInstanceName + 1);
        final com.hazelcast.config.Config hazelcastConfig1 = factory1.apply(config.getConfig(ConfigUtil.joinPath("Hazelcast", "application-1")));
        final HazelcastInstance hazelcast1 = Hazelcast.newHazelcastInstance(hazelcastConfig1);

        final HazelcastConfigFactory factory2 = HazelcastConfigFactory.hazelcastConfigFactory(hazelcastInstanceName + 2);
        final com.hazelcast.config.Config hazelcastConfig2 = factory2.apply(config.getConfig(ConfigUtil.joinPath("Hazelcast", "application-2")));
        final HazelcastInstance hazelcast2 = Hazelcast.newHazelcastInstance(hazelcastConfig2);

        test(hazelcast1, hazelcast2);
    }

}
