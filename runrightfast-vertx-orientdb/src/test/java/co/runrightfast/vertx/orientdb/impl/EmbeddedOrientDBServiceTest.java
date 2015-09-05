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
package co.runrightfast.vertx.orientdb.impl;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.vertx.core.application.ApplicationId;
import static co.runrightfast.vertx.core.utils.JvmProcess.HOST;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.orientdb.DatabasePoolConfig;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import co.runrightfast.vertx.orientdb.classes.EventLogRecord;
import co.runrightfast.vertx.orientdb.classes.Timestamped;
import co.runrightfast.vertx.orientdb.hooks.SetCreatedOnAndUpdatedOn;
import co.runrightfast.vertx.orientdb.lifecycle.RunRightFastOrientDBLifeCycleListener;
import co.runrightfast.vertx.orientdb.utils.OrientDBUtils;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.graph.handler.OGraphServerHandler;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import com.orientechnologies.orient.server.handler.OServerSideScriptInterpreter;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import java.io.File;
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

/**
 *
 * @author alfio
 */
@Log
public class EmbeddedOrientDBServiceTest {

    static final String CLASS_NAME = EmbeddedOrientDBServiceTest.class.getSimpleName();

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

        final ApplicationId appId = ApplicationId.builder().group("co.runrightfast").name("runrightfast-vertx-orientdb").version("1.0.0").build();
        final AppEventLogger appEventLogger = new AppEventJDKLogger(appId);

        final EmbeddedOrientDBServiceConfig config = EmbeddedOrientDBServiceConfig.builder()
                .orientDBRootDir(orientdbHome.toPath())
                .handler(EmbeddedOrientDBServiceTest::oGraphServerHandler)
                .handler(EmbeddedOrientDBServiceTest::oHazelcastPlugin)
                .handler(EmbeddedOrientDBServiceTest::oServerSideScriptInterpreter)
                .networkConfig(oServerNetworkConfiguration())
                .user(new OServerUserConfiguration("root", "root", "*"))
                .property(OGlobalConfiguration.DB_POOL_MIN, "1")
                .property(OGlobalConfiguration.DB_POOL_MAX, "50")
                .databasePoolConfig(new DatabasePoolConfig(CLASS_NAME, "remote:localhost/" + CLASS_NAME, "admin", "admin", 10))
                .lifecycleListener(() -> new RunRightFastOrientDBLifeCycleListener(appEventLogger))
                .hook(() -> new SetCreatedOnAndUpdatedOn())
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

    private static OServerHandlerConfiguration oGraphServerHandler() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OGraphServerHandler.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("graph.pool.max", "50")
        };
        return config;
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
