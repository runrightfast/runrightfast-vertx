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
import co.runrightfast.core.security.bc.OID;
import static co.runrightfast.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_OR_EQUAL_TO;
import static com.google.common.base.Preconditions.checkArgument;
import java.io.IOException;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;

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

    public static X509CertExtension basicConstraints(@NonNull final BasicConstraints basicConstraints) {
        return X509CertExtension.builder()
                .oid(OID.BASIC_CONSTRAINTS.oid)
                .value(basicConstraints)
                .critical(true)
                .build();
    }

    public static X509CertExtension basicConstraintsForEndCertificate() {
        return X509CertExtension.builder()
                .oid(OID.BASIC_CONSTRAINTS.oid)
                .value(new BasicConstraints(false))
                .critical(true)
                .build();
    }

    public static X509CertExtension basicConstraintsForCACertificate(final int pathLenConstraint) {
        checkArgument(pathLenConstraint >= 0, MUST_BE_GREATER_THAN_OR_EQUAL_TO, "pathLenConstraint", 0);
        return X509CertExtension.builder()
                .oid(OID.BASIC_CONSTRAINTS.oid)
                .value(new BasicConstraints(pathLenConstraint))
                .critical(true)
                .build();
    }

    /**
     *
     * @return with pathLenConstraint = {@link Integer#MAX_VALUE}
     */
    public static X509CertExtension basicConstraintsForCACertificate() {
        return X509CertExtension.builder()
                .oid(OID.BASIC_CONSTRAINTS.oid)
                .value(new BasicConstraints(Integer.MAX_VALUE))
                .critical(true)
                .build();
    }

    public static X509CertExtension keyUsage(@NonNull final KeyUsage keyUsage) {
        return X509CertExtension.builder()
                .oid(OID.KEY_USAGE.oid)
                .value(keyUsage)
                .critical(true)
                .build();
    }
}
