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

import static com.google.common.base.Preconditions.checkArgument;
import java.util.Date;
import java.util.function.Function;
import lombok.NonNull;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;

/**
 *
 * @author alfio
 */
@FunctionalInterface
public interface X509V1CertRequestToX509v1CertificateBuilder extends Function<X509V1CertRequest, X509v1CertificateBuilder> {

    static X509v1CertificateBuilder x509v1CertificateBuilder(@NonNull final X509V1CertRequest request) {
        checkArgument(request.getSerialNumber().signum() == 1, "serialNumber must be > 0");
        return new JcaX509v1CertificateBuilder(
                request.getIssuerPrincipal(),
                request.getSerialNumber(),
                Date.from(request.getNotBefore()),
                Date.from(request.getNotAfter()),
                request.getSubjectPrincipal(),
                request.getPublicKey()
        );
    }

}
