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

import static co.runrightfast.vertx.orientdb.StandardField.CREATED_ON;
import static co.runrightfast.vertx.orientdb.StandardField.NAME;
import static co.runrightfast.vertx.orientdb.StandardField.UPDATED_ON;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author alfio
 */
public enum StandardClass {

    TIMESTAMPED_RECORD("Timestamped", CREATED_ON, UPDATED_ON),
    EVENT_LOG_RECORD("EventLog", NAME);

    public final String className;
    public final List<StandardField> fields;

    private StandardClass(final String className, final StandardField... fields) {
        this.className = className;
        if (ArrayUtils.isNotEmpty(fields)) {
            this.fields = ImmutableList.copyOf(fields);
        } else {
            this.fields = ImmutableList.of();
        }
    }
}
