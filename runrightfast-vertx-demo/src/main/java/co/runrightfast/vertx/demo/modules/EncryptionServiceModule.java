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
package co.runrightfast.vertx.demo.modules;

import co.runrightfast.core.crypto.CipherFunctions;
import co.runrightfast.core.crypto.Decryption;
import co.runrightfast.core.crypto.Encryption;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.core.crypto.EncryptionServiceException;
import co.runrightfast.core.crypto.impl.EncryptionServiceImpl;
import co.runrightfast.vertx.core.messages.SecretKeys;
import static co.runrightfast.vertx.core.protobuf.MessageConversions.toKeyMap;
import dagger.Module;
import dagger.Provides;
import java.io.InputStream;
import java.util.Set;
import javax.inject.Singleton;
import lombok.NonNull;
import org.apache.shiro.crypto.AesCipherService;

/**
 *
 * @author alfio
 */
@Module
public class EncryptionServiceModule {

    private static final class EncryptionServiceWithDefaultCiphers implements EncryptionService {

        private final String GLOBAL = "GLOBAL";

        private final EncryptionService encryptionService;

        public EncryptionServiceWithDefaultCiphers(@NonNull final AesCipherService cipherService) {
            try (final InputStream is = getClass().getResourceAsStream("/secretKeys")) {
                encryptionService = new EncryptionServiceImpl(
                        cipherService,
                        toKeyMap(SecretKeys.parseFrom(is))
                );
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public Set<String> getEncryptionKeys() throws EncryptionServiceException {
            return encryptionService.getEncryptionKeys();
        }

        @Override
        public byte[] encrypt(byte[] data, String secretKey) throws EncryptionServiceException {
            return encryptionService.encrypt(data, GLOBAL);
        }

        @Override
        public byte[] decrypt(byte[] data, String secretKey) throws EncryptionServiceException {
            return encryptionService.decrypt(data, GLOBAL);
        }

        @Override
        public Encryption encryption(String secretKey) throws EncryptionServiceException {
            return encryptionService.encryption(GLOBAL);
        }

        @Override
        public Decryption decryption(String secretKey) throws EncryptionServiceException {
            return encryptionService.decryption(GLOBAL);
        }

        @Override
        public CipherFunctions cipherFunctions(String secretKey) throws EncryptionServiceException {
            return encryptionService.cipherFunctions(GLOBAL);
        }

    }

    @Provides
    @Singleton
    public EncryptionService provideEncryptionService(final AesCipherService aes) {
        return new EncryptionServiceWithDefaultCiphers(aes);
    }
}
