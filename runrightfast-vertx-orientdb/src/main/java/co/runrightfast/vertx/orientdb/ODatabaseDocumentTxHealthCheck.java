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

import co.runrightfast.vertx.orientdb.classes.DocumentObject;
import com.codahale.metrics.health.HealthCheck;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import org.apache.commons.collections4.CollectionUtils;

/**
 *
 * @author alfio
 */
@Builder
public class ODatabaseDocumentTxHealthCheck extends HealthCheck {

    @NonNull
    private final ODatabaseDocumentTxSupplier oDatabaseDocumentTxSupplier;

    @NonNull
    @Singular
    private final Set<Class<? extends DocumentObject>> documentObjects;

    @Override
    protected Result check() throws Exception {
        try (final ODatabaseDocumentTx db = oDatabaseDocumentTxSupplier.get()) {
            final JsonObjectBuilder msg = Json.createObjectBuilder().add("db", getDatabaseInfo(db));
            if (CollectionUtils.isNotEmpty(documentObjects)) {
                final JsonArrayBuilder classes = Json.createArrayBuilder();
                documentObjects.stream().forEach(documentObject -> {
                    final ORecordIteratorClass<ODocument> it = db.browseClass(documentObject.getSimpleName());
                    if (it.hasNext()) {
                        it.next();
                        classes.add(Json.createObjectBuilder().add(documentObject.getSimpleName(), 1));
                    } else {
                        classes.add(Json.createObjectBuilder().add(documentObject.getSimpleName(), 0));
                    }
                });
                msg.add("clases", classes);
            }
            return HealthCheck.Result.healthy(msg.build().toString());
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
