/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core;

import static co.runrightfast.core.TypeSafeObjectRegistry.GLOBAL_OBJECT_REGISTRY;
import co.runrightfast.core.TypeSafeObjectRegistry.ObjectRegistration;
import co.runrightfast.core.application.event.AppEvent;
import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.vertx.core.application.ApplicationId;
import java.util.function.Function;
import java.util.function.Supplier;
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

    public static final TypeReference<AppEventLogger> AppEventLogger_TYPE = new TypeReference<AppEventLogger>() {
    };

    public static final TypeReference<EchoService> EchoService_TYPE = new TypeReference<EchoService>() {
    };

    private static final TypeReference<EchoService2> EchoService2_TYPE = new TypeReference<EchoService2>() {
    };

    public static final TypeReference<AppEventJDKLogger> Log4j2Appender_TYPE = new TypeReference<AppEventJDKLogger>() {
    };

    public static final TypeReference<Supplier<AppEventLogger>> AppEventLoggerServiceProvider_TYPE = new TypeReference<Supplier<AppEventLogger>>() {
    };

    /**
     * Test of put method, of class GLOBAL_OBJECT_REGISTRY.
     */
    @Test
    public void testPut() {
        final ApplicationId appId = ApplicationId.builder().group("co.runrigtfast").name("test").version("1.0.0").build();
        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(AppEventLogger_TYPE, new AppEventJDKLogger(appId))).isPresent(), is(false));

        final AppEventLogger appEventLogger = GLOBAL_OBJECT_REGISTRY.get(AppEventLogger_TYPE).get();
        appEventLogger.accept(AppEvent.info("testPut").build());

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

        final Supplier<AppEventLogger> appEventLoggerProvider = () -> new AppEventJDKLogger(appId);
        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(AppEventLoggerServiceProvider_TYPE, appEventLoggerProvider)).isPresent(), is(false));
        assertThat(GLOBAL_OBJECT_REGISTRY.put(new ObjectRegistration<>(AppEventLoggerServiceProvider_TYPE, appEventLoggerProvider)).isPresent(), is(true));
    }

}
