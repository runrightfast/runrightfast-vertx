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
package co.runrightfast.vertx.orientdb.classes;

import static co.runrightfast.vertx.orientdb.classes.EventLogRecord.Field.event;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Date;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public class EventLogRecord extends Timestamped {

    public static enum Field {

        event
    }

    public EventLogRecord newEventLogRecord() {
        final EventLogRecord record = new EventLogRecord();
        final Date now = new Date();
        record.getDocument().field(Timestamped.Field.created_on.name(), now);
        record.getDocument().field(Timestamped.Field.updated_on.name(), now);
        return record;
    }

    public EventLogRecord() {
        super();
    }

    public EventLogRecord(final ODocument doc) {
        super(doc);
    }

    public String getEvent() {
        return document.field(event.name());
    }

    public EventLogRecord setEvent(final String event) {
        checkArgument(isNotBlank(event));
        document.field(Field.event.name(), event);
        return this;
    }

}
