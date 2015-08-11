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

    /**
     *
     * @return secretKey keys
     * @throws EncryptionServiceException
     */
    Set<String> getEncryptionKeys() throws EncryptionServiceException;

    /**
     *
     * @param data
     * @param secretKey
     * @return
     * @throws EncryptionServiceException
     * @throws IllegalArgumentException if any of the args are null, or if secretKey is blank, or if the secretKey is invalid
     */
    byte[] encrypt(byte[] data, String secretKey) throws EncryptionServiceException;

    /**
     *
     * @param data
     * @param secretKey
     * @return
     * @throws EncryptionServiceException
     * @throws IllegalArgumentException if any of the args are null, or if secretKey is blank, or if the secretKey is invalid
     */
    byte[] decrypt(byte[] data, String secretKey) throws EncryptionServiceException;

    /**
     *
     * @param secretKey
     * @return function that encrypts using the specified secretKey key
     * @throws EncryptionServiceException
     * @throws IllegalArgumentException if secretKey is blank, or if the secretKey is invalid
     */
    Encryption encryption(String secretKey) throws EncryptionServiceException;

    /**
     *
     * @param secretKey
     * @return function that can be used to decrypts using the specified secretKey key
     * @throws EncryptionServiceException
     */
    Decryption decryption(String secretKey) throws EncryptionServiceException;
}
