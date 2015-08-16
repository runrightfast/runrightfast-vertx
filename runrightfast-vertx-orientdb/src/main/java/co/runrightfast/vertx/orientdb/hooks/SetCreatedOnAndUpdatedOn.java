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

import static co.runrightfast.vertx.orientdb.StandardClass.TIMESTAMPED_RECORD;
import static co.runrightfast.vertx.orientdb.StandardField.CREATED_ON;
import static co.runrightfast.vertx.orientdb.StandardField.UPDATED_ON;
import com.orientechnologies.orient.core.hook.ODocumentHookAbstract;
import static com.orientechnologies.orient.core.hook.ORecordHook.DISTRIBUTED_EXECUTION_MODE.BOTH;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 *
 * @author alfio
 */
public class SetCreatedOnAndUpdatedOn extends ODocumentHookAbstract {

    private static final String CLASS = SetCreatedOnAndUpdatedOn.class.getName();

    @Override
    public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
        return BOTH;
    }

    @Override
    public RESULT onRecordBeforeUpdate(final ODocument doc) {
        if (doc.getSchemaClass() != null && doc.getSchemaClass().isSubClassOf(TIMESTAMPED_RECORD.className)) {
            doc.field(UPDATED_ON.field, System.currentTimeMillis());
            return RESULT.RECORD_CHANGED;
        }
        return RESULT.RECORD_NOT_CHANGED;
    }

    @Override
    public RESULT onRecordBeforeCreate(final ODocument doc) {
        if (doc.getSchemaClass() != null && doc.getSchemaClass().isSubClassOf(TIMESTAMPED_RECORD.className)) {
            final long createdOn = System.currentTimeMillis();
            doc.field(CREATED_ON.field, createdOn);
            doc.field(UPDATED_ON.field, createdOn);
            return RESULT.RECORD_CHANGED;
        }
        return RESULT.RECORD_NOT_CHANGED;
    }

}
