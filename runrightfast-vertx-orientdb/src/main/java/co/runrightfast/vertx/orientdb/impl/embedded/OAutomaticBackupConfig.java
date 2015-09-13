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
package co.runrightfast.vertx.orientdb.impl.embedded;

import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_ZERO;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_BE_WITHIN_RANGE;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.PATTERN_DOES_NOT_MATCH;
import static co.runrightfast.vertx.orientdb.impl.embedded.OAutomaticBackupConfig.DelayTimeUnit.DAY;
import static co.runrightfast.vertx.orientdb.impl.embedded.OAutomaticBackupConfig.DelayTimeUnit.toDelayTimeUnit;
import static com.google.common.base.Preconditions.checkArgument;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.handler.OAutomaticBackup;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
@Builder
@ToString
public class OAutomaticBackupConfig implements Supplier<OServerHandlerConfiguration> {

    public static enum DelayTimeUnit {

        DAY("d"),
        HOUR("h"),
        MINUTE("m"),
        SECOND("s"),
        MILLISECOND("ms");

        public final String symbol;

        private DelayTimeUnit(final String symbol) {
            this.symbol = symbol;
        }

        public String toDelaySetting(final int delay) {
            return new StringBuilder().append(delay).append(symbol).toString();
        }

        public static DelayTimeUnit toDelayTimeUnit(final String unit) {
            switch (unit) {
                case "d":
                    return DAY;
                case "h":
                    return HOUR;
                case "m":
                    return MINUTE;
                case "s":
                    return SECOND;
                case "ms":
                    return MILLISECOND;
                default:
                    throw new IllegalArgumentException(String.format("Invalid unit. Supported units are: %s",
                            Arrays.stream(DelayTimeUnit.values()).map(timeUnit -> timeUnit.symbol).collect(Collectors.joining(","))
                    ));
            }
        }
    }

    @RequiredArgsConstructor
    @ToString
    public static final class Delay {

        @Getter
        private final int delay;

        @Getter
        @NonNull
        private final DelayTimeUnit timeUnit;

        public Delay(final String delaySetting) {
            checkArgument(isNotBlank(delaySetting), MUST_NOT_BE_BLANK, "delaySetting");
            final Pattern pattern = Pattern.compile("(\\d+)([dhms]{1,2})");
            final Matcher matcher = pattern.matcher(delaySetting);
            checkArgument(matcher.matches(), PATTERN_DOES_NOT_MATCH, "delaySetting", pattern.pattern());
            this.delay = Integer.parseInt(matcher.group(1));
            this.timeUnit = toDelayTimeUnit(matcher.group(2));
        }

        public String toDelaySetting() {
            return timeUnit.toDelaySetting(delay);
        }

        @Override
        public String toString() {
            return toDelaySetting();
        }
    }

    @Getter
    private final boolean enabled;

    @Getter
    private final Delay delay;

    @Getter
    private final String firstTime;

    @Getter
    private final Path backupDir;

    @Getter
    private final int compressionLevel;

    @Getter
    private final int bufferSizeMB;

    @Getter
    private final List<String> databaseIncludes;

    @Getter
    private final List<String> databaseExcludes;

    public static OAutomaticBackupConfig disabledOAutomaticBackupConfig() {
        return new OAutomaticBackupConfig(false, new Delay(1, DAY), "00:00:00", Paths.get("backup"), 9, 1, Collections.emptyList(), Collections.emptyList());
    }

    /**
     *
     * @param enabled
     * @param delay - optional - defaults to daily if null
     * @param firstTime
     * @param backupDir
     * @param compressionLevel
     * @param bufferSizeMB
     * @param databaseIncludes
     * @param databaseExcludes
     */
    public OAutomaticBackupConfig(final boolean enabled, @NonNull final Delay delay, @NonNull final String firstTime, @NonNull final Path backupDir, final int compressionLevel, final int bufferSizeMB, @NonNull final List<String> databaseIncludes, @NonNull final List<String> databaseExcludes) {
        this.enabled = enabled;
        this.delay = delay;
        this.firstTime = firstTime;
        this.backupDir = backupDir;
        this.compressionLevel = compressionLevel;
        this.bufferSizeMB = bufferSizeMB;
        this.databaseIncludes = databaseIncludes;
        this.databaseExcludes = databaseExcludes;

        validate();
    }

    private void validate() {
        if (!enabled) {
            return;
        }
        checkArgument(isNotBlank(firstTime), MUST_NOT_BE_BLANK, "firstTime");
        final String firstTimePattern = "\\d\\d:\\d\\d:\\d\\d";
        checkArgument(Pattern.matches(firstTimePattern, firstTime), PATTERN_DOES_NOT_MATCH, "firstTime", firstTimePattern);
        checkArgument(compressionLevel >= 0 && compressionLevel <= 9, MUST_BE_WITHIN_RANGE, 0, 9);
        checkArgument(bufferSizeMB > 0, MUST_BE_GREATER_THAN_ZERO, "bufferSizeMB");
    }

    @Override
    public OServerHandlerConfiguration get() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OAutomaticBackup.class.getName();
        if (!enabled) {
            config.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("enabled", FALSE.toString())
            };
        } else {
            config.parameters = new OServerParameterConfiguration[]{
                new OServerParameterConfiguration("enabled", TRUE.toString()),
                new OServerParameterConfiguration("delay", delay.toDelaySetting()),
                new OServerParameterConfiguration("backup", backupDir.toAbsolutePath().toString()),
                new OServerParameterConfiguration("target.fileName", "${DBNAME}-${DATE:yyyyMMddHHmmss}.zip"),
                new OServerParameterConfiguration("compressionLevel", Integer.toString(compressionLevel)),
                new OServerParameterConfiguration("bufferSize", Integer.toString(bufferSizeMB * 1024 * 1000)),
                new OServerParameterConfiguration("db.include", databaseIncludes.stream().collect(Collectors.joining(","))),
                new OServerParameterConfiguration("db.exclude", databaseExcludes.stream().collect(Collectors.joining(",")))
            };

        }

        return config;
    }

}
