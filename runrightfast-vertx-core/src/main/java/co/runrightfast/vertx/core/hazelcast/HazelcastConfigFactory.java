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

import co.runrightfast.vertx.core.hazelcast.impl.TypesafeHazelcastConfig;
import com.hazelcast.config.SerializerConfig;
import com.typesafe.config.Config;
import java.util.Set;
import java.util.function.Function;

/**
 *
 * @author alfio
 */
@FunctionalInterface
public interface HazelcastConfigFactory extends Function<Config, com.hazelcast.config.Config> {

    static HazelcastConfigFactory hazelcastConfigFactory(final String hazelcastInstanceName, final Set<SerializerConfig> serializerConfigs) {
        return config -> new TypesafeHazelcastConfig(hazelcastInstanceName, config, serializerConfigs).getConfig();
    }

    static HazelcastConfigFactory hazelcastConfigFactory(final String hazelcastInstanceName) {
        return config -> new TypesafeHazelcastConfig(hazelcastInstanceName, config).getConfig();
    }

}
