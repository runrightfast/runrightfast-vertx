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
package co.runrightfast.vertx.orientdb;

/**
 *
 * @author alfio
 */
public interface OrientDBConstants {

    static final String ROOT_USER = "root";

    static final String NETWORK_BINARY_PROTOCOL = "binary";

    static enum GlobalConfigKey {

        /**
         * In order to speedup the hashing of password, OrientDB uses a password cache implemented as a LRU with maximum 500 entries. To change this setting,
         * set the global configuration security.userPasswordSaltCacheSize to the entries to cache. Use 0 to completely disable the cache.
         *
         * NOTE: If an attacker have access to the JVM memory dump, he could access to this map containing all the passwords. If you want to protect against
         * this attack, disable the in memory password cache.
         */
        SECURITY_USER_PASSWORD_SALT_CACHE_SIZE("security.userPasswordSaltCacheSize"),
        /**
         * LogFormatter is installed automatically by Server. To disable it define the setting <b>orientdb.installCustomFormatter</b> to false.
         */
        LOGGING_INSTALL_CUSTOM_FORMATTER("orientdb.installCustomFormatter");

        public final String key;

        private GlobalConfigKey(final String key) {
            this.key = key;
        }

    }

}
