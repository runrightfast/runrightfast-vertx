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
import co.runrightfast.core.security.cert.CertificateService;
import co.runrightfast.core.security.cert.SelfSignedX509V1CertRequest;
import co.runrightfast.core.security.cert.X509V1CertRequest;
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
import lombok.extern.java.Log;
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
        final DistinguishedName issuer = DistinguishedName.builder()
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

        final X500Principal principal = issuer.toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();

        final X509V1CertRequest request = X509V1CertRequest.builder()
                .issuerPrincipal(principal)
                .notAfter(Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)))
                .notBefore(Instant.now())
                .publicKey(keyPair.getPublic())
                .serialNumber(BigInteger.ONE)
                .subjectPrincipal(principal)
                .build();
        final PrivateKey privateKey = keyPair.getPrivate();
        final X509Certificate result = certificateService.generateX509CertificateV1(request, privateKey);
        log.info(String.format("result.getSigAlgName() = %s, result.getVersion() = %s ", result.getSigAlgName(), result.getVersion()));
        assertThat(result.getVersion(), is(1));

        result.checkValidity();
        assertThat(Arrays.areEqual(principal.getEncoded(), result.getIssuerX500Principal().getEncoded()), is(true));
        result.verify(keyPair.getPublic());
    }

    @Test
    public void test_generateSelfSignedX509CertificateV1() throws NoSuchAlgorithmException, NoSuchProviderException, CertificateExpiredException, CertificateNotYetValidException, CertificateException, InvalidKeyException, SignatureException {
        final DistinguishedName issuer = DistinguishedName.builder()
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

        final X500Principal principal = issuer.toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();

        final SelfSignedX509V1CertRequest request = SelfSignedX509V1CertRequest.builder()
                .issuerPrincipal(principal)
                .notAfter(Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)))
                .notBefore(Instant.now())
                .keyPair(keyPair)
                .serialNumber(BigInteger.ONE)
                .build();
        final PrivateKey privateKey = keyPair.getPrivate();
        final X509Certificate result = certificateService.generateSelfSignedX509CertificateV1(request);
        log.info(String.format("result.getSigAlgName() = %s, result.getVersion() = %s ", result.getSigAlgName(), result.getVersion()));
        assertThat(result.getVersion(), is(1));

        result.checkValidity();
        assertThat(Arrays.areEqual(principal.getEncoded(), result.getIssuerX500Principal().getEncoded()), is(true));
        result.verify(keyPair.getPublic());
    }

}
