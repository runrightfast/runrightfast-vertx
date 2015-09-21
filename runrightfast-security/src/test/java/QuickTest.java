
import java.security.Security;
import java.util.Arrays;
import lombok.extern.java.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

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
/**
 *
 * @author alfio
 */
@Log
public class QuickTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testListSecurityProviders() {
        Arrays.stream(Security.getProviders()).forEach(provider -> log.info(String.format("%s : %s", provider.getName(), provider.getInfo())));
        assertThat(Arrays.stream(Security.getProviders()).filter(provider -> provider.getName().equals("BC")).findFirst().isPresent(), is(true));
    }

}
