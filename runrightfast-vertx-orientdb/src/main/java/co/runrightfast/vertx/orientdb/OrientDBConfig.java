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

import co.runrightfast.vertx.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Qualifier;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public final class OrientDBConfig {

    @Qualifier
    @Documented
    @Retention(RUNTIME)
    public @interface OrientDBConfiguration {
    }

    @Getter
    private final Path homeDirectory;

    public OrientDBConfig(@NonNull final Config config) {
        homeDirectory = Paths.get(ConfigUtils.getString(config, "home", "dir").orElse("/orientdb"));
    }
}
