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
package co.runrightfast.vertx.orientdb.plugins;

import static co.runrightfast.vertx.core.VertxConstants.VERTX_HAZELCAST_INSTANCE_ID;
import static co.runrightfast.core.hazelcast.HazelcastInstanceWithNoopShutdown.hazelcastInstanceWithNoopShutdown;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.function.Supplier;

/**
 *
 * @author alfio
 */
public class HazelcastInstanceWithNoopShutdownSupplier implements Supplier<HazelcastInstance> {

    private HazelcastInstance hazelcast;

    @Override
    public HazelcastInstance get() {
        if (hazelcast == null) {
            this.hazelcast = hazelcastInstanceWithNoopShutdown(Hazelcast.getHazelcastInstanceByName(VERTX_HAZELCAST_INSTANCE_ID));
        }
        return hazelcast;
    }

}
