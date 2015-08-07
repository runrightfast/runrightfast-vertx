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

import static java.util.logging.Level.INFO;
import javax.json.Json;
import lombok.extern.java.Log;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class LoggingUtilsTest {

    @Test
    public void testLog_withNullClassName() {
        LoggingUtils.log(log, INFO, null, "testLog_withNullClassName", () -> Json.createObjectBuilder().build());
    }

    @Test
    public void testLog_withNullMethodName() {
        LoggingUtils.log(log, INFO, getClass().getSimpleName(), null, () -> Json.createObjectBuilder().build());
    }

    @Test(expected = NullPointerException.class)
    public void testLog_withNullThrowable() {
        LoggingUtils.log(log, INFO, getClass().getSimpleName(), "testLog_withNullThrowable", () -> Json.createObjectBuilder().build(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testLog_withNullJsonObjectSupplier() {
        LoggingUtils.log(log, INFO, getClass().getSimpleName(), "testLog_withNullJsonObjectSupplier", null);
    }

    @Test(expected = NullPointerException.class)
    public void testLog_withNullLevel() {
        LoggingUtils.log(log, null, getClass().getSimpleName(), "testLog_withNullLevel", () -> Json.createObjectBuilder().build());
    }

}
