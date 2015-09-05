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
package co.runrightfast.vertx.orientdb;

import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_ZERO;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import co.runrightfast.vertx.orientdb.classes.DocumentObject;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@EqualsAndHashCode(of = {"databaseName"})
@ToString(exclude = "password")
public class DatabasePoolConfig {

    @Getter
    private final String databaseName;

    @Getter
    private final String databaseUrl;

    @Getter
    private final String userName;

    @Getter
    private final String password;

    @Getter
    private final int maxPoolSize;

    /**
     * domain classes that are managed by this database
     */
    @Getter
    private final Set<Class<? extends DocumentObject>> documentClasses;

    public DatabasePoolConfig(final String databaseName, final String databaseUrl, final String userName, final String password, final int maxPoolSize, final Class<? extends DocumentObject>... documentClasses) {
        checkArgument(isNotBlank(databaseName), MUST_NOT_BE_BLANK, databaseName);
        checkArgument(isNotBlank(databaseUrl), MUST_NOT_BE_BLANK, databaseUrl);
        checkArgument(isNotBlank(userName), MUST_NOT_BE_BLANK, userName);
        checkArgument(isNotBlank(password), MUST_NOT_BE_BLANK, password);
        checkArgument(maxPoolSize > 0, MUST_BE_GREATER_THAN_ZERO, "maxPoolSize");
        this.databaseName = databaseName.trim();
        this.databaseUrl = databaseUrl.trim();
        this.userName = userName.trim();
        this.password = password.trim();
        this.maxPoolSize = maxPoolSize;
        this.documentClasses = ArrayUtils.isNotEmpty(documentClasses) ? ImmutableSet.copyOf(documentClasses) : ImmutableSet.of();
    }

    /**
     * with
     * <ol>
     * <li>createDatabase = false
     * <li>databaseUrl = "plocal:" + databaseName - e.g., if databaseName = "config", then databaseUrl = "plocal:config". This assumes that the database is
     * located underneath ${ORIENTDB_HOME}/databases.
     * </ol>
     *
     * @param databaseName
     * @param userName
     * @param password
     * @param maxPoolSize
     * @param documentClasses
     */
    public DatabasePoolConfig(final String databaseName, final String userName, final String password, final int maxPoolSize, final Class<? extends DocumentObject>... documentClasses) {
        this(databaseName, "plocal:" + databaseName, userName, password, maxPoolSize, documentClasses);
    }

    /**
     *
     * @return
     */
    public String getDatabaseNameFromDatabaseUrl() {
        final String url = StringUtils.split(databaseUrl, ':')[1];
        final int startIndex = url.lastIndexOf('/') + 1;
        return url.substring(startIndex);
    }

}
