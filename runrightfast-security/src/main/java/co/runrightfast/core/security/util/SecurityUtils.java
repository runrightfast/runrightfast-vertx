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
package co.runrightfast.core.security.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 *
 * @author alfio
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    private static SecureRandom secureRandom;

    public static SecureRandom strongSecureRandom() {
        if (secureRandom == null) {
            try {
                secureRandom = SecureRandom.getInstanceStrong();
            } catch (final NoSuchAlgorithmException ex) {
                // should never happen because Every implementation of the Java platform is required to
                // support at least one strong {@code SecureRandom} implementation.
                throw new RuntimeException(ex);
            }
        }

        return secureRandom;
    }

}
