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
package co.runrightfast.core.security.bc;

import co.runrightfast.core.security.BouncyCastle;
import static co.runrightfast.core.security.BouncyCastle.BOUNCY_CASTLE;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import lombok.extern.java.Log;
import org.bouncycastle.util.Arrays;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class SHA512DigestCalculatorTest {

    static {
        BouncyCastle.installBouncyCastleSecurityProvider();
    }

    @Test
    public void testGetDigest() throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
        final SHA512DigestCalculator sha512 = new SHA512DigestCalculator();
        final byte[] bytes = getClass().getName().getBytes();
        sha512.getOutputStream().write(bytes);
        final byte[] digest = sha512.getDigest();

        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", BOUNCY_CASTLE);
        final byte[] digest2 = messageDigest.digest(bytes);

        assertThat(Arrays.areEqual(digest, digest2), is(true));

        final MessageDigest messageDigest2 = MessageDigest.getInstance("SHA-512");
        final byte[] digest3 = messageDigest2.digest(bytes);

        assertThat(Arrays.areEqual(digest, digest3), is(true));

    }

}
