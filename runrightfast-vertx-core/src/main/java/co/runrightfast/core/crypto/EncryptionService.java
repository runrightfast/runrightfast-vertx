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
package co.runrightfast.core.crypto;

import java.util.Set;

/**
 *
 * @author alfio
 */
public interface EncryptionService {

    Set<String> getEncryptionKeys() throws EncryptionServiceException;

    byte[] encrypt(byte[] data, String encryptionKey) throws EncryptionServiceException;

    byte[] decrypt(byte[] data, String encryptionKey) throws EncryptionServiceException;

    Encryption encryption(String encryptionKey) throws EncryptionServiceException;

    Decryption decryption(String encryptionKey) throws EncryptionServiceException;
}
