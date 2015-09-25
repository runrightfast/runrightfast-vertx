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

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * The OID can be looked up via:
 *
 * <pre>http://oid-info.com/get/{oid}</pre>
 *
 * e.g http://oid-info.com/get/2.5.29.19
 *
 * @author alfio
 */
public enum OID {

    AUTHORITY_KEY_IDENIFIER("2.5.29.35"),
    BASIC_CONSTRAINTS("2.5.29.19"),
    KEY_USAGE("2.5.29.15"),
    SHA256("1.0.10118.3.0.59"),
    SHA384("1.0.10118.3.0.60"),
    SHA512("2.16.840.1.101.3.4.2.3"),
    SUBJECT_KEY_IDENIFIER("2.5.29.14"),;

    public final ASN1ObjectIdentifier oid;

    private OID(final String oidDotNotation) {
        this.oid = new ASN1ObjectIdentifier(oidDotNotation);
    }
}
