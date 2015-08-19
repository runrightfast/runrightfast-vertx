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

import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_ZERO;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Builder
public final class EmbeddedOrientDBServiceConfig {

    @NonNull
    @Getter
    private final String userName;

    @NonNull
    @Getter
    private final String password;

    @Getter
    private final int poolMaxSize;

    @Getter
    @NonNull
    private final Path orientDBRootDir;

    @Getter
    private final Set<OServerHandlerConfiguration> handlers;

    @Getter
    private final Set<ODatabaseLifecycleListener> lifecycleListeners;

    @Getter
    private final Set<ORecordHook> hooks;

    public void validate() {
        checkArgument(isNotBlank(userName), MUST_NOT_BE_BLANK, "userName");
        checkArgument(isNotBlank(password), MUST_NOT_BE_BLANK, "password");
        checkArgument(poolMaxSize > 0, MUST_BE_GREATER_THAN_ZERO, "poolMaxSize");
        if (Files.exists(orientDBRootDir)) {
            checkArgument(Files.isDirectory(orientDBRootDir), "%s is not a directory", orientDBRootDir);
            checkArgument(Files.isReadable(orientDBRootDir), "%s is not readable", orientDBRootDir);
            checkArgument(Files.isWritable(orientDBRootDir), "%s is not writable", orientDBRootDir);
        }
    }

}
