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
import co.runrightfast.core.utils.JvmProcess;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.protobuf.Message;
import java.util.Arrays;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Utility class for Vertx event bus
 *
 * @author alfio
 */
public interface EventBusAddress {

    public static final String RUNRIGHTFAST = "runrightfast";

    /**
     * Address format follows a URI path convention.
     *
     * e.g., eventBusAddress("path1","path2","path3") returns "/path1/path2/path3"
     *
     *
     * @param path REQUIRED
     * @param paths OPTIONAL
     * @return eventbus address
     */
    public static String eventBusAddress(final String path, final String... paths) {
        checkArgument(isNotBlank(path));
        final StringBuilder sb = new StringBuilder(128).append('/').append(path);
        if (ArrayUtils.isNotEmpty(paths)) {
            checkArgument(!Arrays.stream(paths).filter(StringUtils::isBlank).findFirst().isPresent());
            sb.append('/').append(String.join("/", paths));
        }
        return sb.toString();
    }

    /**
     * Address format follows a URI path convention, but prefixes the path with 'runrightfast'.
     *
     * e.g., eventBusAddress("path1","path2","path3") returns "/runrightfast/path1/path2/path3"
     *
     *
     * @param path REQUIRED
     * @param paths OPTIONAL
     * @return eventbus address
     */
    public static String runrightfastEventBusAddress(final String path, final String... paths) {
        checkArgument(isNotBlank(path));
        final StringBuilder sb = new StringBuilder(128).append('/').append(RUNRIGHTFAST).append('/').append(path);
        if (ArrayUtils.isNotEmpty(paths)) {
            checkArgument(!Arrays.stream(paths).filter(StringUtils::isBlank).findFirst().isPresent());
            sb.append('/').append(String.join("/", paths));
        }
        return sb.toString();
    }

    /**
     * Address format follows a URI path convention. The address path is prefixe with the verticle id's group and name
     *
     * e.g., eventBusAddress(verticleId,"path1","path2","path3") returns "/runrightfast/verticle-manager/path1/path2/path3"
     *
     * where the verticle id group is "runrightfast" and the verticle id name is "verticle-manager"
     *
     *
     * @param verticleId REQUIRED verticleId
     * @param path REQUIRED
     * @param paths OPTIONAL
     * @return eventbus address
     */
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

    /**
     *
     * @param verticleId RunRightFastVerticleId
     * @param messageClass uses the class's simple name
     * @return eventbus address
     */
    public static String eventBusAddress(@NonNull final RunRightFastVerticleId verticleId, @NonNull final Class<? extends Message> messageClass) {
        return eventBusAddress(verticleId, messageClass.getSimpleName());
    }

    /**
     * Prefixes the eventbus address with the JVM ID.
     *
     * e.g., toProcessSpecificEventBusAddress("/a/b/c") returns "/1234@some-host/a/b/c"
     *
     * where the JVM ID = "1234@some-host"
     *
     * @param address eventbus address
     * @return process specific address
     */
    public static String toProcessSpecificEventBusAddress(final String address) {
        checkArgument(isNotBlank(address));
        if (address.charAt(0) == '/') {
            return String.format("/%s%s", JvmProcess.JVM_ID, address);
        }
        return String.format("/%s/%s", JvmProcess.JVM_ID, address);
    }

}
