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
import co.runrightfast.vertx.core.components.ApplicationInfo;
import co.runrightfast.vertx.core.components.DaggerApplicationInfo;
import co.runrightfast.vertx.core.components.RunRightFastVertxApplication;
import co.runrightfast.vertx.core.modules.ApplicationConfigModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import co.runrightfast.vertx.core.utils.ServiceUtils;
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

/**
 * The design pattern for an application is:
 *
 * <ol>
 * <li> Extend this class and implement the {@link #runRightFastVertxApplication() } abstract method
 * <li> Provide a main method that creates an instance of this class. For convenience, your main method can simply delegate to {@link RunRightFastVertxApplicationService#run(co.runrightfast.vertx.core.application.RunRightFastVertxApplicationService, java.lang.String[]) ) :
 * <code>
 *
 *  public final class MyApp extends RunRightFastVertxApplicationService {
 *      public static void main(final String[] args){
 *          System.exit(RunRightFastVertxApplicationService.run(MyApp::createRunRightFastVertxApplicationService, args));
 *      }
 *
 *      private static RunRightFastVertxApplicationService createRunRightFastVertxApplicationService(){
 *          ...
 *      }
 *  }
 * </code>
 * </ol>
 *
 *
 * @author alfio
 */
@Log
@Builder
public final class RunRightFastVertxApplicationService extends AbstractIdleService {

    private static final String CLASS_NAME = RunRightFastVertxApplicationService.class.getName();

    @Getter
    @NonNull
    private final RunRightFastVertxApplication app;

    private final Service.Listener vertxServiceShutdownListener = new Service.Listener() {

        @Override
        public void failed(final Service.State from, Throwable failure) {
            log.logp(SEVERE, CLASS_NAME, "shutdownListener", "failed", failure);
            ServiceUtils.stopAsync(RunRightFastVertxApplicationService.this);
        }

        @Override
        public void terminated(final Service.State from) {
            log.logp(INFO, CLASS_NAME, "shutdownListener", "terminated");
            ServiceUtils.stopAsync(RunRightFastVertxApplicationService.this);
        }

    };

    /**
     *
     *
     * @param app supplies an RunRightFastVertxApplication instance. The recommended approach is to supply a Dagger2 component which extends the
     * RunRightFastVertxApplication interface, which includes the following modules:
     *
     * <ol>
     * <li>{@link ApplicationConfigModule}
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
    public static int run(@NonNull final Supplier<RunRightFastVertxApplication> app, final String[] args) {
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

            runApp(app);
        } catch (final ParseException e) {
            System.out.println(e.getMessage());
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ant", options);
            return 2;
        } catch (final Throwable e) {
            log.logp(SEVERE, CLASS_NAME, "run", "unexpected exception", e);
            return 1;
        }
        return 0;
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
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ant", options);
    }

    private static void runApp(final Supplier<RunRightFastVertxApplication> app) {
        final RunRightFastVertxApplicationService service = RunRightFastVertxApplicationService.builder().app(app.get()).build();
        log.logp(INFO, CLASS_NAME, "run", "starting");
        service.startAsync();
        service.awaitRunning();
        log.logp(INFO, CLASS_NAME, "run", "running");
        service.awaitTerminated();
        log.logp(INFO, CLASS_NAME, "run", "terminated");
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
        logConfig(app);
        registerApplicationInstance();
    }

    @Override
    protected void shutDown() throws Exception {
        final VertxService vertxService = app.vertxService();
        ServiceUtils.stop(vertxService);
        unregisterApplicationInstance();
        unregisterApplicationMBean();
    }

    private void registerShutdownHook() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void registerApplicationMBean() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void unregisterApplicationMBean() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void logConfig(RunRightFastVertxApplication app) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void registerApplicationInstance() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void unregisterApplicationInstance() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
