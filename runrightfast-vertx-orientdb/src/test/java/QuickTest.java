
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import static java.util.logging.Level.INFO;
import lombok.extern.java.Log;
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
public class QuickTest {

    @Test
    public void testPath() {
        final Path path = Paths.get("src", "main", "java");
        log.logp(INFO, getClass().getName(), "testPath", "path = {0}", path);
        log.logp(INFO, getClass().getName(), "testPath", "path.toAbsolutePath() = {0}", path.toAbsolutePath());
    }

    @Test
    public void testDefaultKeyStoreType() {
        log.info(String.format("Default KeyStore type = %s", KeyStore.getDefaultType()));
    }

}
