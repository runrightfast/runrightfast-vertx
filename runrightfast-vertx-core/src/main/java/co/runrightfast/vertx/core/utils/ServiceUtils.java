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
package co.runrightfast.vertx.core.utils;

import com.google.common.util.concurrent.Service;
import static com.google.common.util.concurrent.Service.State.NEW;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public interface ServiceUtils {

    static Logger LOG = Logger.getLogger(ServiceUtils.class.getName());

    static void start(@NonNull final Service service) {
        switch (service.state()) {
            case NEW:
                service.startAsync();
                awaitRunning(service);
                return;
            case STARTING:
                service.awaitRunning();
                awaitRunning(service);
                return;
            case RUNNING:
                return;
            default:
                throw new IllegalStateException("Service cannot be started because the service state is :" + service.state());
        }

    }

    /**
     *
     * @param service if null, then do nothing
     */
    static void stop(final Service service) {
        if (service != null) {
            switch (service.state()) {
                case STARTING:
                case RUNNING:
                    service.stopAsync();
                    awaitTerminated(service);
                    return;
                case STOPPING:
                    awaitTerminated(service);
                    return;
                case NEW:
                case FAILED:
                case TERMINATED:
                    if (LOG.isLoggable(FINE)) {
                        LOG.logp(FINE, ServiceUtils.class.getName(), "stop",
                                "Service ({0}) is not running: {1}", new Object[]{service.getClass().getName(), service.state()}
                        );
                    }
            }
        }
    }

    /**
     * Logs a warning every 10 seconds, waiting for the service to stop
     *
     * @param service Service
     */
    static void awaitTerminated(@NonNull final Service service) {
        while (true) {
            try {
                service.awaitTerminated(10, TimeUnit.SECONDS);
                return;
            } catch (final TimeoutException ex) {
                LOG.logp(WARNING, ServiceUtils.class.getName(), "awaitTerminated", "Wating for service to terminate : {0}", service.getClass().getName());
            }
        }
    }

    /**
     * Logs a warning every 10 seconds, waiting for the service to start
     *
     * @param service Service
     */
    static void awaitRunning(@NonNull final Service service) {
        while (true) {
            try {
                service.awaitRunning(10, TimeUnit.SECONDS);
                return;
            } catch (final TimeoutException ex) {
                LOG.logp(WARNING, ServiceUtils.class.getName(), "awaitTerminated", "Wating for service to start : {0}", service.getClass().getName());
            }
        }
    }

}
