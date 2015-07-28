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
package co.runrightfast.vertx.core;

import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.MetricType.COUNTER;
import static co.runrightfast.vertx.core.RunRightFastVerticleMetrics.MetricType.TIMER;
import static com.google.common.base.Preconditions.checkArgument;
import java.util.Arrays;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

/**
 * These are for metrics collected per verticle deployment, i.e., verticles that have the same deployment id.
 *
 * @author alfio
 */
public interface RunRightFastVerticleMetrics {

    /**
     * Concatenates the name parts, separating each name part with a '.'
     *
     * e.g., COUNTER.verticle.instance-started
     *
     * @param name REQUIRED
     * @param names OPTIONAL
     * @return counter name
     */
    static String counterName(final String name, final String... names) {
        return metricName(COUNTER, name, names);
    }

    static String timerName(final String name, final String... names) {
        return metricName(TIMER, name, names);
    }

    static String metricName(@NonNull final MetricType metricType, final String name, final String... names) {
        checkArgument(StringUtils.isNotBlank(name));
        if (names != null) {
            checkArgument(!Arrays.stream(names).filter(StringUtils::isBlank).findFirst().isPresent(), "any of the names cannot be blank");
        }
        final StringBuilder sb = new StringBuilder(64);
        sb.append(metricType.name()).append(name);
        Arrays.stream(names).forEach(n -> sb.append('.').append(n));
        return sb.toString();
    }

    static enum MetricType {

        COUNTER,
        GAUGE,
        METER,
        HISTOGRAM,
        TIMER
    }

    static enum Counters {

        INSTANCE_STARTED("verticle", "instance", "started"),
        // the EventBus address will be appended to the metric name
        MESSAGE_CONSUMER_EXCEPTION("message-consumer", "exception"),
        MESSAGE_CONSUMER_MESSAGE_TOTAL("message-consumer", "message", "total"),
        MESSAGE_CONSUMER_MESSAGE_PROCESSING("message-consumer", "message", "processing");

        public final String metricName;

        private Counters(final String name, final String... names) {
            this.metricName = counterName(name, names);
        }
    }

    static enum Timers {

        MESSAGE_CONSUMER_HANDLER("message-consumer", "handler");

        public final String metricName;

        private Timers(final String name, final String... names) {
            this.metricName = timerName(name, names);
        }
    }

}
