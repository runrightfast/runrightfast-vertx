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
package co.runrightfast.core;

import java.util.function.Supplier;

/**
 *
 * @author alfio
 */
public class ConfigurationException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public static final class ConfigurationExceptionSupplier implements Supplier<ConfigurationException> {

        private final String message;

        public ConfigurationExceptionSupplier(final String message) {
            this.message = message;
        }

        @Override
        public ConfigurationException get() {
            return new ConfigurationException(message);
        }

    }

    public ConfigurationException(final String message) {
        super(message);
    }

    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

}
