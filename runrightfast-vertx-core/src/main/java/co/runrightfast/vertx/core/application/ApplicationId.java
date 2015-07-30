/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.vertx.core.application;

import static co.runrightfast.vertx.core.utils.ConfigUtils.configPath;
import com.typesafe.config.Config;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@Builder
@Data
public final class ApplicationId {

    private final String group;

    private final String name;

    private final String version;

    /**
     * Config schema:
     *
     * <code>
     * {
     *    app {
     *       group = co.runrightfast
     *       name = security-service
     *       version = 1.0.0
     *    }
     * }
     * </code>
     *
     * @param config Config
     * @return ApplicatioId
     */
    public static ApplicationId fromConfig(@NonNull final Config config) {
        return builder()
                .group(config.getString(configPath("app", "group")))
                .name(config.getString(configPath("app", "name")))
                .version(config.getString(configPath("app", "version")))
                .build();
    }

}
