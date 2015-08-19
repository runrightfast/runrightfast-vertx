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
package co.runrightfast.vertx.orientdb.impl;

import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_EMPTY;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
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
    private final Set<DatabasePoolConfig> databasePoolConfigs;

    @Getter
    @NonNull
    private final Path orientDBRootDir;

    @Getter
    private final Set<OServerHandlerConfiguration> handlers;

    @Getter
    private final Set<ODatabaseLifecycleListener> lifecycleListeners;

    @Getter
    private final Set<ORecordHook> hooks;

    @Getter
    @NonNull
    private final OServerNetworkConfiguration networkConfig;

    /**
     * The root user must be specified in order to administrate the database.
     */
    @Getter
    @NonNull
    private final Set<OServerUserConfiguration> users;

    @Getter
    private final Map<OGlobalConfiguration, String> properties;

    public void validate() {
        checkArgument(CollectionUtils.isNotEmpty(databasePoolConfigs), MUST_NOT_BE_EMPTY, "databasePoolConfigs");
        checkArgument(CollectionUtils.isNotEmpty(handlers), MUST_NOT_BE_EMPTY, "handlers");
        checkArgument(CollectionUtils.isNotEmpty(users), MUST_NOT_BE_EMPTY, "users");
        if (Files.exists(orientDBRootDir)) {
            checkArgument(Files.isDirectory(orientDBRootDir), "%s is not a directory", orientDBRootDir);
            checkArgument(Files.isReadable(orientDBRootDir), "%s is not readable", orientDBRootDir);
            checkArgument(Files.isWritable(orientDBRootDir), "%s is not writable", orientDBRootDir);
        }
    }

}
