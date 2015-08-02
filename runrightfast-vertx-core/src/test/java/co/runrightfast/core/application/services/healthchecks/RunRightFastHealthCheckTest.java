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
package co.runrightfast.core.application.services.healthchecks;

import static co.runrightfast.core.application.services.healthchecks.HealthCheckConfig.FailureSeverity.FATAL;
import com.codahale.metrics.health.HealthCheck;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class RunRightFastHealthCheckTest {

    private HealthCheck healthyCheck = new HealthCheck() {

        @Override
        protected HealthCheck.Result check() throws Exception {
            return HealthCheck.Result.healthy();
        }
    };

    @Test
    public void test_noTags() {
        final RunRightFastHealthCheck healthCheck = RunRightFastHealthCheck.builder()
                .healthCheck(healthyCheck)
                .config(HealthCheckConfig.builder()
                        .name("test")
                        .registryName(getClass().getSimpleName())
                        .severity(FATAL)
                        .build()
                )
                .build();

        assertThat(healthCheck.getConfig().getName(), is("test"));
        assertThat(healthCheck.getConfig().getRegistryName(), is(getClass().getSimpleName()));
        assertThat(healthCheck.getConfig().getSeverity(), is(FATAL));
        assertThat(healthCheck.getConfig().getTags().isEmpty(), is(true));
    }

    @Test
    public void test_withTags() {
        final RunRightFastHealthCheck healthCheck = RunRightFastHealthCheck.builder()
                .healthCheck(healthyCheck)
                .config(HealthCheckConfig.builder()
                        .name("test")
                        .registryName(getClass().getSimpleName())
                        .severity(FATAL)
                        .tag("database")
                        .tag("orientdb")
                        .build()
                )
                .build();

        assertThat(healthCheck.getConfig().getTags().size(), is(2));
        assertThat(healthCheck.getConfig().getTags().contains("database"), is(true));
        assertThat(healthCheck.getConfig().getTags().contains("orientdb"), is(true));
        assertThat(healthCheck.getConfig().getTags().contains("asdad"), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_blankName() {
        RunRightFastHealthCheck.builder()
                .healthCheck(healthyCheck)
                .config(HealthCheckConfig.builder()
                        .name(" ")
                        .registryName(getClass().getSimpleName())
                        .severity(FATAL)
                        .build()
                )
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_blankRegistry() {
        RunRightFastHealthCheck.builder()
                .healthCheck(healthyCheck)
                .config(HealthCheckConfig.builder()
                        .name("test")
                        .registryName("")
                        .severity(FATAL)
                        .build()
                )
                .build();
    }

}
