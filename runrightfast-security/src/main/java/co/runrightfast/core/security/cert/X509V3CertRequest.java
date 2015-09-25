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
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import javax.security.auth.x500.X500Principal;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
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
@ToString(callSuper = true, exclude = "subjectPublicKey")
public final class X509V3CertRequest extends AbstractX509CertRequest {

    @Getter
    private final X500Principal subjectPrincipal;

    @Getter
    private final PublicKey subjectPublicKey;

    @Getter
    private final Collection<X509CertExtension> extensions;

    /**
     * Creates end-entity certificate, i.e., with BasicConstraints(false)
     *
     * @param issuerPrincipal X500Principal
     * @param serialNumber BigInteger
     * @param notBefore Instant
     * @param notAfter Instant
     * @param subjectPrincipal X500Principal
     * @param subjectPublicKey PublicKey
     * @param extensions Subject Key Identifier (OID value "2.5.29.14") extension is added automatically. It must not be specified as an extension when
     * constructing an instance.
     */
    public X509V3CertRequest(
            final X500Principal issuerPrincipal,
            final BigInteger serialNumber,
            final Instant notBefore,
            final Instant notAfter,
            @NonNull final X500Principal subjectPrincipal,
            @NonNull final PublicKey subjectPublicKey,
            @NonNull final Collection<X509CertExtension> extensions
    ) {
        super(issuerPrincipal, serialNumber, notBefore, notAfter);
        checkConstraints(extensions);
        this.subjectPrincipal = subjectPrincipal;
        this.subjectPublicKey = subjectPublicKey;
        this.extensions = augmentExtensions(extensions, subjectPublicKey);
    }

    public X509V3CertRequest(
            final X500Principal issuerPrincipal,
            final BigInteger serialNumber,
            final Instant notBefore,
            final Instant notAfter,
            @NonNull final X500Principal subjectPrincipal,
            @NonNull final PublicKey subjectPublicKey,
            @NonNull final Collection<X509CertExtension> extensions,
            @NonNull final BasicConstraints basicConstraints
    ) {
        super(issuerPrincipal, serialNumber, notBefore, notAfter);
        checkConstraints(extensions);
        this.subjectPrincipal = subjectPrincipal;
        this.subjectPublicKey = subjectPublicKey;
        this.extensions = augmentExtensions(extensions, subjectPublicKey, X509CertExtension.basicConstraints(basicConstraints));
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

        extensions.stream().forEach(ext -> {
            try {
                builder.addExtension(ext.getOid(), ext.isCritical(), ext.getValue());
            } catch (final CertIOException ex) {
                throw new ApplicationException(String.format("Failed to add extenstion: %s", ext), ex);
            }
        });

        return builder;
    }

    private void checkConstraints(final Collection<X509CertExtension> extensions) {
        if (CollectionUtils.isEmpty(extensions)) {
            return;
        }

        final Extensions exts = new Extensions(extensions.stream().map(X509CertExtension::toExtension).toArray(Extension[]::new));
        checkArgument(BasicConstraints.fromExtensions(exts) == null, "BasicConstraints must not be specified as an extension - it is added automatically");
        checkArgument(SubjectKeyIdentifier.fromExtensions(exts) == null, "SubjectKeyIdentifier must not be specified as an extension - it is added automatically");
    }

    private Collection<X509CertExtension> augmentExtensions(final Collection<X509CertExtension> extensions, final PublicKey subjectPublicKey, final X509CertExtension... exts) {
        final JcaX509ExtensionUtils extUtils = jcaX509ExtensionUtils();
        return ImmutableList.<X509CertExtension>builder()
                .add(X509CertExtension.builder()
                        .oid(Extension.subjectKeyIdentifier)
                        .value(extUtils.createSubjectKeyIdentifier(subjectPublicKey))
                        .critical(false)
                        .build()
                )
                .addAll(extensions)
                .addAll(exts != null ? Arrays.stream(exts).collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }

}
