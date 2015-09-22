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
package co.runrightfast.vertx.demo;

import co.runrightfast.vertx.core.application.RunRightFastVertxApplicationLauncher;
import co.runrightfast.vertx.core.components.RunRightFastVertxApplication;
import co.runrightfast.core.utils.JmxUtils;
import co.runrightfast.vertx.demo.components.DaggerDemoApp;
import co.runrightfast.vertx.demo.testHarness.jmx.DemoMXBean;
import co.runrightfast.vertx.demo.testHarness.jmx.DemoMXBeanImpl;
import io.vertx.core.Vertx;

/**
 *
 * @author alfio
 */
public final class VertxApp {

    public static void main(final String[] args) {
        System.exit(RunRightFastVertxApplicationLauncher.run(VertxApp::runRightFastVertxApplication, args));
    }

    public static RunRightFastVertxApplication runRightFastVertxApplication() {
        final RunRightFastVertxApplication app = DaggerDemoApp.create();
        registerDemoMXBean(app);
        return app;
    }

    private static void registerDemoMXBean(final RunRightFastVertxApplication app) {
        final String jmxDomain = app.runRightFastApplication().getJmxDefaultDomain();
        final Vertx vertx = app.vertxService().getVertx();
        final DemoMXBean mbean = new DemoMXBeanImpl(vertx, app.getConfig());
        JmxUtils.registerApplicationMBean(jmxDomain, mbean, DemoMXBean.class);
    }
}
