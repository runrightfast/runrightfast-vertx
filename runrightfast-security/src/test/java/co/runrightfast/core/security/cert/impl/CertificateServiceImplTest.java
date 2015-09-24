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

import co.runrightfast.core.security.BouncyCastle;
import static co.runrightfast.core.security.BouncyCastle.BOUNCY_CASTLE;
import static co.runrightfast.core.security.KeyPairGeneratorAlgorithm.RSA;
import co.runrightfast.core.security.auth.x500.DistinguishedName;
import static co.runrightfast.core.security.bc.BouncyCastleUtils.jcaX509ExtensionUtils;
import co.runrightfast.core.security.cert.CertificateService;
import co.runrightfast.core.security.cert.SelfSignedX509V1CertRequest;
import co.runrightfast.core.security.cert.X509CertExtension;
import co.runrightfast.core.security.cert.X509V1CertRequest;
import co.runrightfast.core.security.cert.X509V3CertRequest;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import javax.security.auth.x500.X500Principal;
import lombok.Value;
import lombok.extern.java.Log;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.util.Arrays;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class CertificateServiceImplTest {

    private final CertificateService certificateService = new CertificateServiceImpl();

    public CertificateServiceImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        BouncyCastle.installBouncyCastleSecurityProvider();
    }

    @Test
    public void testGenerateX509CertificateV1() throws NoSuchAlgorithmException, NoSuchProviderException, CertificateExpiredException, CertificateNotYetValidException, CertificateException, InvalidKeyException, SignatureException {
        final DistinguishedName issuer = issuer();

        final X500Principal issuerPrincipal = issuer.toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair signingKeyPair = keyPairGenerator.generateKeyPair();

        final KeyPair certKeyPair = keyPairGenerator.generateKeyPair();

        final X509V1CertRequest request = new X509V1CertRequest(
                issuerPrincipal,
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                issuerPrincipal,
                certKeyPair.getPublic()
        );
        log.info(String.format("request : %s", request));

        final X509Certificate cert = certificateService.generateX509CertificateV1(request, signingKeyPair.getPrivate());
        log.info(String.format("result.getSigAlgName() = %s, result.getVersion() = %s ", cert.getSigAlgName(), cert.getVersion()));
        assertThat(cert.getVersion(), is(1));

        cert.checkValidity();
        assertThat(Arrays.areEqual(issuerPrincipal.getEncoded(), cert.getIssuerX500Principal().getEncoded()), is(true));
        cert.verify(signingKeyPair.getPublic());

    }

    @Test
    public void test_generateSelfSignedX509CertificateV1() throws NoSuchAlgorithmException, NoSuchProviderException, CertificateExpiredException, CertificateNotYetValidException, CertificateException, InvalidKeyException, SignatureException {
        final DistinguishedName issuer = issuer();

        final X500Principal principal = issuer.toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();

        final SelfSignedX509V1CertRequest request = new SelfSignedX509V1CertRequest(
                principal,
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                keyPair
        );
        log.info(String.format("request : %s", request));

        final X509Certificate cert = certificateService.generateSelfSignedX509CertificateV1(request);
        log.info(String.format("result.getSigAlgName() = %s, result.getVersion() = %s ", cert.getSigAlgName(), cert.getVersion()));
        assertThat(cert.getVersion(), is(1));

        cert.checkValidity();
        assertThat(Arrays.areEqual(principal.getEncoded(), cert.getIssuerX500Principal().getEncoded()), is(true));
        cert.verify(cert.getPublicKey());
    }

    /**
     * creates an intermediate certificate which can be used to sign other certificates
     *
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws CertificateExpiredException
     * @throws CertificateNotYetValidException
     * @throws CertificateException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    @Test
    public void testGenerateX509CertificateV3_intermediateCACertificate() throws NoSuchAlgorithmException, NoSuchProviderException, CertificateExpiredException, CertificateNotYetValidException, CertificateException, InvalidKeyException, SignatureException {
        final DistinguishedName subject = subject();

        final X500Principal subjectPrincipal = subject.toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair certKeyPair = keyPairGenerator.generateKeyPair();

        final CaCert caCert = caCert();
        final JcaX509ExtensionUtils extUtils = jcaX509ExtensionUtils();
        final ImmutableList<X509CertExtension> x509CertExtensions = ImmutableList.<X509CertExtension>builder()
                .add(X509CertExtension.builder()
                        .oid(Extension.authorityKeyIdentifier)
                        .value(extUtils.createAuthorityKeyIdentifier(caCert.getCert()))
                        .critical(false)
                        .build()
                )
                .add(X509CertExtension.builder()
                        .oid(Extension.subjectKeyIdentifier)
                        .value(extUtils.createSubjectKeyIdentifier(certKeyPair.getPublic()))
                        .critical(false)
                        .build()
                )
                .add(X509CertExtension.builder()
                        .oid(Extension.keyUsage)
                        .value(new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign))
                        .critical(true)
                        .build()
                )
                .add(X509CertExtension.builder()
                        .oid(Extension.basicConstraints)
                        .value(new BasicConstraints(0))
                        .critical(true)
                        .build()
                )
                .build();

        final X509V3CertRequest request = new X509V3CertRequest(
                caCert.cert.getIssuerX500Principal(),
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                subjectPrincipal,
                certKeyPair.getPublic(),
                x509CertExtensions
        );
        log.info(String.format("request : %s", request));

        final X509Certificate cert = certificateService.generateX509CertificateV3(request, caCert.getPrivateKey());
        log.info(String.format("result.getSigAlgName() = %s, result.getVersion() = %s ", cert.getSigAlgName(), cert.getVersion()));
        assertThat(cert.getVersion(), is(3));

        cert.checkValidity();
        assertThat(Arrays.areEqual(subjectPrincipal.getEncoded(), cert.getSubjectX500Principal().getEncoded()), is(true));
        assertThat(Arrays.areEqual(caCert.getCert().getSubjectX500Principal().getEncoded(), cert.getIssuerX500Principal().getEncoded()), is(true));
        cert.verify(caCert.getCert().getPublicKey());

    }

    /**
     * creates an end entity certificate which might be used to verify one of the subject's signatures or to encrypt data to be sent to the entity represented
     * by the certificate's subject
     *
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws CertificateExpiredException
     * @throws CertificateNotYetValidException
     * @throws CertificateException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    @Test
    public void testGenerateX509CertificateV3_endEntityCertificate() throws NoSuchAlgorithmException, NoSuchProviderException, CertificateExpiredException, CertificateNotYetValidException, CertificateException, InvalidKeyException, SignatureException {
        final DistinguishedName subject = subject();

        final X500Principal subjectPrincipal = subject.toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair certKeyPair = keyPairGenerator.generateKeyPair();

        final CaCert caCert = caCert();
        final JcaX509ExtensionUtils extUtils = jcaX509ExtensionUtils();
        final ImmutableList<X509CertExtension> x509CertExtensions = ImmutableList.<X509CertExtension>builder()
                .add(X509CertExtension.builder()
                        .oid(Extension.authorityKeyIdentifier)
                        .value(extUtils.createAuthorityKeyIdentifier(caCert.getCert()))
                        .critical(false)
                        .build()
                )
                .add(X509CertExtension.builder()
                        .oid(Extension.subjectKeyIdentifier)
                        .value(extUtils.createSubjectKeyIdentifier(certKeyPair.getPublic()))
                        .critical(false)
                        .build()
                )
                .add(X509CertExtension.builder()
                        .oid(Extension.keyUsage)
                        .value(new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))
                        .critical(true)
                        .build()
                )
                .add(X509CertExtension.builder()
                        .oid(Extension.basicConstraints)
                        .value(new BasicConstraints(false))
                        .critical(true)
                        .build()
                )
                .build();

        final X509V3CertRequest request = new X509V3CertRequest(
                caCert.cert.getIssuerX500Principal(),
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                subjectPrincipal,
                certKeyPair.getPublic(),
                x509CertExtensions
        );
        log.info(String.format("request : %s", request));

        final X509Certificate cert = certificateService.generateX509CertificateV3(request, caCert.getPrivateKey());
        log.info(String.format("result.getSigAlgName() = %s, result.getVersion() = %s ", cert.getSigAlgName(), cert.getVersion()));
        assertThat(cert.getVersion(), is(3));

        cert.checkValidity();
        assertThat(Arrays.areEqual(subjectPrincipal.getEncoded(), cert.getSubjectX500Principal().getEncoded()), is(true));
        assertThat(Arrays.areEqual(caCert.getCert().getSubjectX500Principal().getEncoded(), cert.getIssuerX500Principal().getEncoded()), is(true));
        cert.verify(caCert.getCert().getPublicKey());

    }

    private DistinguishedName issuer() {
        return DistinguishedName.builder()
                .commonName("Alfio Zappala")
                .country("US")
                .domain("www.runrightfast.co")
                .localityName("Rochester")
                .organizationName("RunRightFast.co")
                .organizationalUnitName("Executive")
                .stateOrProvinceName("NY")
                .streetAddress("123 Main St.")
                .userid("0123456789")
                .build();
    }

    private DistinguishedName subject() {
        return DistinguishedName.builder()
                .commonName("John Doe Inc.")
                .country("US")
                .domain("www.john.doe.co")
                .localityName("Rochester")
                .organizationName("John Doe Inc.")
                .organizationalUnitName("Executive")
                .stateOrProvinceName("NY")
                .streetAddress("123 Main St.")
                .build();
    }

    private CaCert caCert() throws NoSuchAlgorithmException, NoSuchProviderException {
        final DistinguishedName issuer = issuer();

        final X500Principal issuerPrincipal = issuer.toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair certKeyPair = keyPairGenerator.generateKeyPair();

        final X509V1CertRequest request = new X509V1CertRequest(
                issuerPrincipal,
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                issuerPrincipal,
                certKeyPair.getPublic()
        );
        log.info(String.format("request : %s", request));

        return new CaCert(certificateService.generateX509CertificateV1(request, certKeyPair.getPrivate()), certKeyPair.getPrivate());
    }

    @Value
    private class CaCert {

        X509Certificate cert;

        PrivateKey privateKey;
    }

}
