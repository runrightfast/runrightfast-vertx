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
package co.runrightfast.vertx.orientdb.impl.embedded;

import co.runrightfast.vertx.orientdb.config.OGraphServerHandlerConfig;
import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.vertx.core.application.ApplicationId;
import static co.runrightfast.vertx.core.utils.JvmProcess.HOST;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import co.runrightfast.vertx.orientdb.OrientDBConstants;
import co.runrightfast.vertx.orientdb.OrientDBPoolConfig;
import co.runrightfast.vertx.orientdb.classes.Timestamped;
import co.runrightfast.vertx.orientdb.hooks.SetCreatedOnAndUpdatedOn;
import static co.runrightfast.vertx.orientdb.impl.embedded.EmbeddedOrientDBServiceWithSSLWithClientAuthTest.orientdbHome;
import co.runrightfast.vertx.orientdb.lifecycle.RunRightFastOrientDBLifeCycleListener;
import co.runrightfast.vertx.orientdb.utils.OrientDBUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerSocketFactoryConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import com.orientechnologies.orient.server.handler.OServerSideScriptInterpreter;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import com.orientechnologies.orient.server.network.OServerSSLSocketFactory;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_CLIENT_AUTH;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_KEYSTORE;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_KEYSTORE_PASSWORD;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_TRUSTSTORE;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_TRUSTSTORE_PASSWORD;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import static java.util.logging.Level.INFO;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.co.runrightfast.vertx.orientdb.classes.EventLogRecord;

/**
 *
 * @author alfio
 */
@Log
public class EmbeddedOrientDBServiceWithSSLWithClientAuthTest {

    static final String CLASS_NAME = EmbeddedOrientDBServiceWithSSLWithClientAuthTest.class.getSimpleName();

    private static EmbeddedOrientDBService service;

    static final File orientdbHome = new File(String.format("build/temp/%s/orientdb", CLASS_NAME));

    @BeforeClass
    public static void setUpClass() throws Exception {
        orientdbHome.mkdirs();
        FileUtils.cleanDirectory(orientdbHome);
        FileUtils.deleteDirectory(orientdbHome);
        log.logp(INFO, CLASS_NAME, "setUpClass", String.format("orientdbHome.exists() = %s", orientdbHome.exists()));

        final File configDirSrc = new File("src/test/resources/orientdb/config");
        final File configDirTarget = new File(orientdbHome, "config");
        FileUtils.copyFileToDirectory(new File(configDirSrc, "default-distributed-db-config.json"), configDirTarget);
        FileUtils.copyFileToDirectory(new File(configDirSrc, "hazelcast.xml"), configDirTarget);

        final File configCertDirSrc = new File("src/test/resources/orientdb/config/cert");
        final File configCertDirTarget = new File(orientdbHome, "config/cert");
        FileUtils.copyDirectory(configCertDirSrc, configCertDirTarget);

        final ApplicationId appId = ApplicationId.builder().group("co.runrightfast").name("runrightfast-vertx-orientdb").version("1.0.0").build();
        final AppEventLogger appEventLogger = new AppEventJDKLogger(appId);

        setSSLSystemProperties();
        final EmbeddedOrientDBServiceConfig config = EmbeddedOrientDBServiceConfig.builder()
                .orientDBRootDir(orientdbHome.toPath())
                .handler(new OGraphServerHandlerConfig(false))
                .handler(EmbeddedOrientDBServiceWithSSLWithClientAuthTest::oHazelcastPlugin)
                .handler(EmbeddedOrientDBServiceWithSSLWithClientAuthTest::oServerSideScriptInterpreter)
                .networkConfig(oServerNetworkConfiguration())
                .user(new OServerUserConfiguration("root", "root", "*"))
                .property(OGlobalConfiguration.DB_POOL_MIN, "1")
                .property(OGlobalConfiguration.DB_POOL_MAX, "50")
                .globalConfigProperty(OrientDBConstants.GlobalConfigKey.SECURITY_USER_PASSWORD_SALT_CACHE_SIZE, "0")
                .databasePoolConfig(new OrientDBPoolConfig(CLASS_NAME, "remote:localhost/" + CLASS_NAME, "admin", "admin", 10, ImmutableSet.of(() -> new SetCreatedOnAndUpdatedOn())))
                .lifecycleListener(() -> new RunRightFastOrientDBLifeCycleListener(appEventLogger))
                .build();

        service = new EmbeddedOrientDBService(config);
        ServiceUtils.start(service);
        initDatabase();
    }

    @AfterClass
    public static void tearDownClass() {
        ServiceUtils.stop(service);
    }

    private static OServerNetworkConfiguration oServerNetworkConfiguration() {
        final OServerNetworkConfiguration network = new OServerNetworkConfiguration();

        final OServerSocketFactoryConfiguration sslConfig = new OServerSocketFactoryConfiguration("ssl", OServerSSLSocketFactory.class.getName());
        sslConfig.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration(PARAM_NETWORK_SSL_KEYSTORE, serverKeyStorePath().toString()),
            new OServerParameterConfiguration(PARAM_NETWORK_SSL_KEYSTORE_PASSWORD, serverKeyStorePassword()),
            // client auth config
            new OServerParameterConfiguration(PARAM_NETWORK_SSL_CLIENT_AUTH, "true"),
            new OServerParameterConfiguration(PARAM_NETWORK_SSL_TRUSTSTORE, serverTrustStorePath().toString()),
            new OServerParameterConfiguration(PARAM_NETWORK_SSL_TRUSTSTORE_PASSWORD, serverTrustStorePassword())

        };
        network.sockets = ImmutableList.of(sslConfig);

        network.protocols = ImmutableList.of(
                new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName())
        );

        final OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = "0.0.0.0";
        binaryListener.protocol = "binary";
        binaryListener.portRange = "2434-2440";
        binaryListener.socket = sslConfig.name;
        network.listeners = ImmutableList.of(binaryListener);

        return network;
    }

    private static Path serverKeyStorePath() {
        return Paths.get(orientdbHome.getAbsolutePath(), "config", "cert", "orientdb.ks");
    }

    private static Path serverTrustStorePath() {
        return Paths.get(orientdbHome.getAbsolutePath(), "config", "cert", "orientdb.ts");
    }

    private static String serverKeyStorePassword() {
        return "qwerty90";
    }

    private static String serverTrustStorePassword() {
        return "qwerty90";
    }

    private static Path clientKeyStorePath() {
        return Paths.get(orientdbHome.getAbsolutePath(), "config", "cert", "orientdb-client.ks");
    }

    private static Path clientTrustStorePath() {
        return Paths.get(orientdbHome.getAbsolutePath(), "config", "cert", "orientdb-client.ks");
    }

    private static String clientKeyStorePassword() {
        return "qwerty90";
    }

    private static String clientTrustStorePassword() {
        return "qwerty90";
    }

    /**
     * The safest to do is to set the system properties before any OrientDB config classes are loaded.
     */
    private static void setSSLSystemProperties() {
        System.setProperty("client.ssl.enabled", "true");
        System.setProperty("client.ssl.keyStore", clientKeyStorePath().toString());
        System.setProperty("client.ssl.keyStorePass", clientKeyStorePassword());
        System.setProperty("client.ssl.trustStore", clientTrustStorePath().toString());
        System.setProperty("client.ssl.trustStorePass", clientTrustStorePassword());
    }

    private static void initDatabase() {
        final OServerAdmin admin = service.getServerAdmin();
        try {
            OrientDBUtils.createDatabase(admin, CLASS_NAME);
        } finally {
            admin.close();
        }

        final Optional<ODatabaseDocumentTxSupplier> dbSupplier = service.getODatabaseDocumentTxSupplier(CLASS_NAME);
        assertThat(dbSupplier.isPresent(), is(true));

        try (final ODatabase db = dbSupplier.get().get()) {
            final OClass timestampedClass = db.getMetadata().getSchema().createAbstractClass(Timestamped.class.getSimpleName());
            timestampedClass.createProperty(Timestamped.Field.created_on.name(), OType.DATETIME);
            timestampedClass.createProperty(Timestamped.Field.updated_on.name(), OType.DATETIME);

            final OClass logRecordClass = db.getMetadata().getSchema().createClass(EventLogRecord.class.getSimpleName()).setSuperClasses(ImmutableList.of(timestampedClass));
            logRecordClass.createProperty(EventLogRecord.Field.event.name(), OType.STRING);
        }
    }

    private static OServerHandlerConfiguration oHazelcastPlugin() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OHazelcastPlugin.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("nodeName", HOST),
            new OServerParameterConfiguration("configuration.db.default", new File(orientdbHome, "config/default-distributed-db-config.json").getAbsolutePath()),
            new OServerParameterConfiguration("configuration.hazelcast", new File(orientdbHome, "config/hazelcast.xml").getAbsolutePath())
        };
        return config;
    }

    private static OServerHandlerConfiguration oServerSideScriptInterpreter() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OServerSideScriptInterpreter.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("allowedLanguages", "SQL")
        };
        return config;
    }

    /**
     * Test of getDatabaseNames method, of class EmbeddedOrientDBService.
     */
    @Test
    public void testGetDatabaseNames() {
        assertThat(service.getDatabaseNames().contains(CLASS_NAME), is(true));

    }

    /**
     * Test of getODatabaseDocumentTxSupplier method, of class EmbeddedOrientDBService.
     */
    @Test
    public void testGetODatabaseDocumentTxSupplier() throws Exception {
        try (final ODatabaseDocumentTx db = service.getODatabaseDocumentTxSupplier(CLASS_NAME).get().get()) {
            testSavingEventLogRecord(db, "testGetODatabaseDocumentTxSupplier");
        }
    }

    private void testSavingEventLogRecord(final ODatabase db, final String testName) throws Exception {
        db.registerHook(new SetCreatedOnAndUpdatedOn());

        final EventLogRecord eventLogRecord = new EventLogRecord();
        try {
            db.begin();
            eventLogRecord.setEvent("app.started");
            eventLogRecord.save();
            db.commit();
        } catch (final Exception e) {
            db.rollback();
            throw e;
        }

        log.logp(INFO, CLASS_NAME, testName, String.format("doc = %s", eventLogRecord.toJSON()));
        assertThat(eventLogRecord.getCreatedOn(), is(notNullValue()));
        assertThat(eventLogRecord.getUpdatedOn(), is(notNullValue()));
    }

}
