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
package co.runrightfast.vertx.orientdb.modules;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.orientdb.impl.EmbeddedOrientDBServiceConfig;
import co.runrightfast.vertx.orientdb.verticle.OrientDBRepositoryVerticleDeployment;
import co.runrightfast.vertx.orientdb.verticle.OrientDBVerticle;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.DeploymentOptions;
import java.util.Set;
import javax.inject.Singleton;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 *
 * @author alfio
 */
@Module
@Builder
public class OrientDBVerticleDeploymentModule {

    @NonNull
    @Singular
    private final Set<OrientDBRepositoryVerticleDeployment> repositoryDeployments;

    @Provides(type = Provides.Type.SET)
    @Singleton
    public RunRightFastVerticleDeployment provideOrientDBVerticleRunRightFastVerticleDeployment(
            final AppEventLogger logger,
            final EncryptionService encryptionService,
            final EmbeddedOrientDBServiceConfig embeddedOrientDBServiceConfig
    ) {
        return new RunRightFastVerticleDeployment(
                () -> new OrientDBVerticle(
                        logger,
                        encryptionService,
                        embeddedOrientDBServiceConfig,
                        repositoryDeployments.stream().toArray(OrientDBRepositoryVerticleDeployment[]::new)
                ),
                OrientDBVerticle.class,
                new DeploymentOptions()
        );
    }

}
