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

import co.runrightfast.vertx.core.VertxOptionsFactory;
import static co.runrightfast.vertx.core.VertxOptionsFactory.configureMetricsOptions;
import static co.runrightfast.vertx.core.utils.ConfigUtils.configPath;
import static com.google.common.base.Preconditions.checkNotNull;
import com.typesafe.config.Config;
import io.vertx.core.VertxOptions;

/**
 *
 * @author alfio
 */
public class VertxOptionsFactoryImpl implements VertxOptionsFactory {

    private final Config config;

    public VertxOptionsFactoryImpl(final Config config) {
        checkNotNull(config);
        this.config = config;
    }

    @Override
    public VertxOptions get() {
        final VertxOptions options = new VertxOptions();
        final String METRICS = "metrics";
        if (config.hasPath(METRICS)) {
            final String JMX_ENABLED = "jmxEnabled";
            if (config.hasPath(configPath(METRICS, JMX_ENABLED))) {
                configureMetricsOptions(options, config.getBoolean(configPath(METRICS, JMX_ENABLED)));
            }
        }
        return options;
    }

}
