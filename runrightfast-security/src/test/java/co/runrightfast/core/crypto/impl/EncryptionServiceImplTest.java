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

import co.runrightfast.core.security.crypto.AESKeySizes;
import co.runrightfast.core.security.crypto.Decryption;
import co.runrightfast.core.security.crypto.Encryption;
import co.runrightfast.core.security.crypto.EncryptionService;
import co.runrightfast.core.security.crypto.EncryptionServiceException;
import co.runrightfast.core.security.crypto.UnknownSecretKeyException;
import co.runrightfast.core.security.crypto.impl.EncryptionServiceImpl;
import static co.runrightfast.core.security.crypto.impl.EncryptionServiceImpl.toKeyMap;
import co.runrightfast.core.security.messages.SecretKeys;
import co.runrightfast.core.security.messages.test.RunRightFastApplicationInstance;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.shiro.crypto.AesCipherService;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class EncryptionServiceImplTest {

    private final RunRightFastApplicationInstance appInstance = RunRightFastApplicationInstance.newBuilder()
            .setAppDeploymentVersion("201509-1")
            .setAppName(getClass().getSimpleName())
            .setAppVersion("1.0.0")
            .setHost("test-01")
            .setJvmId("123@test-01")
            .setStartedOn(Instant.now().toString())
            .build();

    private final byte[] appInstanceBytes = appInstance.toByteArray();

    private AesCipherService aesCipherService(final int keySize) {
        final AesCipherService aes = new AesCipherService();
        aes.setKeySize(keySize);
        return aes;
    }

    /**
     * Test of getEncryptionKeys method, of class EncryptionServiceImpl.
     */
    @Test
    public void testGetEncryptionKeys() {
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        assertThat(service.getEncryptionKeys().size(), is(1));
        assertThat(service.getEncryptionKeys().contains("a"), is(true));
        assertThat(service.getEncryptionKeys().contains("b"), is(false));
    }

    @Test
    public void testDefaultEncryptDecrypt() throws InvalidProtocolBufferException {
        final String METHOD = "testDefaultEncryptDecrypt";
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        byte[] encrypted = service.encrypt(appInstanceBytes, "a");
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("uncrypted length = %d", appInstanceBytes.length));
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("encrypted length = %d", encrypted.length));

        final byte[] decrypted = service.decrypt(encrypted, "a");
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("decrypted length = %d", appInstanceBytes.length));
        final RunRightFastApplicationInstance appInstance2 = RunRightFastApplicationInstance.parseFrom(decrypted);
        assertThat(appInstance2, is(equalTo(appInstance)));
    }

    @Test
    public void testBasicEncryptionDecryption() throws InvalidProtocolBufferException {
        final String METHOD = "testBasicEncryptionDecryption";
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        final Encryption encryption = service.encryption("a");
        final Decryption decryption = service.decryption("a");
        byte[] encrypted = encryption.apply(appInstanceBytes);
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("uncrypted length = %d", appInstanceBytes.length));
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("encrypted length = %d", encrypted.length));

        final byte[] decrypted = decryption.apply(encrypted);
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("decrypted length = %d", appInstanceBytes.length));
        final RunRightFastApplicationInstance appInstance2 = RunRightFastApplicationInstance.parseFrom(decrypted);
        assertThat(appInstance2, is(equalTo(appInstance)));
    }

    @Test
    public void testDefaultEncryptDecrypt_256KeySize() throws InvalidProtocolBufferException {
        final String METHOD = "testDefaultEncryptDecrypt_256KeySize";
        final AesCipherService aes = aesCipherService(256);
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        byte[] encrypted = service.encrypt(appInstanceBytes, "a");
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("uncrypted length = %d", appInstanceBytes.length));
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("encrypted length = %d", encrypted.length));

        final byte[] decrypted = service.decrypt(encrypted, "a");
        log.logp(Level.INFO, getClass().getName(), METHOD, String.format("decrypted length = %d", appInstanceBytes.length));
        final RunRightFastApplicationInstance appInstance2 = RunRightFastApplicationInstance.parseFrom(decrypted);
        assertThat(appInstance2, is(equalTo(appInstance)));
    }

    @Test(expected = UnknownSecretKeyException.class)
    public void testInvalidSecretKey_cipherFunctions() {
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        service.cipherFunctions("b");
    }

    @Test(expected = UnknownSecretKeyException.class)
    public void testInvalidSecretKey_encrypt() {
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        service.encrypt("data".getBytes(), "b");
    }

    @Test(expected = UnknownSecretKeyException.class)
    public void testInvalidSecretKey_encryption() {
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        service.encryption("b");
    }

    @Test(expected = UnknownSecretKeyException.class)
    public void testInvalidSecretKey_decrypt() {
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        service.decrypt(service.encrypt("data".getBytes(), "a"), "b");
    }

    @Test(expected = UnknownSecretKeyException.class)
    public void testInvalidSecretKey_decryption() {
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        service.decryption("b");
    }

    @Test(expected = EncryptionServiceException.class)
    public void testInvalidSecretKey_decrypt_with_wrong_key() {
        final AesCipherService aes = new AesCipherService();
        final Map<String, Key> keys = ImmutableMap.<String, Key>builder()
                .put("a", aes.generateNewKey())
                .put("b", aes.generateNewKey())
                .build();
        final EncryptionService service = new EncryptionServiceImpl(aes, keys);
        service.decrypt(service.encrypt("data".getBytes(), "a"), "b");
    }

    @Test
    public void testStoringSecretKeys() throws IOException {
        final AesCipherService cipherService = new AesCipherService();
        cipherService.setKeySize(AESKeySizes.KEY_SIZE_256);

        final Key key = cipherService.generateNewKey();
        final byte[] keyBytes = SerializationUtils.serialize(key);
        final ByteString keyByteString = ByteString.copyFrom(keyBytes);

        final String keyName = "GLOBAL";
        final SecretKeys keys = SecretKeys.newBuilder()
                .putAllKeys(ImmutableMap.of(keyName, keyByteString))
                .build();

        final EncryptionService service1 = new EncryptionServiceImpl(cipherService, toKeyMap(keys));

        final byte[] encryptedData = service1.encrypt("data".getBytes(), keyName);
        assertThat("data", is(new String(service1.decrypt(encryptedData, keyName))));

        final File secretKeysFile = new File("build/temp/secretKeys");
        secretKeysFile.getParentFile().mkdirs();
        try (final OutputStream os = new FileOutputStream(secretKeysFile)) {
            keys.writeTo(os);
        }

        final AesCipherService cipherService2 = new AesCipherService();
        cipherService2.setKeySize(AESKeySizes.KEY_SIZE_256);
        try (final InputStream is = new FileInputStream(secretKeysFile)) {
            final EncryptionService service2 = new EncryptionServiceImpl(cipherService2, toKeyMap(SecretKeys.parseFrom(is)));
            assertThat("data", is(new String(service2.decrypt(encryptedData, keyName))));
        }
    }

}
