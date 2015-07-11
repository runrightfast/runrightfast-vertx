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
//package co.runrightfast.vertx.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 *
 * @author alfio
 */
public class RunRightFastVerticle extends AbstractVerticle {

    protected MetricRegistry metricRegistry;

    protected HealthCheckRegistry healthCheckRegistry;

    @Override
    public void init(final Vertx vertx, final Context context) {
        super.init(vertx, context);
        this.metricRegistry = SharedMetricRegistries.getOrCreate(context.deploymentID());
        this.healthCheckRegistry = SharedHealthCheckRegistries.getOrCreate(context.deploymentID());
    }

}
