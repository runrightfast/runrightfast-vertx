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
package co.runrightfast.core.security.bc;

import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

/**
 *
 * @author alfio
 */
public final class BouncyCastleUtils {

    private static final JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils(new SHA512DigestCalculator());

    private BouncyCastleUtils() {
    }

    public static JcaX509ExtensionUtils jcaX509ExtensionUtils() {
        return jcaX509ExtensionUtils;
    }
}
