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
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_EMPTY;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public enum ServerResource {

    ANY("*"),
    DATABASES_LIST("server.listDatabases"),
    DATABASE_CREATE("database.create"),
    DATABASE_DROP("database.drop"),
    DATABASE_PASSTHROUGH("database.passthrough");

    public final String resource;

    private ServerResource(final String resource) {
        this.resource = resource;
    }

    /**
     *
     * @param user user name
     * @param password user password
     * @param resources at least 1 is required
     * @return
     */
    public static OServerUserConfiguration serverUserConfiguration(final String user, final String password, final ServerResource... resources) {
        checkArgument(isNotBlank(user), MUST_NOT_BE_BLANK, "user");
        checkArgument(isNotBlank(password), MUST_NOT_BE_BLANK, "password");
        checkArgument(ArrayUtils.isNotEmpty(resources), MUST_NOT_BE_EMPTY, "resources");
        return new OServerUserConfiguration(user, password, Arrays.stream(resources).map(resource -> resource.resource).collect(Collectors.joining(",")));
    }

    /**
     *
     * @param user user name
     * @param password user password
     * @param resources at least 1 is required. The resource names must match the enum names.
     * @return
     */
    public static OServerUserConfiguration serverUserConfiguration(final String user, final String password, final String... resources) {
        return serverUserConfiguration(
                user,
                password,
                Arrays.stream(resources).map(ServerResource::valueOf).toArray(ServerResource[]::new)
        );
    }

}
