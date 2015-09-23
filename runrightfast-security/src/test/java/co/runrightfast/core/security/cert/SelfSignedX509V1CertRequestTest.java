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

import co.runrightfast.core.security.BouncyCastle;
import static co.runrightfast.core.security.BouncyCastle.BOUNCY_CASTLE;
import static co.runrightfast.core.security.KeyPairGeneratorAlgorithm.RSA;
import co.runrightfast.core.security.auth.x500.DistinguishedName;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Instant;
import javax.security.auth.x500.X500Principal;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class SelfSignedX509V1CertRequestTest {

    static {
        BouncyCastle.installBouncyCastleSecurityProvider();
    }

    /**
     * The combination of the {@link AbstractX509V1CertRequest#issuerPrincipal } {@link AbstractX509V1CertRequest#serialNumber} must be unique.
     *
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    @Test
    public void testHashCodeEquals() throws NoSuchAlgorithmException, NoSuchProviderException {

        final X500Principal principal = DistinguishedName.builder()
                .commonName("Alfio Zappala")
                .country("US")
                .domain("www.runrightfast.co")
                .localityName("Rochester")
                .organizationName("RunRightFast.co")
                .organizationalUnitName("Executive")
                .stateOrProvinceName("NY")
                .streetAddress("123 Main St.")
                .userid("0123456789")
                .build()
                .toX500Principal();

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA.name(), BOUNCY_CASTLE);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();

        final SelfSignedX509V1CertRequest request1 = new SelfSignedX509V1CertRequest(
                principal,
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                keyPair
        );

        final SelfSignedX509V1CertRequest request2 = new SelfSignedX509V1CertRequest(
                principal,
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                keyPair
        );

        final SelfSignedX509V1CertRequest request3 = new SelfSignedX509V1CertRequest(
                principal,
                BigInteger.TEN,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                keyPair
        );

        final KeyPair keyPair4 = keyPairGenerator.generateKeyPair();
        final SelfSignedX509V1CertRequest request4 = new SelfSignedX509V1CertRequest(
                principal,
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                keyPair4
        );

        final X500Principal principal2 = DistinguishedName.builder()
                .commonName("Alfio Zappala II")
                .country("US")
                .domain("www.runrightfast.co")
                .localityName("Rochester")
                .organizationName("RunRightFast.co")
                .organizationalUnitName("Executive")
                .stateOrProvinceName("NY")
                .streetAddress("123 Main St.")
                .userid("0123456789")
                .build()
                .toX500Principal();

        final SelfSignedX509V1CertRequest request5 = new SelfSignedX509V1CertRequest(
                principal2,
                BigInteger.ONE,
                Instant.now(),
                Instant.ofEpochMilli(System.currentTimeMillis() + (10 * 1000)),
                keyPair
        );

        assertThat(request1.hashCode(), is(request2.hashCode()));
        assertThat(request1, is(equalTo(request2)));

        assertThat(request1.hashCode(), is(request4.hashCode()));
        assertThat(request1, is(equalTo(request4)));

        assertThat(request1.hashCode() != request3.hashCode(), is(true));
        assertThat(request1, is(not(equalTo(request3))));

        assertThat(request1.hashCode() != request5.hashCode(), is(true));
        assertThat(request1, is(not(equalTo(request5))));
    }

}
