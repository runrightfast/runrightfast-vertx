
import co.runrightfast.core.utils.ConfigUtils;
import static co.runrightfast.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.util.logging.Level.INFO;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

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
/**
 *
 * @author alfio
 */
@Log
public class LocalDemoAppSetup {

    @Test
    public void appSetup() {
        final Config config = ConfigFactory.load();
        final File orientdbHome = new File(config.getString(ConfigUtils.configPath(CONFIG_NAMESPACE, "orientdb", "server", "home", "dir")));

        orientdbHome.mkdirs();

        log.logp(INFO, getClass().getName(), "providesEmbeddedOrientDBServiceConfig", String.format("orientdbHome.exists() = %s", orientdbHome.exists()));
        final Path defaultDistributedDBConfigFile = Paths.get(orientdbHome.toPath().toAbsolutePath().toString(), "config", "default-distributed-db-config.json");
        log.info(String.format("defaultDistributedDBConfigFile = %s", defaultDistributedDBConfigFile));
        if (!Files.exists(defaultDistributedDBConfigFile)) {
            try {
                Files.createDirectories(defaultDistributedDBConfigFile.getParent());
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
            try (final InputStream is = getClass().getResourceAsStream("/orientdb/config/default-distributed-db-config.json")) {
                try (final OutputStream os = new FileOutputStream(defaultDistributedDBConfigFile.toFile())) {
                    IOUtils.copy(is, os);
                }
                log.info(String.format("copied over defaultDistributedDBConfigFile : %s", defaultDistributedDBConfigFile));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
