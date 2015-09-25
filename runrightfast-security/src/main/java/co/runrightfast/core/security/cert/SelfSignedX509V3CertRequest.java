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
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;

/**
 * Version 3 certificates are used as intermediate certificates and for what are referred to as end-entity certificates. An end-entity in this context is usual
 * an organization or individual that is using the certificate for some purpose, either to publish their public encryption key or to provide others with a way
 * of verifying signatures.
 *
 * The following extensions are added automatically and must not be specified when constructing and instance:
 * <ol>
 * <li>Basic Constraints - OID value "2.5.29.19" (id-ce-basicConstraints)
 * <li>Subject Key Identifier - OID value "2.5.29.14" (id-ce-subjectKeyIdentifier)
 * </ol>
 *
 * @author alfio
 */
@ToString(callSuper = true)
public final class SelfSignedX509V3CertRequest {

    @Getter
    private final PrivateKey privateKey;

    @Getter
    private final X509V3CertRequest x509V3CertRequest;

    /**
     * Creates end-entity certificate, i.e., with BasicConstraints(false)
     *
     * @param issuerPrincipal X500Principal
     * @param serialNumber BigInteger
     * @param notBefore Instant
     * @param notAfter Instant
     * @param keyPair KeyPair
     * @param extensions Subject Key Identifier (OID value "2.5.29.14") extension is added automatically. It must not be specified as an extension when
     * constructing an instance.
     */
    public SelfSignedX509V3CertRequest(
            final X500Principal issuerPrincipal,
            final BigInteger serialNumber,
            final Instant notBefore,
            final Instant notAfter,
            @NonNull final KeyPair keyPair,
            @NonNull final Collection<X509CertExtension> extensions
    ) {
        this.x509V3CertRequest = new X509V3CertRequest(
                issuerPrincipal,
                serialNumber,
                notBefore,
                notAfter,
                issuerPrincipal,
                keyPair.getPublic(),
                extensions
        );
        this.privateKey = keyPair.getPrivate();
    }

    public SelfSignedX509V3CertRequest(
            final X500Principal issuerPrincipal,
            final BigInteger serialNumber,
            final Instant notBefore,
            final Instant notAfter,
            @NonNull final KeyPair keyPair,
            @NonNull final Collection<X509CertExtension> extensions,
            @NonNull final BasicConstraints basicConstraints
    ) {
        this.x509V3CertRequest = new X509V3CertRequest(
                issuerPrincipal,
                serialNumber,
                notBefore,
                notAfter,
                issuerPrincipal,
                keyPair.getPublic(),
                extensions,
                basicConstraints
        );
        this.privateKey = keyPair.getPrivate();
    }

    public X509v3CertificateBuilder x509v3CertificateBuilder() {
        final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                x509V3CertRequest.getIssuerPrincipal(),
                x509V3CertRequest.getSerialNumber(),
                Date.from(x509V3CertRequest.getNotBefore()),
                Date.from(x509V3CertRequest.getNotAfter()),
                x509V3CertRequest.getSubjectPrincipal(),
                x509V3CertRequest.getSubjectPublicKey()
        );

        x509V3CertRequest.getExtensions().stream().forEach(ext -> {
            try {
                builder.addExtension(ext.getOid(), ext.isCritical(), ext.getValue());
            } catch (final CertIOException ex) {
                throw new ApplicationException(String.format("Failed to add extenstion: %s", ext), ex);
            }
        });

        return builder;
    }

}
