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
package co.runrightfast.core.application.event.impl;

import co.runrightfast.core.application.event.AppEvent;
import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog;
import static co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog.newErrorLog;
import static co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog.newInfoLog;
import static co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog.newWarningLog;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 */
@Log
@RequiredArgsConstructor
public final class AppEventJDKLogger implements AppEventLogger {

    @NonNull
    private final ApplicationId applicationId;

    private static final JsonLog info = newInfoLog(log, AppEventJDKLogger.class.getName());
    private static final JsonLog warning = newWarningLog(log, AppEventJDKLogger.class.getName());
    private static final JsonLog error = newErrorLog(log, AppEventJDKLogger.class.getName());

    private static final String METHOD = "accept";

    @Override
    public void accept(@NonNull final AppEvent event) {
        switch (event.getEventLevel()) {
            case INFO:
            case CLEAR:
                info.log(METHOD, () -> build(event));
                return;
            case WARN:
                warning.log(METHOD, () -> build(event));
                return;
            default:
                error.log(METHOD, () -> build(event));
        }

    }

    private JsonObject build(final AppEvent event) {
        final JsonObjectBuilder json = Json.createObjectBuilder().add("appId", applicationId.toJson());
        event.addEventInfo(json);
        return json.build();
    }

}
