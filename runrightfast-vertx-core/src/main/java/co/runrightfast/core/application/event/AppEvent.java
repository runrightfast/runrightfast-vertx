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
package co.runrightfast.core.application.event;

import co.runrightfast.core.JsonRepresentation;
import static co.runrightfast.core.application.event.AppEvent.AppEventLevel.ALERT;
import static co.runrightfast.core.application.event.AppEvent.AppEventLevel.ERROR;
import static co.runrightfast.core.application.event.AppEvent.AppEventLevel.INFO;
import static co.runrightfast.core.application.event.AppEvent.AppEventLevel.WARN;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.utils.JsonUtils;
import com.google.common.base.MoreObjects;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.commons.lang3.exception.ExceptionUtils;
import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

/**
 * The intent is to use this to serialize the events to JSON which can then be stored within elastic.
 *
 * @author alfio
 */
public final class AppEvent {

    public static final class Builder {

        private final AppEvent appEvent;

        private Builder(final String event, final AppEventLevel eventLevel) {
            this.appEvent = new AppEvent(event, eventLevel);
        }

        public Builder setMessage(final String message) {
            appEvent.message = message;
            return this;
        }

        public Builder setException(final Throwable exception) {
            appEvent.exception = exception;
            return this;
        }

        public Builder setTags(final String... tags) {
            if (ArrayUtils.isNotEmpty(tags)) {
                appEvent.tags = ImmutableList.copyOf(tags);
            }
            return this;
        }

        public Builder setData(@NonNull final JsonRepresentation data) {
            appEvent.data = data;
            return this;
        }

        public Builder setVerticleId(@NonNull final RunRightFastVerticleId verticleId) {
            appEvent.verticleId = verticleId;
            return this;
        }

        public AppEvent build() {
            return appEvent;
        }

    }

    public static Builder builder(final String event, final AppEventLevel eventLevel) {
        return new Builder(event, eventLevel);
    }

    public static Builder info(final String event) {
        return builder(event, INFO);
    }

    public static Builder warn(final String event) {
        return builder(event, WARN);
    }

    public static Builder error(final String event) {
        return builder(event, ERROR);
    }

    public static Builder alert(final String event) {
        return builder(event, ALERT);
    }

    public static enum AppEventLevel {

        INFO(100),
        WARN(200),
        ERROR(300),
        ALERT(400),
        CLEAR(500);

        public final int id;

        private AppEventLevel(final int id) {
            this.id = id;
        }

        public boolean isWarnErrorAlert() {
            return this != INFO;
        }

    }

    @Getter
    private final long timestampMillis;

    @Getter
    private final String event;

    @Getter
    private final AppEventLevel eventLevel;

    @Getter
    private String message;

    @Getter
    private Throwable exception;

    @Getter
    private List<String> tags = ImmutableList.of();

    @Getter
    private RunRightFastVerticleId verticleId;

    @Getter
    private JsonRepresentation data;

    private AppEvent(final String event, @NonNull final AppEventLevel eventLevel) {
        checkArgument(isNotBlank(event));
        this.timestampMillis = System.currentTimeMillis();
        this.event = event;
        this.eventLevel = eventLevel;
    }

    /**
     *
     * @param json add event info to the supplied JsonObjectBuilder
     */
    public void addEventInfo(@NonNull final JsonObjectBuilder json) {
        json
                .add("event", event)
                .add("level", eventLevel.id)
                .add("timestamp", getTimestamp());
        addMessage(json);
        addException(json);
        addTags(json);
        addData(json);
        addVerticleId(json);
    }

    private void addMessage(final JsonObjectBuilder json) {
        if (message != null) {
            json.add("msg", message);
        }
    }

    private void addException(final JsonObjectBuilder json) {
        if (exception != null) {
            final JsonObjectBuilder exBuilder = Json.createObjectBuilder()
                    .add("class", exception.getClass().getName())
                    .add("stacktrace", ExceptionUtils.getStackTrace(exception));
            final Throwable rootCause = ExceptionUtils.getRootCause(exception);
            if (rootCause != null) {
                exBuilder.add("rootCause", rootCause.getClass().getName());
            }
            json.add("exception", exBuilder);
        }
    }

    private void addTags(final JsonObjectBuilder json) {
        final JsonArray jsonArray = JsonUtils.toJsonArray(tags);
        if (!jsonArray.isEmpty()) {
            json.add("tags", jsonArray);
        }
    }

    private void addData(final JsonObjectBuilder json) {
        if (data == null) {
            return;
        }
        json.add("data", Json.createObjectBuilder().add(data.getType(), data.toJson()));
    }

    private void addVerticleId(final JsonObjectBuilder json) {
        if (verticleId != null) {
            json.add("verticleId", verticleId.toJson());
        }
    }

    public String getTimestamp() {
        return ISO_DATETIME_TIME_ZONE_FORMAT.format(timestampMillis);
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(this)
                .add("timestamp", getTimestamp())
                .add("event", event)
                .add("eventLevel", eventLevel);

        if (StringUtils.isNotBlank(message)) {
            toStringHelper.add("message", message);
        }

        if (CollectionUtils.isNotEmpty(tags)) {
            toStringHelper.add("tags", tags.stream().collect(Collectors.joining(",")));
        }

        if (data != null) {
            toStringHelper.add("data", data.getType());
        }

        if (exception != null) {
            toStringHelper.add("exception", ExceptionUtils.getStackTrace(exception));
        }

        return toStringHelper.toString();

    }

}
