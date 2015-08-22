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
package co.runrightfast.vertx.core;

import static co.runrightfast.vertx.core.VertxConstants.VERTX_METRIC_REGISTRY_NAME;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.util.concurrent.Service;
import com.hazelcast.core.HazelcastInstance;
import com.typesafe.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @author alfio
 */
public interface VertxService extends Service {

    static final Logger LOG = Logger.getLogger(VertxService.class.getName());

    /**
     * Use case: When Vertx is clustered, verticles can retrieve the HazelcastInstance that the Vertx cluster is using.
     */
    static final Map<Vertx, Optional<HazelcastInstance>> hazelcastInstances = new ConcurrentHashMap<>(1);

    Vertx getVertx();

    /**
     *
     * @return config which was used to create the VertxOptions
     */
    Config getConfig();

    /**
     *
     * @return a copy of the VertxOptions that were used to create the Vertx instance
     */
    VertxOptions getVertxOptions();

    Map<String, RunRightFastVerticleDeployment> deployedVerticles();

    Set<RunRightFastVerticleDeployment> deployments();

    /**
     *
     * @return HazelcastInstance that is being used by Vertx - or empty if Vertx is not clustered
     */
    Optional<HazelcastInstance> getHazelcastInstance();

    static final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(VERTX_METRIC_REGISTRY_NAME);

}
