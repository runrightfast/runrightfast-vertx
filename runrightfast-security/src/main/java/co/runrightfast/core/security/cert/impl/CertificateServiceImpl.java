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
package co.runrightfast.core.security.cert.impl;

import static co.runrightfast.core.security.BouncyCastle.BOUNCY_CASTLE;
import static co.runrightfast.core.security.SignatureAlgorithm.SHA512withRSA;
import co.runrightfast.core.security.cert.CertificateService;
import co.runrightfast.core.security.cert.CertificateServiceException;
import co.runrightfast.core.security.cert.X509V1CertRequest;
import co.runrightfast.core.security.cert.X509V3CertRequest;
import static co.runrightfast.core.security.util.SecurityUtils.strongSecureRandom;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import lombok.NonNull;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 *
 * @author alfio
 */
public class CertificateServiceImpl implements CertificateService {

    @Override
    public X509Certificate generateX509CertificateV1(@NonNull final X509V1CertRequest request, @NonNull final PrivateKey privateKey) {
        final ContentSigner signer = contentSigner(privateKey);
        final X509v1CertificateBuilder certBuilder = request.x509v1CertificateBuilder();
        final X509CertificateHolder certHolder = certBuilder.build(signer);
        return toX509Certificate(certHolder);
    }

    private X509Certificate toX509Certificate(final X509CertificateHolder certHolder) {
        try {
            return new JcaX509CertificateConverter()
                    .setProvider(BOUNCY_CASTLE)
                    .getCertificate(certHolder);
        } catch (final CertificateException ex) {
            throw new CertificateServiceException(ex);
        }
    }

    private ContentSigner contentSigner(final PrivateKey privateKey) {
        try {
            return new JcaContentSignerBuilder(SHA512withRSA.name())
                    .setProvider(BOUNCY_CASTLE)
                    .setSecureRandom(strongSecureRandom())
                    .build(privateKey);
        } catch (final OperatorCreationException ex) {
            throw new CertificateServiceException(ex);
        }
    }

    @Override
    public X509Certificate generateX509CertificateV3(@NonNull final X509V3CertRequest request, @NonNull final PrivateKey privateKey) throws CertificateServiceException {
        final ContentSigner signer = contentSigner(privateKey);
        final X509v3CertificateBuilder certBuilder = request.x509v3CertificateBuilder();
        final X509CertificateHolder certHolder = certBuilder.build(signer);
        return toX509Certificate(certHolder);
    }

}
