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
package co.runrightfast.vertx.core.eventbus;

import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.utils.JvmProcess;
import static com.google.common.base.Preconditions.checkArgument;
import java.util.Arrays;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public interface EventBusAddress {

    public static final String RUNRIGHTFAST = "runrightfast";

    public static String eventBusAddress(final String path, final String... paths) {
        checkArgument(isNotBlank(path));
        final StringBuilder sb = new StringBuilder(128).append('/').append(path);
        if (ArrayUtils.isNotEmpty(paths)) {
            checkArgument(!Arrays.stream(paths).filter(StringUtils::isBlank).findFirst().isPresent());
            sb.append('/').append(String.join("/", paths));
        }
        return sb.toString();
    }

    public static String runrightfastEventBusAddress(final String path, final String... paths) {
        checkArgument(isNotBlank(path));
        final StringBuilder sb = new StringBuilder(128).append('/').append(RUNRIGHTFAST).append('/').append(path);
        if (ArrayUtils.isNotEmpty(paths)) {
            checkArgument(!Arrays.stream(paths).filter(StringUtils::isBlank).findFirst().isPresent());
            sb.append('/').append(String.join("/", paths));
        }
        return sb.toString();
    }

    public static String eventBusAddress(@NonNull final RunRightFastVerticleId verticleId, final String path, final String... paths) {
        checkArgument(isNotBlank(path));
        final StringBuilder sb = new StringBuilder(128)
                .append('/').append(verticleId.getGroup())
                .append('/').append(verticleId.getName())
                .append('/').append(path);
        if (ArrayUtils.isNotEmpty(paths)) {
            checkArgument(!Arrays.stream(paths).filter(StringUtils::isBlank).findFirst().isPresent());
            sb.append('/').append(String.join("/", paths));
        }
        return sb.toString();
    }

    public static String toProcessSpecificEventBusAddress(final String address) {
        checkArgument(isNotBlank(address));
        if (address.charAt(0) == '/') {
            return String.format("/%s%s", JvmProcess.JVM_ID, address);
        }
        return String.format("/%s/%s", JvmProcess.JVM_ID, address);
    }

}
