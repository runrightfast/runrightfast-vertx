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
package co.runrightfast.vertx.core.verticles.verticleManager;

import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.RunRightFastVerticleId.RUNRIGHTFAST_GROUP;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;

/**
 *
 * @author alfio
 */
public final class RunRightFastVerticleManager extends RunRightFastVerticle {

    private final Set<RunRightFastVerticleDeployment> deployments;

    @Inject
    public RunRightFastVerticleManager(final Set<RunRightFastVerticleDeployment> deployments) {
        super();
        checkArgument(CollectionUtils.isNotEmpty(deployments));
        this.deployments = ImmutableSet.copyOf(deployments);
    }

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
    }

    @Override
    protected RunRightFastVerticleId runRightFastVerticleId() {
        return RunRightFastVerticleId.builder()
                .group(RUNRIGHTFAST_GROUP)
                .name(getClass().getSimpleName())
                .version("0.1")
                .build();
    }

}
