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

import co.runrightfast.core.utils.ConfigUtils;
import static co.runrightfast.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_ZERO;
import static co.runrightfast.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import static com.google.common.base.Preconditions.checkArgument;
import com.typesafe.config.Config;
import static java.lang.Boolean.FALSE;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.java.Log;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Builder
@Log
@ToString(exclude = {"serverKeyStorePassword", "serverTrustStorePassword"})
public class NetworkSSLConfig {

    public static final int DEFAULT_SSL_PORT = 2434;

    @Getter
    private int port = DEFAULT_SSL_PORT;

    @Getter
    @NonNull
    private final Path serverKeyStorePath;
    @Getter
    @NonNull
    private final String serverKeyStorePassword;

    /**
     * if present, then it implies client certificate auth is enabled
     */
    @Getter
    @NonNull
    private final Optional<Path> serverTrustStorePath;
    @Getter
    @NonNull
    private final Optional<String> serverTrustStorePassword;

    public NetworkSSLConfig(final int port, @NonNull final Path serverKeyStorePath, @NonNull final String serverKeyStorePassword, @NonNull final Optional<Path> serverTrustStorePath, @NonNull final Optional<String> serverTrustStorePassword) {
        this.serverKeyStorePath = serverKeyStorePath;
        this.serverKeyStorePassword = serverKeyStorePassword;
        this.serverTrustStorePath = serverTrustStorePath;
        this.serverTrustStorePassword = serverTrustStorePassword;
        this.port = port <= 0 ? DEFAULT_SSL_PORT : port;
        validate();
    }

    public NetworkSSLConfig(@NonNull final Path serverKeyStorePath, @NonNull final String serverKeyStorePassword) {
        this(DEFAULT_SSL_PORT, serverKeyStorePath, serverKeyStorePassword, Optional.empty(), Optional.empty());
    }

    public NetworkSSLConfig(@NonNull final Path serverKeyStorePath, @NonNull final String serverKeyStorePassword, @NonNull final Path serverTrustStorePath, @NonNull final String serverTrustStorePassword) {
        this(DEFAULT_SSL_PORT, serverKeyStorePath, serverKeyStorePassword, Optional.of(serverTrustStorePath), Optional.of(serverTrustStorePassword));
    }

    public NetworkSSLConfig(@NonNull final Config config) {
        this.port = ConfigUtils.getInt(config, "port").orElse(DEFAULT_SSL_PORT);
        this.serverKeyStorePath = Paths.get(config.getString("keyStore"));
        this.serverKeyStorePassword = config.getString("keyStorePass");
        if (ConfigUtils.getBoolean(config, "clientAuthEnabled").orElse(FALSE)) {
            this.serverTrustStorePath = ConfigUtils.getString(config, "trustStore").map(Paths::get);
            this.serverTrustStorePassword = ConfigUtils.getString(config, "trustStorePass");
        } else {
            this.serverTrustStorePath = Optional.empty();
            this.serverTrustStorePassword = Optional.empty();
        }
    }

    private void validate() {
        checkArgument(isNotBlank(serverKeyStorePassword), MUST_NOT_BE_BLANK, "serverKeyStorePassword");
        if (serverTrustStorePath.isPresent()) {
            final String password = serverTrustStorePassword.orElseThrow(() -> new IllegalArgumentException("'serverTrustStorePassword' is required if 'serverTrustStorePath' is specified"));
            checkArgument(isNotBlank(password), MUST_NOT_BE_BLANK, "serverTrustStorePassword");
        }
        checkArgument(port > 0, MUST_BE_GREATER_THAN_ZERO, "port");
    }
}
