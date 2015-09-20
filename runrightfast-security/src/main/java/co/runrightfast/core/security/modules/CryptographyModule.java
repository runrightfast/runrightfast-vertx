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
package co.runrightfast.core.security.modules;

import co.runrightfast.core.security.crypto.AESKeySizes;
import co.runrightfast.core.security.crypto.KeyGenerator;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.CipherService;

/**
 *
 * @author alfio
 */
@Module
public final class CryptographyModule {

    /**
     *
     * @return AesCipherService with 256 bit key size
     */
    @Provides
    @Singleton
    public AesCipherService provideAesCipherService() {
        final AesCipherService cipherService = new AesCipherService();
        cipherService.setKeySize(AESKeySizes.KEY_SIZE_256);
        return cipherService;
    }

    @Provides
    @Singleton
    public CipherService provideCipherService(final AesCipherService cipherService) {
        return cipherService;
    }

    @Provides
    @Singleton
    public KeyGenerator provideKeyGenerator(final AesCipherService cipherService) {
        return () -> cipherService.generateNewKey();
    }

}
