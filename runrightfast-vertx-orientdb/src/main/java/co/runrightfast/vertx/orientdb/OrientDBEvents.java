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

import co.runrightfast.core.JsonRepresentation;
import static co.runrightfast.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import static com.google.common.base.Preconditions.checkArgument;
import javax.json.Json;
import javax.json.JsonObject;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public interface OrientDBEvents {

    public static final String ODATABASE_CREATE = "orientdb.db.create";
    public static final String ODATABASE_OPEN = "orientdb.db.open";
    public static final String ODATABASE_CLOSE = "orientdb.db.close";
    public static final String ODATABASE_DROP = "orientdb.db.drop";
    public static final String ODATABASE_CREATE_CLASS = "orientdb.db.createClass";
    public static final String ODATABASE_DROP_CLASS = "orientdb.db.dropClass";

    public static final class ODatabaseLifecycleEvent implements JsonRepresentation {

        private final String db;

        public ODatabaseLifecycleEvent(final String db) {
            checkArgument(isNotBlank(db), MUST_NOT_BE_BLANK, "db");
            this.db = db;
        }

        @Override
        public JsonObject toJson() {
            return Json.createObjectBuilder()
                    .add("db", db)
                    .build();
        }

    }

    public static final class ODatabaseLifecycleClassEvent implements JsonRepresentation {

        private final String db;

        private final String className;

        public ODatabaseLifecycleClassEvent(final String db, final String className) {
            checkArgument(isNotBlank(db), MUST_NOT_BE_BLANK, "db");
            checkArgument(isNotBlank(className), MUST_NOT_BE_BLANK, "className");
            this.db = db;
            this.className = className;
        }

        @Override
        public JsonObject toJson() {
            return Json.createObjectBuilder()
                    .add("db", db)
                    .add("class", className)
                    .build();
        }

    }

}
