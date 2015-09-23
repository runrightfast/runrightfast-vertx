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
package co.runrightfast.core.security.auth.x500;

import java.io.IOException;
import javax.security.auth.x500.X500Principal;
import lombok.extern.java.Log;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.util.Arrays;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class DistinguishedNameTest {

    @Test
    public void test_toX500Principal() {
        final DistinguishedName dn = DistinguishedName.builder()
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

        log.info(String.format("dn =\n%s", dn));
        final X500Principal principal = dn.toX500Principal();
        log.info(String.format("principle name =\n%s", principal.getName()));

        final X500Principal principal2 = new X500Principal(principal.getEncoded());
        log.info(String.format("principle2 name =\n%s", principal2.getName()));
        log.info(String.format("principle2 name RFC2253 =\n%s", principal2.getName(X500Principal.RFC2253)));

        assertThat(Arrays.areEqual(principal.getEncoded(), principal2.getEncoded()), is(true));
        assertThat(principal, is(principal2));
    }

    @Test
    public void test_toX500Name() throws IOException {
        final DistinguishedName dn = DistinguishedName.builder()
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

        log.info(String.format("dn =\n%s", dn));
        final X500Principal principal = dn.toX500Principal();
        log.info(String.format("principle name =\n%s", principal.getName()));

        final X500Principal principal2 = new X500Principal(principal.getEncoded());
        log.info(String.format("principle2 name =\n%s", principal2.getName()));
        log.info(String.format("principle2 name RFC2253 =\n%s", principal2.getName(X500Principal.RFC2253)));

        assertThat(Arrays.areEqual(principal.getEncoded(), principal2.getEncoded()), is(true));
        assertThat(principal, is(principal2));

        final X500Name name1 = DistinguishedName.toX500Name(principal);
        final X500Name name2 = DistinguishedName.toX500Name(principal2);

        log.info(String.format("name1 : %s", name1));
        log.info(String.format("name2 : %s", name2));

        assertThat(Arrays.areEqual(name1.getEncoded(), name1.getEncoded()), is(true));
    }

}
