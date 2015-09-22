/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core;

import static co.runrightfast.core.TypeSafeObjectRegistry.GLOBAL_OBJECT_REGISTRY;
import co.runrightfast.core.TypeSafeObjectRegistry.ObjectRegistration;
import java.util.function.Function;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class TypeSafeObjectRegistryTest {

    @BeforeClass
    public static void setUpClass() {
        GLOBAL_OBJECT_REGISTRY.clear();
    }

    @AfterClass
    public static void tearDownClass() {
        GLOBAL_OBJECT_REGISTRY.clear();
    }

    @Before
    public void setUp() {
        GLOBAL_OBJECT_REGISTRY.clear();
    }

    @After
    public void tearDown() {
        GLOBAL_OBJECT_REGISTRY.clear();
    }

    @FunctionalInterface
    public static interface EchoService extends Function<String, String> {

    }

    @FunctionalInterface
    private static interface EchoService2 extends Function<String, String> {

    }

    public static final TypeReference<EchoService> EchoService_TYPE = new TypeReference<EchoService>() {
    };

    private static final TypeReference<EchoService2> EchoService2_TYPE = new TypeReference<EchoService2>() {
    };

    /**
     * Test of put method, of class GLOBAL_OBJECT_REGISTRY.
     */
    @Test
    public void testPut() {

        final EchoService echoService = msg -> msg;
        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(EchoService_TYPE, echoService)).isPresent(), is(false));
        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(EchoService_TYPE, echoService)).isPresent(), is(true));
        assertThat(GLOBAL_OBJECT_REGISTRY.get(EchoService_TYPE).isPresent(), is(true));

        log.info(String.format("ObjectRegistry.getRegisteredTypes() = %s", GLOBAL_OBJECT_REGISTRY.getRegisteredTypes()));

        assertThat(GLOBAL_OBJECT_REGISTRY.remove(EchoService_TYPE).isPresent(), is(true));
        assertThat(GLOBAL_OBJECT_REGISTRY.get(EchoService_TYPE).isPresent(), is(false));

        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(EchoService_TYPE, echoService)).isPresent(), is(false));
        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(EchoService_TYPE, echoService)).isPresent(), is(true));
        assertThat(GLOBAL_OBJECT_REGISTRY.get(EchoService_TYPE).isPresent(), is(true));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutPrivateInterfaceIsNotAllowed() {
        final EchoService2 echoService2 = msg -> msg;
        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(EchoService2_TYPE, echoService2)).isPresent(), is(false));
    }

}
