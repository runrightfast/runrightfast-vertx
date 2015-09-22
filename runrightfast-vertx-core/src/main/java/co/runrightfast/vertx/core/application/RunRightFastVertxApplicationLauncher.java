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
package co.runrightfast.vertx.core.application;

import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.VertxService;
import co.runrightfast.vertx.core.application.jmx.ApplicationMXBean;
import co.runrightfast.vertx.core.application.jmx.impl.ApplicationMXBeanImpl;
import co.runrightfast.vertx.core.components.ApplicationInfo;
import co.runrightfast.vertx.core.components.DaggerApplicationInfo;
import co.runrightfast.vertx.core.components.RunRightFastVertxApplication;
import static co.runrightfast.vertx.core.eventbus.EventBusAddress.runrightfastEventBusAddress;
import co.runrightfast.vertx.core.modules.RunRightFastApplicationModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.core.utils.ConfigUtils;
import static co.runrightfast.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import static co.runrightfast.core.utils.ConfigUtils.configPath;
import co.runrightfast.core.utils.JmxUtils;
import co.runrightfast.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.typesafe.config.Config;
import java.util.function.Supplier;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * The design pattern for an application is:
 *
 * <ol>
 * <li>Create an application main class which provides a method to create an {@link RunRightFastVertxApplication} instance
 * <li>The application main method delegates to {@link RunRightFastVertxApplicationLauncher#run(java.util.function.Supplier, java.lang.String...)}
 * </ol>
 *
 * <pre>
 *
 * public final class MyApp  {
 *     public static void main(final String[] args){
 *         System.exit(RunRightFastVertxApplicationLauncher.run(MyApp::runRightFastVertxApplication, args));
 *     }
 *
 *     private static RunRightFastVertxApplication runRightFastVertxApplication(){
 *         ...
 *     }
 * }
 * </pre>
 *
 * @author alfio
 */
@Log
@Builder
public final class RunRightFastVertxApplicationLauncher extends AbstractIdleService {

    public static interface EventBusAddresses {

        static final String APP_INSTANCE_STARTED = runrightfastEventBusAddress("application-instance", "started");
        static final String APP_INSTANCE_STOPPED = runrightfastEventBusAddress("application-instance", "stopped");
    }

    private static final String CLASS_NAME = RunRightFastVertxApplicationLauncher.class.getName();

    @Getter
    @NonNull
    private final RunRightFastVertxApplication app;

    private final Service.Listener vertxServiceShutdownListener = new Service.Listener() {

        @Override
        public void failed(final Service.State from, Throwable failure) {
            log.logp(SEVERE, CLASS_NAME, "vertxServiceShutdownListener", "failed", failure);
            ServiceUtils.stopAsync(RunRightFastVertxApplicationLauncher.this);
        }

        @Override
        public void terminated(final Service.State from) {
            log.logp(INFO, CLASS_NAME, "vertxServiceShutdownListener", "terminated");
            ServiceUtils.stopAsync(RunRightFastVertxApplicationLauncher.this);
        }

    };

    /**
     * Runs an application on the caller's thread. The calling thread will block, waiting for the application to terminate.
     *
     * @param app supplies an RunRightFastVertxApplication instance. The recommended approach is to supply a Dagger2 component which extends the
     * RunRightFastVertxApplication interface, which includes the following modules:
     *
     * <ol>
     * <li>{@link RunRightFastApplicationModule}
     * <li>{@link VertxServiceModule}
     * </ol>
     *
     * plus your modules that provide the application's {@link RunRightFastVerticle} instances and their corresponding {@link RunRightFastVerticleDeployment}
     * definitions.
     *
     * @param args
     * @return exit code, where
     * <ul>
     * <li>0 = application terminated normally
     * <li>1 = application aborted with an exception
     * <li>2 = invalid command line usage
     * </ul>
     */
    public static int run(@NonNull final Supplier<RunRightFastVertxApplication> app, final String... args) {
        final int returnCode = processCommandLine(args);
        if (returnCode != -1) {
            return returnCode;
        }

        try {
            runApp(app);
            return 0;
        } catch (final Throwable e) {
            log.logp(SEVERE, CLASS_NAME, "run", "unexpected exception", e);
            return 1;
        }
    }

    private static int processCommandLine(final String... args) {
        if (ArrayUtils.isNotEmpty(args)) {
            final Options options = cliOptions();
            try {
                final CommandLineParser parser = new DefaultParser();
                final CommandLine cmd = parser.parse(options, args);

                if (cmd.hasOption('v')) {
                    printVersion();
                    return 0;
                }

                if (cmd.hasOption('h')) {
                    printHelp(options);
                    return 0;
                }

                if (cmd.hasOption('c')) {
                    printConfig();
                    return 0;
                }
            } catch (final ParseException e) {
                System.out.println(e.getMessage());
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ant", options);
                return 2;
            } catch (final Throwable e) {
                log.logp(SEVERE, CLASS_NAME, "run", "unexpected exception", e);
                return 1;
            }
        }

        return -1;
    }

    private static void printConfig() {
        final ApplicationInfo appInfo = DaggerApplicationInfo.create();
        final Config config = appInfo.runRightFastApplication().getConfig();
        System.out.println(ConfigUtils.renderConfigAsJson(config, true));
    }

    private static void printVersion() {
        final ApplicationInfo appInfo = DaggerApplicationInfo.create();
        final ApplicationId appId = appInfo.runRightFastApplication().getApplicationId();
        System.out.println(appId.toJson());
    }

    private static void printHelp(final Options options) {
        final ApplicationInfo appInfo = DaggerApplicationInfo.create();
        final ApplicationId appId = appInfo.runRightFastApplication().getApplicationId();

        String header = ConfigUtils.getString(appInfo.runRightFastApplication().getConfig(), configPath(CONFIG_NAMESPACE, "app", "cli", "help", "header")).orElse(StringUtils.EMPTY);
        String footer = ConfigUtils.getString(appInfo.runRightFastApplication().getConfig(), configPath(CONFIG_NAMESPACE, "app", "cli", "help", "footer")).orElse(StringUtils.EMPTY);

        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(appId.getName(), header, options, footer, true);
    }

    private static void runApp(final Supplier<RunRightFastVertxApplication> app) {
        final RunRightFastVertxApplicationLauncher service = RunRightFastVertxApplicationLauncher.builder().app(app.get()).build();
        log.logp(INFO, CLASS_NAME, "runApp", "starting");
        service.startAsync();
        service.awaitRunning();
        log.logp(INFO, CLASS_NAME, "runApp", "running");
        service.awaitTerminated();
        log.logp(INFO, CLASS_NAME, "runApp", "terminated");
    }

    private static Options cliOptions() {
        final Options options = new Options()
                .addOptionGroup(new OptionGroup()
                        .addOption(Option.builder("v").longOpt("version").desc("Show the application version").build())
                        .addOption(Option.builder("h").longOpt("help").desc("Print usage").build())
                        .addOption(Option.builder("c").longOpt("config").desc("Show the application configuration as JSON").build())
                );
        return options;
    }

    @Override
    protected String serviceName() {
        return getClass().getSimpleName();
    }

    @Override
    protected void startUp() throws Exception {
        final VertxService vertxService = app.vertxService();
        vertxService.awaitRunning();
        vertxService.addListener(vertxServiceShutdownListener, MoreExecutors.directExecutor());
        registerShutdownHook();
        registerApplicationMBean();
        // In order for Dagger to create the service, it must be used first
        app.healthChecksService();
        logConfig();
    }

    @Override
    protected void shutDown() throws Exception {
        final VertxService vertxService = app.vertxService();
        ServiceUtils.stop(vertxService);
        unregisterApplicationMBean();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    log.logp(INFO, CLASS_NAME, "shutdownHook", "invoked");
                    ServiceUtils.stop(RunRightFastVertxApplicationLauncher.this);
                    log.logp(INFO, CLASS_NAME, "shutdownHook", "complete");
                } catch (final Throwable ex) {
                    log.logp(SEVERE, CLASS_NAME, "shutdownHook", "exception", ex);
                }
            }
        });
    }

    private void registerApplicationMBean() {
        JmxUtils.registerApplicationMBean(
                app.runRightFastApplication().getJmxDefaultDomain(),
                new ApplicationMXBeanImpl(this),
                ApplicationMXBean.class
        );
    }

    private void unregisterApplicationMBean() {
        JmxUtils.unregisterApplicationMBean(app.runRightFastApplication().getJmxDefaultDomain(), ApplicationMXBean.class);
    }

    private void logConfig() {
        final Config config = app.runRightFastApplication().getConfig();
        if (ConfigUtils.getBoolean(config, ConfigUtils.configPath(CONFIG_NAMESPACE, "logConfigOnStartup")).orElse(Boolean.TRUE)) {
            log.logp(INFO, CLASS_NAME, "logConfig", ConfigUtils.renderConfig(config));
        }
    }
}
