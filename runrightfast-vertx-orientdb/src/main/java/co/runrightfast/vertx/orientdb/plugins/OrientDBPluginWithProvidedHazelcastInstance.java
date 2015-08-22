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

import co.runrightfast.vertx.orientdb.verticle.OrientDBVerticle;
import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import java.util.function.Supplier;

/**
 *
 * @author alfio
 */
public final class OrientDBPluginWithProvidedHazelcastInstance extends OHazelcastPlugin {

    /**
     * OrientDB requires that the plugin instance be created by OrientDB using the default constructor. Thus, we need to "inject" the HazelcastInstance. The
     * purpose is to be able to share the HazelcastInstance.
     *
     * HazelcastInstanceWithNoopShutdownSupplier implies that OrientDB is deployed via {@link OrientDBVerticle}. By default, it will try to re-use the
     * HazelcastInstance used by the Vertx cluster it is running in. Both OrientDB and Vertx want to manage the HazelcastInstance lifecycle. However, if
     * OrientDB is re-using Vertx's HazelcastInstance, then we don't want OrientDB to shutdown the HazelcastInsatnce - we want Vertx to control shutdown.
     */
    public static Supplier<HazelcastInstance> HAZELCAST_INSTANCE = new HazelcastInstanceWithNoopShutdownSupplier();

    @Override
    protected HazelcastInstance configureHazelcast() {
        return HAZELCAST_INSTANCE.get();
    }

}
