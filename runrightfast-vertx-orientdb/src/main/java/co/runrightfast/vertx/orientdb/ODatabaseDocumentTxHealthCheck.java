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
package co.runrightfast.vertx.orientdb;

import co.runrightfast.core.ApplicationException;
import co.runrightfast.vertx.orientdb.classes.DocumentObject;
import com.codahale.metrics.health.HealthCheck;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Queries each of the specified classes.
 *
 * The message that is returned is a Json message that indicates if there was at least 1 instance for the class found.
 *
 * for example:  <code>
 * { "db" : "logging",
 *   "classes" : [
 *       { "event" : 1 },
 *       { "alert" : 0 }
 * }
 * </code>
 *
 * where 1 means at least 1 instance was found, and 0 means the class has no instances
 *
 * @author alfio
 */
@Builder
public class ODatabaseDocumentTxHealthCheck extends HealthCheck {

    public static final class ODatabaseDocumentTxHealthCheckException extends ApplicationException {

        private static final long serialVersionUID = 1L;

        public ODatabaseDocumentTxHealthCheckException(final String message, final Throwable cause) {
            super(message, cause);
        }

    }

    @NonNull
    private final String databaseName;

    @NonNull
    private final ODatabaseDocumentTxSupplier oDatabaseDocumentTxSupplier;

    @NonNull
    @Singular
    private final Set<Class<? extends DocumentObject>> documentObjects;

    @Override
    protected Result check() throws Exception {
        try (final ODatabaseDocumentTx db = oDatabaseDocumentTxSupplier.get()) {
            final JsonObjectBuilder msgBuilder = Json.createObjectBuilder().add("db", getDatabaseInfo(db));
            if (CollectionUtils.isNotEmpty(documentObjects)) {
                final JsonObjectBuilder counts = Json.createObjectBuilder();
                documentObjects.stream().forEach(documentObject -> browseClass(db, documentObject, counts));
                msgBuilder.add("counts", counts);
            }

            final JsonObject msg = msgBuilder.build();
            if (isHealthy(msg)) {
                return HealthCheck.Result.healthy(msg.toString());
            }
            return HealthCheck.Result.unhealthy(msg.toString());
        } catch (final Exception e) {
            final JsonObjectBuilder msg = Json.createObjectBuilder().add("db", Json.createObjectBuilder().add("name", databaseName).build());
            return HealthCheck.Result.unhealthy(new ODatabaseDocumentTxHealthCheckException(msg.build().toString(), e));
        }
    }

    private boolean isHealthy(final JsonObject healthcheckData) {
        if (documentObjects.isEmpty()) {
            return true;
        }

        final JsonObject counts = healthcheckData.getJsonObject("counts");
        return !documentObjects.stream()
                .filter(clazz -> counts.getInt(DocumentObject.documentClassName(clazz)) == -1)
                .findFirst()
                .isPresent();
    }

    private void browseClass(final ODatabaseDocumentTx db, final Class<? extends DocumentObject> documentObject, final JsonObjectBuilder counts) {
        final String documentClassName = DocumentObject.documentClassName(documentObject);
        if (db.getMetadata().getSchema().getClass(documentClassName) == null) {
            counts.add(documentClassName, -1);
            return;
        }

        final ORecordIteratorClass<ODocument> it = db.browseClass(documentClassName);
        if (it.hasNext()) {
            it.next();
            counts.add(documentClassName, 1);
        } else {
            counts.add(documentClassName, 0);
        }
    }

    private JsonObject getDatabaseInfo(final ODatabase db) {
        return Json.createObjectBuilder()
                .add("name", db.getName())
                .add("type", db.getType())
                .add("url", db.getURL())
                .build();

    }

}
