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
package co.runrightfast.core.crypto.impl;

import co.runrightfast.core.crypto.Decryption;
import co.runrightfast.core.crypto.Encryption;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.core.crypto.EncryptionServiceException;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableMap;
import java.security.Key;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.shiro.crypto.CipherService;

/**
 *
 * @author alfio
 */
public class EncryptionServiceImpl implements EncryptionService {

    @RequiredArgsConstructor
    private static class CipherFunctions {

        final Encryption encryption;

        final Decryption decryption;
    }

    private final Map<String, CipherFunctions> cipherFunctions;

    public EncryptionServiceImpl(@NonNull final CipherService cipherService, final Map<String, Key> secretKeys) {
        checkSecretKeys(secretKeys);
        this.cipherFunctions = cipherFunctions(cipherService, secretKeys);
    }

    private Map<String, CipherFunctions> cipherFunctions(@NonNull final CipherService cipherService, final Map<String, Key> secretKeys) {
        final ImmutableMap.Builder<String, CipherFunctions> map = ImmutableMap.builder();
        secretKeys.entrySet().stream().forEach(entry -> {
            final byte[] secretKey = entry.getValue().getEncoded();
            final Encryption encryption = data -> cipherService.encrypt(data, secretKey).getBytes();
            final Decryption decryption = data -> cipherService.decrypt(data, secretKey).getBytes();
            map.put(entry.getKey(), new CipherFunctions(encryption, decryption));
        });

        return map.build();
    }

    private void checkSecretKeys(final Map<String, Key> secretKeys) {
        checkArgument(MapUtils.isNotEmpty(secretKeys));
        checkArgument(!secretKeys.values().stream().filter(key -> key == null).findFirst().isPresent());
    }

    @Override
    public Set<String> getEncryptionKeys() throws EncryptionServiceException {
        return cipherFunctions.keySet();
    }

    @Override
    public byte[] encrypt(@NonNull final byte[] data, @NonNull final String secretKey) throws EncryptionServiceException {
        final CipherFunctions cipher = cipherFunctions.get(secretKey);
        checkNotNull(cipher);
        return cipher.encryption.apply(data);
    }

    @Override
    public byte[] decrypt(@NonNull final byte[] data, @NonNull final String secretKey) throws EncryptionServiceException {
        final CipherFunctions cipher = cipherFunctions.get(secretKey);
        checkNotNull(cipher);
        return cipher.decryption.apply(data);
    }

    @Override
    public Encryption encryption(@NonNull final String secretKey) throws EncryptionServiceException {
        final CipherFunctions cipher = cipherFunctions.get(secretKey);
        checkNotNull(cipher);
        return cipher.encryption;
    }

    @Override
    public Decryption decryption(@NonNull final String secretKey) throws EncryptionServiceException {
        final CipherFunctions cipher = cipherFunctions.get(secretKey);
        checkNotNull(cipher);
        return cipher.decryption;
    }

}
