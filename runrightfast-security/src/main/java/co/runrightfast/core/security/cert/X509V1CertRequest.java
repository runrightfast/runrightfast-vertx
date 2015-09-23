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

import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Instant;
import javax.security.auth.x500.X500Principal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * The combination of the {@link X509V1CertRequest#issuerPrincipal } {@link X509V1CertRequest#serialNumber} must be unique.
 *
 *
 * @author alfio
 */
@ToString(callSuper = true, exclude = "publicKey")
@EqualsAndHashCode(callSuper = true)
public final class X509V1CertRequest extends AbstractX509V1CertRequest {

    @Getter
    private final X500Principal subjectPrincipal;

    @Getter
    private final PublicKey publicKey;

    public X509V1CertRequest(final X500Principal issuerPrincipal, final BigInteger serialNumber, final Instant notBefore, final Instant notAfter, @NonNull final X500Principal subjectPrincipal, @NonNull final PublicKey publicKey) {
        super(issuerPrincipal, serialNumber, notBefore, notAfter);
        this.subjectPrincipal = subjectPrincipal;
        this.publicKey = publicKey;
    }

}
