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
package co.runrightfast.vertx.orientdb.hooks;

import static co.runrightfast.vertx.orientdb.StandardClass.EVENT_LOG_RECORD;
import static co.runrightfast.vertx.orientdb.StandardClass.TIMESTAMPED_RECORD;
import static co.runrightfast.vertx.orientdb.StandardField.CREATED_ON;
import static co.runrightfast.vertx.orientdb.StandardField.NAME;
import static co.runrightfast.vertx.orientdb.StandardField.UPDATED_ON;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseFactory;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.graph.handler.OGraphServerHandler;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
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
public class RunRightFastOrientDBLifeCycleListenerTest {

    static final String CLASS_NAME = RunRightFastOrientDBLifeCycleListenerTest.class.getSimpleName();

    private static OServer server;
    static final File orientdbHome = new File("build/temp/orientdb");

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

        server = createOServer();
        server.activate();

        final String dbUrl = String.format("plocal:%s/databases/%s", orientdbHome.getAbsolutePath(), RunRightFastOrientDBLifeCycleListenerTest.class.getSimpleName());
        try (final ODatabase db = new ODatabaseFactory().createDatabase("document", dbUrl).create()) {
            log.logp(INFO, CLASS_NAME, "setUpClass", String.format("created db = %s", db.getName()));

            final OClass timestampedClass = db.getMetadata().getSchema().createAbstractClass(TIMESTAMPED_RECORD.className);
            timestampedClass.createProperty(CREATED_ON.field, OType.DATETIME);
            timestampedClass.createProperty(UPDATED_ON.field, OType.DATETIME);

            final OClass logRecordClass = db.getMetadata().getSchema().createClass(EVENT_LOG_RECORD.className).setSuperClasses(ImmutableList.of(timestampedClass));
            logRecordClass.createProperty(NAME.field, OType.STRING);
        }
    }

    private static OServer createOServer() throws Exception {
        server = OServerMain.create(true);
        server.setServerRootDirectory(orientdbHome.getAbsolutePath());
        final OServerConfiguration config = new OServerConfiguration();

        config.handlers = ImmutableList.<OServerHandlerConfiguration>builder()
                .add(oGraphServerHandler())
                .add(oHazelcastPlugin())
                .add(oServerSideScriptInterpreter())
                .build();

        config.network = new OServerNetworkConfiguration();
        config.network.protocols = ImmutableList.<OServerNetworkProtocolConfiguration>builder()
                .add(new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName()))
                .build();
        final OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = "0.0.0.0";
        binaryListener.protocol = "binary";
        binaryListener.portRange = "2424-2430";
        binaryListener.socket = "default";
        config.network.listeners = ImmutableList.<OServerNetworkListenerConfiguration>builder()
                .add(binaryListener)
                .build();

        config.users = new OServerUserConfiguration[]{
            new OServerUserConfiguration("root", "root", "*")
        };

        config.properties = new OServerEntryConfiguration[]{
            new OServerEntryConfiguration("db.pool.min", "1"),
            new OServerEntryConfiguration("db.pool.max", "50")
        };

        server.startup(config);
        return server;
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

    @AfterClass
    public static void tearDownClass() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testLifeCycleListener() throws Exception {
        try (final ODatabase db = server.openDatabase("document", "plocal:" + getClass().getSimpleName(), "root", "root")
                .registerHook(new SetCreatedOnAndUpdatedOn())
                .activateOnCurrentThread()) {
            testSavingEventLogRecord(db, "testLifeCycleListener");
        }

    }

    @Test
    public void testPooling() throws Exception {
        try (final ODatabase db = new OPartitionedDatabasePoolFactory().get("plocal:" + getClass().getSimpleName(), "writer", "writer").acquire()) {
            testSavingEventLogRecord(db, "testPooling");
        }
    }

    private void testSavingEventLogRecord(final ODatabase db, final String testName) {
        db.registerHook(new SetCreatedOnAndUpdatedOn()).activateOnCurrentThread();

        Orient.instance().addDbLifecycleListener(new RunRightFastOrientDBLifeCycleListener());

        final ODocument doc = new ODocument(EVENT_LOG_RECORD.className);
        try {
            db.begin();
            doc.field(NAME.field, "app.started");
            db.save(doc);
            db.commit();
        } catch (final Exception e) {
            db.rollback();
            throw e;
        }

        log.logp(INFO, CLASS_NAME, testName, String.format("doc = %s", doc.toJSON()));
        assertThat(doc.field(CREATED_ON.field), is(notNullValue()));
        assertThat(doc.field(UPDATED_ON.field), is(notNullValue()));

        final ODocument doc2 = (ODocument) db.load(doc);
        log.logp(INFO, CLASS_NAME, testName, String.format("doc2 = %s", doc2.toJSON()));
        assertThat(doc2.field(CREATED_ON.field), is(notNullValue()));
        assertThat(doc2.field(UPDATED_ON.field), is(notNullValue()));
    }

}
