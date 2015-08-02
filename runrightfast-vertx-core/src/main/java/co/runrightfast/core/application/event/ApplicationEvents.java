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
import com.codahale.metrics.health.HealthCheck;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 *
 * All application events and AppEvent.EventData classes should be defined here in order to make it easy to lookup events.
 *
 * Each module should do the same.
 *
 * @author alfio
 */
public interface ApplicationEvents {

    /**
     * One stop shop for all application event names
     */
    public static interface Events {

        static final String APP_STARTED = "app.started";
        static final String APP_START_FAILED = "app.start.failed";
        static final String APP_STOPPING = "app.stopping";
        static final String APP_STOP_EXCEPTION = "app.stop.exception";
        static final String APP_STOPPED = "app.stopped";

        static final String SERVICES_STOPPED = "services.stopped";
        static final String SERVICES_HEALTHY = "services.healthy";
        static final String SERVICES_FAILURE = "services.failure";

        static final String SERVICE_STARTING = "service.starting";
        static final String SERVICE_RUNNING = "service.running";
        static final String SERVICE_STOPPING = "service.stopping";
        static final String SERVICE_TERMINATED = "service.terminated";
        static final String SERVICE_FAILED = "service.failed";
    }

    @RequiredArgsConstructor
    public static final class HealthCheckResult implements JsonRepresentation {

        @NonNull
        private final HealthCheck.Result result;

        @Override
        public JsonObject toJson() {
            return Json.createObjectBuilder().add("healthy", result.isHealthy()).build();
        }

    }

}