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
package co.runrightfast.vertx.orientdb.verticle;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.RunRightFastVerticleId.RUNRIGHTFAST_GROUP;
import co.runrightfast.vertx.core.eventbus.EventBusAddressMessageMapping;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import static co.runrightfast.vertx.core.eventbus.MessageConsumerConfig.ExecutionMode.WORKER_POOL_PARALLEL;
import co.runrightfast.vertx.core.verticles.messages.Ping;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import co.runrightfast.vertx.orientdb.classes.EventLogRecord;
import co.runrightfast.vertx.orientdb.classes.Timestamped;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import io.vertx.core.eventbus.Message;
import java.util.Set;
import javax.json.Json;
import lombok.Getter;
import test.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.GetEventCount;

/**
 *
 * @author alfio
 */
public class EventLogRepository extends OrientDBRepositoryVerticle {

    public static final RunRightFastVerticleId VERTICLE_ID = RunRightFastVerticleId.builder()
            .group(RUNRIGHTFAST_GROUP)
            .name(EventLogRepository.class.getSimpleName())
            .version("0.1.0")
            .build();

    public static final String DB = EventLogRepository.class.getSimpleName();

    @Getter
    private final RunRightFastVerticleId runRightFastVerticleId = VERTICLE_ID;

    private ODatabaseDocumentTxSupplier dbSupplier;

    public EventLogRepository(final AppEventLogger appEventLogger, final EncryptionService encryptionService) {
        super(appEventLogger, encryptionService);
    }

    @Override
    public Set<RunRightFastHealthCheck> getHealthChecks() {
        return ImmutableSet.of();
    }

    @Override
    protected void startUp() {
        dbSupplier = orientDBService.getODatabaseDocumentTxSupplier(DB).get();
        initDatabase();
        registerGetEventCountMessageConsumer();
    }

    public void initDatabase() {
        try (final ODatabase db = dbSupplier.get()) {
            OClass timestampedClass = db.getMetadata().getSchema().getClass(Timestamped.class.getSimpleName());
            if (timestampedClass == null) {
                timestampedClass = db.getMetadata().getSchema().createAbstractClass(Timestamped.class.getSimpleName());
                timestampedClass.createProperty(Timestamped.Field.created_on.name(), OType.DATETIME);
                timestampedClass.createProperty(Timestamped.Field.updated_on.name(), OType.DATETIME);
                info.log("startUp", () -> Json.createObjectBuilder().add("class", Timestamped.class.getSimpleName()).add("created", true).build());
            } else {
                info.log("startUp", () -> Json.createObjectBuilder().add("class", Timestamped.class.getSimpleName()).build());
            }

            OClass logRecordClass = db.getMetadata().getSchema().getClass(EventLogRecord.class.getSimpleName());
            if (logRecordClass == null) {
                logRecordClass = db.getMetadata().getSchema().createClass(EventLogRecord.class.getSimpleName()).setSuperClasses(ImmutableList.of(timestampedClass));
                logRecordClass.createProperty(EventLogRecord.Field.event.name(), OType.STRING);
                info.log("startUp", () -> Json.createObjectBuilder().add("class", EventLogRecord.class.getSimpleName()).add("created", true).build());
            } else {
                info.log("startUp", () -> Json.createObjectBuilder().add("class", EventLogRecord.class.getSimpleName()).build());
            }

        }
    }

    @Override
    protected void shutDown() {
    }

    private void registerGetEventCountMessageConsumer() {
        final MessageConsumerConfig<GetEventCount.Request, GetEventCount.Response> config = MessageConsumerConfig.<GetEventCount.Request, GetEventCount.Response>builder()
                .addressMessageMapping(EventBusAddressMessageMapping.builder()
                        .address(eventBusAddress(GetEventCount.class))
                        .requestDefaultInstance(GetEventCount.Request.getDefaultInstance())
                        .responseDefaultInstance(GetEventCount.Response.getDefaultInstance())
                        .build()
                )
                .handler(this::handleGetEventCount)
                .addExceptionFailureMapping(IllegalArgumentException.class, MessageConsumerConfig.Failure.BAD_REQUEST)
                .ciphers(cipherFunctions(Ping.getDefaultInstance()))
                .executionMode(WORKER_POOL_PARALLEL)
                .build();
    }

    private void handleGetEventCount(final Message<GetEventCount.Request> msg) {
        try (final ODatabaseDocumentTx db = orientDBService.getODatabaseDocumentTxSupplier(DB).get().get()) {
            final long count = db.countClass(EventLogRecord.class.getSimpleName());
            final GetEventCount.Response response = GetEventCount.Response.newBuilder().setCount(count).build();
            msg.reply(response);
        }
    }

}