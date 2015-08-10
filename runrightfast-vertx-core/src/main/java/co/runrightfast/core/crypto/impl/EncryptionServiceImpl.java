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
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.MapUtils;

/**
 *
 * @author alfio
 */
public class EncryptionServiceImpl implements EncryptionService {

    private final Map<String, byte[]> encryptionKeys;

    public EncryptionServiceImpl(final Map<String, byte[]> encryptionKeys) {
        checkArgument(MapUtils.isNotEmpty(encryptionKeys));
        this.encryptionKeys = ImmutableMap.copyOf(encryptionKeys);
    }

    @Override
    public Set<String> getEncryptionKeys() throws EncryptionServiceException {
        return encryptionKeys.keySet();
    }

    @Override
    public byte[] encrypt(byte[] data, String encryptionKey) throws EncryptionServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] decrypt(byte[] data, String encryptionKey) throws EncryptionServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Encryption encryption(String encryptionKey) throws EncryptionServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Decryption decryption(String encryptionKey) throws EncryptionServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
