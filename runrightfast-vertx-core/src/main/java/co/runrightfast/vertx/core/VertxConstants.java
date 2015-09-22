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

import co.runrightfast.core.utils.ConfigUtils;
import static co.runrightfast.core.utils.ConfigUtils.CONFIG_NAMESPACE;

/**
 *
 * @author alfio
 */
public interface VertxConstants {

    /**
     * The Vertx root config path used as the config namespace within the TypeSafe config.
     *
     * runrightfast.vertx
     */
    static final String VERTX_CONFIG_ROOT = ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx");

    /**
     * The Vertx registry name.
     */
    static final String VERTX_METRIC_REGISTRY_NAME = VERTX_CONFIG_ROOT;

    /**
     * The Vertx registry name.
     */
    static final String VERTX_HEALTHCHECK_REGISTRY_NAME = VERTX_CONFIG_ROOT;

    /**
     * The Vertx Hazelcast instance id
     */
    static final String VERTX_HAZELCAST_INSTANCE_ID = VERTX_CONFIG_ROOT;

}
