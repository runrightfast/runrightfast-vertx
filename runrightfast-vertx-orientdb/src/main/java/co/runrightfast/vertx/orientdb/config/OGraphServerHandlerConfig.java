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
package co.runrightfast.vertx.orientdb.config;

import co.runrightfast.vertx.core.utils.ConfigUtils;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_ZERO;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.graph.handler.OGraphServerHandler;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.typesafe.config.Config;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 *
 * @author alfio
 */
@ToString
@EqualsAndHashCode
public final class OGraphServerHandlerConfig implements Supplier<OServerHandlerConfiguration> {

    public static final int DEFAULT_GRAPH_POOL_MAZ_SIZE = 100;

    @Getter
    private final boolean enabled;

    @Getter
    private final int graphPoolMaxSize;

    public OGraphServerHandlerConfig(final boolean enabled, final int graphPoolMaxSize) {
        checkArgument(graphPoolMaxSize > 0, MUST_BE_GREATER_THAN_ZERO, "graphPoolMaxSize");
        this.enabled = enabled;
        this.graphPoolMaxSize = graphPoolMaxSize;
    }

    /**
     * graphPoolMaxSize = DEFAULT_GRAPH_POOL_MAZ_SIZE
     *
     * @param enabled
     */
    public OGraphServerHandlerConfig(final boolean enabled) {
        this(enabled, DEFAULT_GRAPH_POOL_MAZ_SIZE);
    }

    /**
     * enabled = true, graphPoolMaxSize = 100
     */
    public OGraphServerHandlerConfig() {
        this(true, DEFAULT_GRAPH_POOL_MAZ_SIZE);
    }

    /**
     *
     * @param config config example: <code>
     * {
     *    enabled = false
     *    graphPoolMaxSize = 100
     * }
     * </code>
     */
    public OGraphServerHandlerConfig(@NonNull final Config config) {
        this(
                ConfigUtils.getBoolean(config, "enabled").orElse(Boolean.FALSE),
                ConfigUtils.getInt(config, "graphPoolMaxSize").orElse(DEFAULT_GRAPH_POOL_MAZ_SIZE)
        );
    }

    @Override
    public OServerHandlerConfiguration get() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OGraphServerHandler.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", Boolean.toString(enabled)),
            new OServerParameterConfiguration("graph.pool.max", Integer.toString(graphPoolMaxSize))
        };
        return config;
    }

}
