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
package co.runrightfast.vertx.orientdb.impl.embedded;

import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_EMPTY;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.PATH_DOES_NOT_EXIST;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.PATH_IS_NOT_A_DIRECTORY;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.PATH_IS_NOT_READABLE;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.PATH_IS_NOT_WRITEABLE;
import co.runrightfast.vertx.orientdb.OrientDBPoolConfig;
import co.runrightfast.vertx.orientdb.config.OrientDBConfig;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.apache.commons.collections4.CollectionUtils;

/**
 *
 * TODO: research how to use OServerStorageConfiguration
 *
 * @author alfio
 */
@Builder
public final class EmbeddedOrientDBServiceConfig {

    @NonNull
    @Getter
    @Singular
    private final Set<OrientDBPoolConfig> databasePoolConfigs;

    @Getter
    @NonNull
    private final Path orientDBRootDir;

    @Getter
    @Singular
    private final Set<Supplier<OServerHandlerConfiguration>> handlers;

    @Getter
    @Singular
    private final Set<Supplier<ODatabaseLifecycleListener>> lifecycleListeners;

    @Getter
    @NonNull
    private final OServerNetworkConfiguration networkConfig;

    /**
     * The root user must be specified in order to administrate the database.
     */
    @Getter
    @NonNull
    @Singular
    private final Set<OServerUserConfiguration> users;

    @Getter
    @Singular
    private final Map<OGlobalConfiguration, String> properties;

    public static EmbeddedOrientDBServiceConfigBuilder newBuilder(@NonNull final OrientDBConfig config) {
        return EmbeddedOrientDBServiceConfig.builder()
                .orientDBRootDir(config.getHomeDirectory())
                .handlers(config.getHandlers())
                .networkConfig(config.getNetworkConfig().get())
                .users(config.getServerUsers())
                .properties(config.getProperties());
    }

    public void validate() {
        checkArgument(CollectionUtils.isNotEmpty(handlers), MUST_NOT_BE_EMPTY, "handlers");
        checkArgument(CollectionUtils.isNotEmpty(users), MUST_NOT_BE_EMPTY, "users");
        if (Files.exists(orientDBRootDir)) {
            checkOrientDBRootDir();
        } else {
            try {
                Files.createDirectories(orientDBRootDir);
            } catch (final IOException ex) {
                throw new RuntimeException("Failed to create OrientDB root dir: " + orientDBRootDir, ex);
            }
            checkOrientDBRootDir();
        }
    }

    private void checkOrientDBRootDir() {
        checkArgument(Files.exists(orientDBRootDir), PATH_DOES_NOT_EXIST, "orientDBRootDir", orientDBRootDir);
        checkArgument(Files.isDirectory(orientDBRootDir), PATH_IS_NOT_A_DIRECTORY, "orientDBRootDir", orientDBRootDir);
        checkArgument(Files.isReadable(orientDBRootDir), PATH_IS_NOT_READABLE, "orientDBRootDir", orientDBRootDir);
        checkArgument(Files.isWritable(orientDBRootDir), PATH_IS_NOT_WRITEABLE, "orientDBRootDir", orientDBRootDir);
    }

}
