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
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * The combination of the {@link X509V1CertRequest#getIssuerPrincipal() } {@link X509V1CertRequest#getSerialNumber() } must be unique.
 *
 *
 * @author alfio
 */
@Data
@Builder
public class X509V1CertRequest {

    @NonNull
    private final X500Principal issuerPrincipal;

    /**
     * Must be a positive number and must be unique per issuerPrincipal
     */
    @NonNull
    private final BigInteger serialNumber;

    @NonNull
    private final Instant notBefore;

    @NonNull
    private final Instant notAfter;

    @NonNull
    private final X500Principal subjectPrincipal;

    @NonNull
    private final PublicKey publicKey;

}
