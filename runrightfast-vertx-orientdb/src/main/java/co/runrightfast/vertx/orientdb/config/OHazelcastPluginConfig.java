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

import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.PATH_DOES_NOT_EXIST;
import co.runrightfast.vertx.orientdb.plugins.OrientDBPluginWithProvidedHazelcastInstance;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public final class OHazelcastPluginConfig implements Supplier<OServerHandlerConfiguration> {

    @Getter
    private final boolean enabled;

    @Getter
    private final String nodeName;

    @Getter
    private final Path distributedDBConfigFilePath;

    /**
     * enabled = false, nodeName = null, distributedDBConfigFilePath = null
     */
    public OHazelcastPluginConfig() {
        this.enabled = false;
        this.nodeName = null;
        this.distributedDBConfigFilePath = null;
    }

    public OHazelcastPluginConfig(final String nodeName, @NonNull final Path distributedDBConfigFilePath) {
        checkArgument(isNotBlank(nodeName), MUST_NOT_BE_BLANK, "nodeName");
        checkArgument(Files.exists(distributedDBConfigFilePath), PATH_DOES_NOT_EXIST, "distributedDBConfigFilePath", distributedDBConfigFilePath.toAbsolutePath());
        this.enabled = true;
        this.nodeName = nodeName;
        this.distributedDBConfigFilePath = distributedDBConfigFilePath;
    }

    @Override
    public OServerHandlerConfiguration get() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OrientDBPluginWithProvidedHazelcastInstance.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", Boolean.toString(enabled)),
            new OServerParameterConfiguration("nodeName", nodeName),
            new OServerParameterConfiguration("configuration.db.default", distributedDBConfigFilePath.toAbsolutePath().toString())
        };
        return config;
    }

}
