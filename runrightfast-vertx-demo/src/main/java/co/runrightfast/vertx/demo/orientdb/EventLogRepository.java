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
package co.runrightfast.vertx.demo.orientdb;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.RunRightFastVerticleId.RUNRIGHTFAST_GROUP;
import co.runrightfast.vertx.core.eventbus.EventBusAddressMessageMapping;
import co.runrightfast.vertx.core.eventbus.MessageConsumerConfig;
import static co.runrightfast.vertx.core.eventbus.MessageConsumerConfig.ExecutionMode.WORKER_POOL_PARALLEL;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import co.runrightfast.vertx.orientdb.classes.demo.EventLogRecord;
import co.runrightfast.vertx.orientdb.classes.Timestamped;
import co.runrightfast.vertx.orientdb.verticle.OrientDBRepositoryVerticle;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.CreateEvent;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.Event;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.GetEventCount;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.GetEvents;
import demo.co.runrightfast.vertx.orientdb.verticle.eventLogRepository.messages.RecordId;
import io.vertx.core.eventbus.Message;
import java.util.List;
import java.util.Set;
import javax.json.Json;
import lombok.Getter;
import static org.apache.commons.lang.StringUtils.isNotBlank;

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
        //initDatabase();
        registerGetEventCountMessageConsumer();
        registerCreateEventCountMessageConsumer();
        registerGetEventsMessageConsumer();
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
                .ciphers(cipherFunctions(GetEventCount.getDefaultInstance()))
                .executionMode(WORKER_POOL_PARALLEL)
                .build();
        registerMessageConsumer(config);
    }

    private void handleGetEventCount(final Message<GetEventCount.Request> msg) {
        try (final ODatabaseDocumentTx db = orientDBService.getODatabaseDocumentTxSupplier(DB).get().get()) {
            final long count = db.countClass(EventLogRecord.class.getSimpleName());
            final GetEventCount.Response response = GetEventCount.Response.newBuilder().setCount(count).build();
            reply(msg, response);
        }
    }

    private void registerCreateEventCountMessageConsumer() {
        final MessageConsumerConfig<CreateEvent.Request, CreateEvent.Response> config = MessageConsumerConfig.<CreateEvent.Request, CreateEvent.Response>builder()
                .addressMessageMapping(EventBusAddressMessageMapping.builder()
                        .address(eventBusAddress(CreateEvent.class))
                        .requestDefaultInstance(CreateEvent.Request.getDefaultInstance())
                        .responseDefaultInstance(CreateEvent.Response.getDefaultInstance())
                        .build()
                )
                .handler(this::handleCreateEvent)
                .addExceptionFailureMapping(IllegalArgumentException.class, MessageConsumerConfig.Failure.BAD_REQUEST)
                .ciphers(cipherFunctions(CreateEvent.getDefaultInstance()))
                .executionMode(WORKER_POOL_PARALLEL)
                .build();
        registerMessageConsumer(config);
    }

    private void handleCreateEvent(final Message<CreateEvent.Request> msg) {
        final CreateEvent.Request request = msg.body();
        checkArgument(isNotBlank(request.getEvent()), MUST_NOT_BE_BLANK, "event");
        try (final ODatabaseDocumentTx db = orientDBService.getODatabaseDocumentTxSupplier(DB).get().get()) {
            final EventLogRecord eventLogRecord = new EventLogRecord();
            try {
                db.begin();
                eventLogRecord.setEvent(request.getEvent());
                eventLogRecord.save();
                db.commit();

                final CreateEvent.Response response = CreateEvent.Response.newBuilder()
                        .setId(RecordId.newBuilder())
                        .build();
                reply(msg, response);
            } catch (final Exception e) {
                db.rollback();
                throw e;
            }
        }
    }

    private void registerGetEventsMessageConsumer() {
        final MessageConsumerConfig<GetEvents.Request, GetEvents.Response> config = MessageConsumerConfig.<GetEvents.Request, GetEvents.Response>builder()
                .addressMessageMapping(EventBusAddressMessageMapping.builder()
                        .address(eventBusAddress(GetEvents.class))
                        .requestDefaultInstance(GetEvents.Request.getDefaultInstance())
                        .responseDefaultInstance(GetEvents.Response.getDefaultInstance())
                        .build()
                )
                .handler(this::handleGetEvents)
                .addExceptionFailureMapping(IllegalArgumentException.class, MessageConsumerConfig.Failure.BAD_REQUEST)
                .ciphers(cipherFunctions(GetEvents.getDefaultInstance()))
                .executionMode(WORKER_POOL_PARALLEL)
                .build();
        registerMessageConsumer(config);
    }

    private void handleGetEvents(final Message<GetEvents.Request> msg) {
        final GetEvents.Request request = msg.body();
        final int skip = request.getSkip() <= 0 ? 0 : request.getSkip();
        final int limit = request.getLimit() <= 0 ? 10 : request.getLimit();
        final GetEvents.Response.Builder response = GetEvents.Response.newBuilder();
        try (final ODatabaseDocumentTx db = orientDBService.getODatabaseDocumentTxSupplier(DB).get().get()) {
            final EventLogRecord eventLogRecord = new EventLogRecord();
            final List<ODocument> docs = db.query(new OSQLSynchQuery<ODocument>("select from EventLogRecord SKIP ? LIMIT ?"), skip, limit);
            docs.stream().forEach(doc -> {
                final EventLogRecord record = new EventLogRecord(doc);
                final ORID orid = record.getDocument().getRecord().getIdentity();
                final RecordId recordId = RecordId.newBuilder().setClusterId(orid.getClusterId()).setPosition(orid.getClusterPosition()).build();
                response.addEvents(Event.newBuilder()
                        .setCreatedOn(record.getCreatedOn() != null ? record.getCreatedOn().getTime() : 0)
                        .setUpdatedOn(record.getUpdatedOn() != null ? record.getUpdatedOn().getTime() : 0)
                        .setEvent(record.getEvent())
                        .setRecordId(recordId)
                        .build()
                );
            });
            reply(msg, response.build());
        }
    }

}
