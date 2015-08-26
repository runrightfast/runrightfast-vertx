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
package co.runrightfast.vertx.core.utils;

import static co.runrightfast.vertx.core.utils.JvmProcess.HOST;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigUtil;
import com.typesafe.config.ConfigValue;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author alfio
 */
public interface ConfigUtils {

    static final String CONFIG_NAMESPACE = "runrightfast";

    static String configPath(final String name, final String... names) {
        checkArgument(StringUtils.isNotBlank(name));
        if (ArrayUtils.isNotEmpty(names)) {
            return ConfigUtil.joinPath(ImmutableList.<String>builder().add(name).add(names).build());
        }
        return name;
    }

    /**
     * if the path is a bad path expression, then false is returned
     *
     * @param config config
     * @param path base path
     * @param paths additional paths to be joined to the base path
     * @return if path exists
     */
    static boolean hasPath(final Config config, final String path, final String... paths) {
        checkNotNull(config);
        checkArgument(StringUtils.isNotBlank(path));
        return config.hasPath(configPath(path, paths));
    }

    static Optional<Config> getConfig(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getConfig(configPath(path, paths)));
    }

    static Optional<List<? extends Config>> getConfigList(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getConfigList(configPath(path, paths)));
    }

    static Optional<Boolean> getBoolean(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getBoolean(configPath(path, paths)));
    }

    static Optional<Double> getDouble(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getDouble(configPath(path, paths)));
    }

    static Optional<Long> getDuration(final Config config, final TimeUnit unit, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getDuration(configPath(path, paths), unit));
    }

    static Optional<Integer> getInt(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getInt(configPath(path, paths)));
    }

    static Optional<Long> getLong(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getLong(configPath(path, paths)));
    }

    static Optional<Number> getNumber(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getNumber(configPath(path, paths)));
    }

    static Optional<ConfigObject> getObject(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getObject(configPath(path, paths)));
    }

    static Optional<List<? extends ConfigObject>> getObjectList(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getObjectList(configPath(path, paths)));
    }

    static Optional<String> getString(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getString(configPath(path, paths)));
    }

    static Optional<List<String>> getStringList(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getStringList(configPath(path, paths)));
    }

    static Optional<ConfigValue> getValue(final Config config, final String path, final String... paths) {
        if (!hasPath(config, path, paths)) {
            return Optional.empty();
        }
        return Optional.of(config.getValue(configPath(path, paths)));
    }

    static String renderConfig(final Config config) {
        return renderConfig(config, false, false, false);
    }

    static String renderConfig(final Config config, final boolean comments, final boolean orginComments, final boolean json) {
        checkNotNull(config);
        final ConfigRenderOptions options = ConfigRenderOptions.defaults()
                .setComments(comments)
                .setOriginComments(orginComments)
                .setJson(json)
                .setFormatted(true);
        return config.root().render(options);
    }

    static JsonObject toJsonObject(final Config config) {
        checkNotNull(config);
        try (final JsonReader reader = Json.createReader(new StringReader(renderConfigAsJson(config, false)))) {
            return reader.readObject();
        }
    }

    static String renderConfigAsJson(final Config config, final boolean formatted) {
        checkNotNull(config);
        final ConfigRenderOptions options = ConfigRenderOptions.defaults()
                .setComments(false)
                .setOriginComments(false)
                .setJson(true)
                .setFormatted(formatted);
        return config.root().render(options);
    }

    /**
     * same as loadConfig(false);
     *
     * @return Config
     */
    static Config loadConfig() {
        return loadConfig(false);
    }

    /**
     * Injects the following properties before the config is loaded as a JVM system property: HOSTNAME
     *
     * @param invalidateCaches if true, then the config caches are cleared
     * @return Config
     */
    static Config loadConfig(final boolean invalidateCaches) {
        System.setProperty("HOSTNAME", HOST);
        if (invalidateCaches) {
            ConfigFactory.invalidateCaches();
        }
        return ConfigFactory.load();
    }

    /**
     *
     * Injects the following properties before the config is loaded as a JVM system property: HOSTNAME, config.resource
     *
     * @param configResource used to set JVM system property : 'config.resource'
     * @param invalidateCaches if true, then the config caches are cleared
     * @return Config
     */
    static Config loadConfig(final String configResource, final boolean invalidateCaches) {
        checkArgument(StringUtils.isNotBlank(configResource));
        System.setProperty("config.resource", configResource);
        return loadConfig(invalidateCaches);
    }

    static Properties toProperties(final Config config) {
        checkNotNull(config);
        final Properties props = new Properties();
        config.entrySet().forEach(entry -> props.setProperty(entry.getKey(), entry.getValue().unwrapped().toString()));
        return props;
    }

}
