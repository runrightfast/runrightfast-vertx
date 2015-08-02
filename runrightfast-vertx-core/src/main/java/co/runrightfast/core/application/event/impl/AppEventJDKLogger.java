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
import static java.util.logging.Level.INFO;
import javax.json.Json;
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

    private static final String CLASS_NAME = AppEventJDKLogger.class.getName();

    @Override
    public void accept(@NonNull final AppEvent event) {
        final JsonObjectBuilder json = Json.createObjectBuilder().add("appId", applicationId.toJson());
        event.addEventInfo(json);
        log.logp(INFO, CLASS_NAME, "accept", json.build().toString());
    }

}
