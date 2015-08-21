package co.runrightfast.vertx.testSupport;

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
import co.runrightfast.core.crypto.CipherFunctions;
import co.runrightfast.core.crypto.Decryption;
import co.runrightfast.core.crypto.Encryption;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.core.crypto.EncryptionServiceException;
import co.runrightfast.core.crypto.impl.EncryptionServiceImpl;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import org.apache.shiro.crypto.AesCipherService;

/**
 *
 * @author alfio
 */
public class EncryptionServiceWithDefaultCiphers implements EncryptionService {

    final String GLOBAL = "GLOBAL";

    final AesCipherService cipherService = new AesCipherService();

    {
        cipherService.setKeySize(256);
    }

    private final EncryptionService encryptionService = new EncryptionServiceImpl(
            cipherService,
            ImmutableMap.of(GLOBAL, cipherService.generateNewKey())
    );

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
