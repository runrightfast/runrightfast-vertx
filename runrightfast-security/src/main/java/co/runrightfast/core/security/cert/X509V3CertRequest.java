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
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;

/**
 * Version 3 certificates are used as intermediate certificates and for what are referred to as end-entity certificates. An end-entity in this context is usual
 * an organization or individual that is using the certificate for some purpose, either to publish their public encryption key or to provide others with a way
 * of verifying signatures.
 *
 * @author alfio
 */
@ToString(callSuper = true, exclude = "subjectPublicKey")
public final class X509V3CertRequest extends AbstractX509CertRequest {

    @Getter
    private final X500Principal subjectPrincipal;

    @Getter
    private final PublicKey subjectPublicKey;

    @Getter
    private final Collection<X509CertExtension> extentions;

    public X509V3CertRequest(
            final X500Principal issuerPrincipal,
            final BigInteger serialNumber,
            final Instant notBefore,
            final Instant notAfter,
            @NonNull final X500Principal subjectPrincipal,
            @NonNull final PublicKey subjectPublicKey,
            @NonNull final Collection<X509CertExtension> extentions
    ) {
        super(issuerPrincipal, serialNumber, notBefore, notAfter);
        this.subjectPrincipal = subjectPrincipal;
        this.subjectPublicKey = subjectPublicKey;
        this.extentions = ImmutableList.copyOf(extentions);
    }

    public X509v3CertificateBuilder x509v3CertificateBuilder() {
        final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerPrincipal,
                serialNumber,
                Date.from(notBefore),
                Date.from(notAfter),
                subjectPrincipal,
                subjectPublicKey
        );

        extentions.stream().forEach(ext -> {
            try {
                builder.addExtension(ext.getOid(), ext.isCritical(), ext.getValue());
            } catch (final CertIOException ex) {
                throw new ApplicationException(String.format("Failed to add extenstion: %s", ext), ex);
            }
        });

        return builder;
    }

}
