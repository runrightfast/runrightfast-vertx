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
package co.runrightfast.vertx.core.eventbus;

import static co.runrightfast.vertx.core.eventbus.EventBusAddress.RUNRIGHTFAST;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class EventBusAddressTest {

    /**
     * Test of eventBusAddress method, of class EventBusAddress.
     */
    @Test
    public void testAddress() {
        assertThat(EventBusAddress.eventBusAddress("a", "b", "c"), is("/a/b/c"));
        assertThat(EventBusAddress.eventBusAddress("a"), is("/a"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddress_withBlankPath() {
        EventBusAddress.eventBusAddress(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddress_withBlankPath2() {
        EventBusAddress.eventBusAddress("a", " ");
    }

    @Test
    public void testRunRightFastAddress() {
        assertThat(EventBusAddress.runrightfastEventBusAddress("a", "b", "c"), is("/" + RUNRIGHTFAST + "/a/b/c"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunRightFastAddress_withBlankPath2() {
        EventBusAddress.runrightfastEventBusAddress("a", " ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunRightFastAddress_withBlankPath1() {
        EventBusAddress.runrightfastEventBusAddress("  ");
    }

}
