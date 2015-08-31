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
package co.runrightfast.vertx.demo.modules;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.demo.orientdb.EventLogRepository;
import co.runrightfast.vertx.orientdb.OrientDBConfig;
import co.runrightfast.vertx.orientdb.classes.EventLogRecord;
import co.runrightfast.vertx.orientdb.hooks.SetCreatedOnAndUpdatedOn;
import co.runrightfast.vertx.orientdb.impl.DatabasePoolConfig;
import co.runrightfast.vertx.orientdb.impl.EmbeddedOrientDBServiceConfig;
import co.runrightfast.vertx.orientdb.lifecycle.RunRightFastOrientDBLifeCycleListener;
import co.runrightfast.vertx.orientdb.plugins.OrientDBPluginWithProvidedHazelcastInstance;
import co.runrightfast.vertx.orientdb.verticle.OrientDBRepositoryVerticleDeployment;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.graph.handler.OGraphServerHandler;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import com.orientechnologies.orient.server.handler.OServerSideScriptInterpreter;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.DeploymentOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.util.logging.Level.INFO;
import javax.inject.Singleton;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author alfio
 */
@Module
@Log
public class OrientDBModule {

    @Provides(type = Provides.Type.SET)
    @Singleton
    public OrientDBRepositoryVerticleDeployment provideEventLogRepositoryDeployment(final AppEventLogger logger, final EncryptionService encryptionService) {
        return new OrientDBRepositoryVerticleDeployment(
                () -> new EventLogRepository(logger, encryptionService),
                EventLogRepository.class,
                new DeploymentOptions()
        );
    }

    @Provides
    @Singleton
    public EmbeddedOrientDBServiceConfig providesEmbeddedOrientDBServiceConfig(final OrientDBConfig orientDBConfig) {
        final File orientdbHome = orientDBConfig.getHomeDirectory().toFile();
        orientdbHome.mkdirs();
//        try {
//            FileUtils.cleanDirectory(orientdbHome);
//            FileUtils.deleteDirectory(orientdbHome);
//        } catch (final IOException e) {
//            throw new RuntimeException(e);
//        }
        log.logp(INFO, getClass().getName(), "providesEmbeddedOrientDBServiceConfig", String.format("orientdbHome.exists() = %s", orientdbHome.exists()));

//        final File configDirSrc = new File("src/main/resources/orientdb/config");
//        final File configDirTarget = new File(orientdbHome, "config");
//        try {
//            FileUtils.copyFileToDirectory(new File(configDirSrc, "default-distributed-db-config.json"), configDirTarget);
//        } catch (final IOException e) {
//            throw new RuntimeException(e);
//        }
        final Path defaultDistributedDBConfigFile = Paths.get(orientDBConfig.getHomeDirectory().toAbsolutePath().toString(), "config", "default-distributed-db-config.json");
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

        final ApplicationId appId = ApplicationId.builder().group("co.runrightfast").name("runrightfast-vertx-orientdb").version("1.0.0").build();
        final AppEventLogger appEventLogger = new AppEventJDKLogger(appId);

        return EmbeddedOrientDBServiceConfig.builder()
                .orientDBRootDir(orientdbHome.toPath())
                .handler(this::oGraphServerHandler)
                .handler(() -> this.oHazelcastPlugin(orientdbHome, orientDBConfig))
                .handler(this::oServerSideScriptInterpreter)
                .networkConfig(oServerNetworkConfiguration())
                .user(new OServerUserConfiguration("root", "root", "*"))
                .property(OGlobalConfiguration.DB_POOL_MIN, "1")
                .property(OGlobalConfiguration.DB_POOL_MAX, "50")
                .databasePoolConfig(new DatabasePoolConfig(EventLogRepository.DB, "admin", "admin", 10, true, EventLogRecord.class))
                .lifecycleListener(() -> new RunRightFastOrientDBLifeCycleListener(appEventLogger))
                .hook(() -> new SetCreatedOnAndUpdatedOn())
                .build();
    }

    private OServerNetworkConfiguration oServerNetworkConfiguration() {
        final OServerNetworkConfiguration network = new OServerNetworkConfiguration();
        network.protocols = ImmutableList.<OServerNetworkProtocolConfiguration>builder()
                .add(new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName()))
                .build();
        final OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = "0.0.0.0";
        binaryListener.protocol = "binary";
        binaryListener.portRange = "2424-2430";
        binaryListener.socket = "default";
        network.listeners = ImmutableList.<OServerNetworkListenerConfiguration>builder()
                .add(binaryListener)
                .build();
        return network;
    }

    private OServerHandlerConfiguration oGraphServerHandler() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OGraphServerHandler.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("graph.pool.max", "50")
        };
        return config;
    }

    private OServerHandlerConfiguration oHazelcastPlugin(final File orientdbHome, final OrientDBConfig orientDBConfig) {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OrientDBPluginWithProvidedHazelcastInstance.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("nodeName", orientDBConfig.getNodeName()),
            new OServerParameterConfiguration("configuration.db.default", new File(orientdbHome, "config/default-distributed-db-config.json").getAbsolutePath()),};
        return config;
    }

    private OServerHandlerConfiguration oServerSideScriptInterpreter() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OServerSideScriptInterpreter.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("allowedLanguages", "SQL")
        };
        return config;
    }

}
