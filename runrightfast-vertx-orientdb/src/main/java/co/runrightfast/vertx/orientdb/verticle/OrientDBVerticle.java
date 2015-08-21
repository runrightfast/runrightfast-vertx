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
package co.runrightfast.vertx.orientdb.verticle;

import co.runrightfast.core.application.event.AppEventLogger;
import static co.runrightfast.core.application.services.healthchecks.HealthCheckConfig.FailureSeverity.FATAL;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_NOT_BE_BLANK;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxHealthCheck;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxHealthCheck.ODatabaseDocumentTxHealthCheckBuilder;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import co.runrightfast.vertx.orientdb.classes.DocumentObject;
import co.runrightfast.vertx.orientdb.impl.EmbeddedOrientDBService;
import co.runrightfast.vertx.orientdb.impl.EmbeddedOrientDBServiceConfig;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.Json;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Manages an embedded OrientDB server.
 *
 * Only a single instance of this verticle should be created per JVM process.
 *
 * @author alfio
 */
public class OrientDBVerticle extends RunRightFastVerticle {

    @Getter
    private final RunRightFastVerticleId runRightFastVerticleId;

    private final EmbeddedOrientDBServiceConfig config;

    private EmbeddedOrientDBService service;

    private final ImmutableSetMultimap<String, Class<? extends DocumentObject>> databaseClassesForHealthCheck;

    public OrientDBVerticle(
            final AppEventLogger appEventLogger,
            final EncryptionService encryptionService,
            final String verticleName,
            @NonNull final EmbeddedOrientDBServiceConfig config,
            @NonNull SetMultimap<String, Class<? extends DocumentObject>> databaseClassesForHealthCheck) {
        super(appEventLogger, encryptionService);
        checkArgument(isNotBlank(verticleName), MUST_NOT_BE_BLANK, "serviceName");
        config.validate();

        runRightFastVerticleId = RunRightFastVerticleId.builder()
                .group(RunRightFastVerticleId.RUNRIGHTFAST_GROUP)
                .name(verticleName)
                .version("1.0.0")
                .build();

        this.config = config;
        this.databaseClassesForHealthCheck = ImmutableSetMultimap.copyOf(databaseClassesForHealthCheck);
    }

    @Override
    protected void startUp() {
        service = new EmbeddedOrientDBService(config);
        ServiceUtils.start(service);
    }

    @Override
    protected void shutDown() {
        ServiceUtils.stop(service);
        service = null;
    }

    @Override
    public Set<RunRightFastHealthCheck> getHealthChecks() {
        return oDatabaseDocumentTxHealthChecks();
    }

    private Set<RunRightFastHealthCheck> oDatabaseDocumentTxHealthChecks() {
        ServiceUtils.awaitRunning(service);
        return service.getDatabaseNames().stream()
                .map(name -> {
                    final ODatabaseDocumentTxSupplier oDatabaseDocumentTxSupplier = service.getODatabaseDocumentTxSupplier(name).get();
                    final ODatabaseDocumentTxHealthCheckBuilder healtcheckBuilder = ODatabaseDocumentTxHealthCheck.builder().oDatabaseDocumentTxSupplier(oDatabaseDocumentTxSupplier);
                    final Set<Class<? extends DocumentObject>> classes = databaseClassesForHealthCheck.get(name);
                    if (CollectionUtils.isNotEmpty(classes)) {
                        classes.stream().forEach(healtcheckBuilder::documentObject);
                    } else {
                        warning.log("oDatabaseDocumentTxHealthChecks", () -> {
                            return Json.createObjectBuilder()
                            .add("database", name)
                            .add("message", "No OrientDB classes are configured for the healthcheck")
                            .build();
                        });
                    }
                    return healtcheckBuilder.build();
                }).map(healthcheck -> {
                    return RunRightFastHealthCheck.builder()
                    .config(healthCheckConfigBuilder()
                            .name("EmbeddedOrientDBServiceHealthCheck")
                            .severity(FATAL)
                            .build()
                    )
                    .healthCheck(healthcheck)
                    .build();
                }).collect(Collectors.toSet());
    }

}
