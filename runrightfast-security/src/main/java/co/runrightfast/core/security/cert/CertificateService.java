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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public interface CertificateService {

    static boolean containsKeyUsage(@NonNull final X509Certificate cert, @NonNull final KeyUsage keyUsage) {
        final boolean[] keyUsages = cert.getKeyUsage();
        if (keyUsages == null) {
            return false;
        }

        return keyUsages[keyUsage.id];
    }

    /**
     *
     * @param request X509V1CertRequest
     * @param privateKey used to sign the certificate
     * @return X509Certificate
     * @throws CertificateServiceException
     */
    X509Certificate generateX509CertificateV1(X509V1CertRequest request, PrivateKey privateKey) throws CertificateServiceException;

    default X509Certificate generateSelfSignedX509CertificateV1(@NonNull final SelfSignedX509V1CertRequest request) throws CertificateServiceException {
        return generateX509CertificateV1(request.toX509V1CertRequest(), request.getKeyPair().getPrivate());
    }

    /**
     *
     * @param request X509V3CertRequest
     * @param privateKey used to sign the certificate
     * @return X509Certificate
     * @throws CertificateServiceException
     */
    X509Certificate generateX509CertificateV3(X509V3CertRequest request, PrivateKey privateKey) throws CertificateServiceException;

    /**
     *
     * @param request CAIssuedX509V3CertRequest
     * @param privateKey used to sign the certificate
     * @return X509Certificate
     * @throws CertificateServiceException
     */
    default X509Certificate generateX509CertificateV3(@NonNull final CAIssuedX509V3CertRequest request, final PrivateKey privateKey) throws CertificateServiceException {
        return generateX509CertificateV3(request.getX509V3CertRequest(), privateKey);
    }

    default X509Certificate generateSelfSignedX509CertificateV3(@NonNull final SelfSignedX509V3CertRequest request) throws CertificateServiceException {
        return generateX509CertificateV3(request.getX509V3CertRequest(), request.getPrivateKey());
    }

}
