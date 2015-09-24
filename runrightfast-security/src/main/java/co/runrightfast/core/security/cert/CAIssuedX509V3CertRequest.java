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
import static co.runrightfast.core.security.bc.BouncyCastleUtils.jcaX509ExtensionUtils;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collection;
import javax.security.auth.x500.X500Principal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

/**
 * Version 3 certificates are used as intermediate certificates and for what are referred to as end-entity certificates. An end-entity in this context is usual
 * an organization or individual that is using the certificate for some purpose, either to publish their public encryption key or to provide others with a way
 * of verifying signatures.
 *
 * The following extensions are added automatically and must not be specified when constructing and instance:
 * <ol>
 * <li>Authority Key Identifier - OID value "2.5.29.35" (id-ce-authorityKeyIdentifier)
 * <li>Subject Key Identifier - OID value "2.5.29.14" (id-ce-subjectKeyIdentifier)
 * </ol>
 *
 * @author alfio
 */
@ToString(exclude = "caCert")
@EqualsAndHashCode(of = "x509V3CertRequest")
public final class CAIssuedX509V3CertRequest {

    @Getter
    private final X509Certificate caCert;

    @Getter
    private final X509V3CertRequest x509V3CertRequest;

    public CAIssuedX509V3CertRequest(
            @NonNull final X509Certificate caCert,
            @NonNull final BigInteger serialNumber,
            @NonNull final Instant notBefore,
            @NonNull final Instant notAfter,
            @NonNull final X500Principal subjectPrincipal,
            @NonNull final PublicKey subjectPublicKey,
            @NonNull final Collection<X509CertExtension> extensions
    ) {
        checkConstraints(extensions);
        this.caCert = caCert;
        this.x509V3CertRequest = new X509V3CertRequest(
                caCert.getSubjectX500Principal(),
                serialNumber,
                notBefore,
                notAfter,
                subjectPrincipal,
                subjectPublicKey,
                augmentExtensions(extensions)
        );
    }

    private void checkConstraints(final Collection<X509CertExtension> extensions) {
        if (CollectionUtils.isEmpty(extensions)) {
            return;
        }

        final Extensions exts = new Extensions(extensions.stream().map(X509CertExtension::toExtension).toArray(Extension[]::new));
        checkArgument(AuthorityKeyIdentifier.fromExtensions(exts) == null, "AuthorityKeyIdentifier must not be specified as an extension - it is added automatically");
    }

    private Collection<X509CertExtension> augmentExtensions(final Collection<X509CertExtension> extensions) {
        final JcaX509ExtensionUtils extUtils = jcaX509ExtensionUtils();
        final ImmutableList.Builder<X509CertExtension> x509CertExtensions = ImmutableList.<X509CertExtension>builder().addAll(extensions);
        try {
            x509CertExtensions.add(X509CertExtension.builder()
                    .oid(Extension.authorityKeyIdentifier)
                    .value(extUtils.createAuthorityKeyIdentifier(caCert))
                    .critical(false)
                    .build()
            );
        } catch (final CertificateEncodingException ex) {
            throw new ApplicationException(ex);
        }
        return x509CertExtensions.build();
    }

}
