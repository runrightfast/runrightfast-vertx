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

import java.io.OutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.operator.DigestCalculator;

/**
 *
 * @author alfio
 */
public final class SHA512DigestCalculator implements DigestCalculator {

    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return new AlgorithmIdentifier(OID.SHA512.oid);
    }

    @Override
    public OutputStream getOutputStream() {
        return bos;
    }

    @Override
    public byte[] getDigest() {
        final byte[] bytes = bos.toByteArray();
        bos.reset();
        final Digest sha512 = new SHA512Digest();
        sha512.update(bytes, 0, bytes.length);
        byte[] digest = new byte[sha512.getDigestSize()];
        sha512.doFinal(digest, 0);
        return digest;
    }

}
