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
package co.runrightfast.core.security.cert;

import co.runrightfast.core.ApplicationException;
import static co.runrightfast.core.security.ASN1Encoding.DER;
import java.io.IOException;
import lombok.Builder;
import lombok.Value;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;

/**
 *
 * @author alfio
 */
@Value
@Builder
public class X509CertExtension {

    ASN1ObjectIdentifier oid;

    boolean critical;

    ASN1Encodable value;

    public Extension toExtension() {
        try {
            return new Extension(oid, critical, value.toASN1Primitive().getEncoded(DER.name()));
        } catch (final IOException ex) {
            throw new ApplicationException(ex);
        }
    }
}
