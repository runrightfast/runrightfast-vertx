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
package co.runrightfast.vertx.demo.modules;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.application.ApplicationId;
import static co.runrightfast.vertx.core.docker.weave.WeaveUtils.getWeaveClusterHostIPAddress;
import co.runrightfast.vertx.demo.orientdb.EventLogRepository;
import co.runrightfast.vertx.orientdb.OrientDBPoolConfig;
import co.runrightfast.vertx.orientdb.classes.demo.EventLogRecord;
import co.runrightfast.vertx.orientdb.config.OrientDBConfig;
import co.runrightfast.vertx.orientdb.hooks.SetCreatedOnAndUpdatedOn;
import co.runrightfast.vertx.orientdb.impl.embedded.EmbeddedOrientDBServiceConfig;
import co.runrightfast.vertx.orientdb.lifecycle.RunRightFastOrientDBLifeCycleListener;
import co.runrightfast.vertx.orientdb.verticle.OrientDBRepositoryVerticleDeployment;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.DeploymentOptions;
import javax.inject.Singleton;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 */
@Module
@Log
public class OrientDBModule {

    @Provides(type = Provides.Type.SET)
    @Singleton
    public OrientDBRepositoryVerticleDeployment provideEventLogRepositoryDeployment(final AppEventLogger logger, final EncryptionService encryptionService) {
        return new OrientDBRepositoryVerticleDeployment(
                () -> new EventLogRepository(logger, encryptionService),
                EventLogRepository.class,
                new DeploymentOptions()
        );
    }

    @Provides
    @Singleton
    public EmbeddedOrientDBServiceConfig providesEmbeddedOrientDBServiceConfig(final OrientDBConfig orientDBConfig) {
        final ApplicationId appId = ApplicationId.builder().group("co.runrightfast").name("runrightfast-vertx-orientdb").version("1.0.0").build();
        final AppEventLogger appEventLogger = new AppEventJDKLogger(appId);

        final String dbUrl = String.format("remote:%s/%s",
                getWeaveClusterHostIPAddress().orElse(getWeaveClusterHostIPAddress("eth0").orElse("localhost")),
                EventLogRepository.DB
        );

        return EmbeddedOrientDBServiceConfig.newBuilder(orientDBConfig)
                .property(OGlobalConfiguration.DB_POOL_MIN, "1")
                .property(OGlobalConfiguration.DB_POOL_MAX, "50")
                .databasePoolConfig(new OrientDBPoolConfig(EventLogRepository.DB, dbUrl, "admin", "admin", 10, ImmutableSet.of(() -> new SetCreatedOnAndUpdatedOn()), EventLogRecord.class))
                .lifecycleListener(() -> new RunRightFastOrientDBLifeCycleListener(appEventLogger))
                .build();
    }

}
