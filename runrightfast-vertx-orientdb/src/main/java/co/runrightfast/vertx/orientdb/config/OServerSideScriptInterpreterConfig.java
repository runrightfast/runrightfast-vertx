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
package co.runrightfast.vertx.orientdb.config;

import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_EMPTY;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.handler.OServerSideScriptInterpreter;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 *
 * @author alfio
 */
public class OServerSideScriptInterpreterConfig implements Supplier<OServerHandlerConfiguration> {

    @Getter
    private final boolean enabled;

    @Getter
    private final List<String> allowedLanguages;

    public OServerSideScriptInterpreterConfig() {
        this(true, ImmutableList.of("SQL"));
    }

    public OServerSideScriptInterpreterConfig(final boolean enabled, @NonNull final List<String> allowedLanguages) {
        checkArgument(isNotEmpty(allowedLanguages), MUST_NOT_BE_EMPTY, "allowedLanguages");
        this.enabled = enabled;
        this.allowedLanguages = allowedLanguages;
    }

    @Override
    public OServerHandlerConfiguration get() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OServerSideScriptInterpreter.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", Boolean.toString(enabled)),
            new OServerParameterConfiguration("allowedLanguages", allowedLanguages.stream().collect(Collectors.joining(",")))
        };
        return config;
    }

}
