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
package co.runrightfast.vertx.core.impl;

import co.runrightfast.core.application.event.AppEvent;
import co.runrightfast.core.application.event.AppEventLogger;
import static co.runrightfast.core.application.event.ApplicationEvents.APP_STARTED;
import static co.runrightfast.core.application.event.ApplicationEvents.APP_START_FAILED;
import static co.runrightfast.core.application.event.ApplicationEvents.APP_STOPPED;
import static co.runrightfast.core.application.event.ApplicationEvents.APP_STOPPING;
import static co.runrightfast.core.application.event.ApplicationEvents.APP_STOP_EXCEPTION;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.VertxConstants;
import static co.runrightfast.vertx.core.VertxConstants.VERTX_HAZELCAST_INSTANCE_ID;
import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.VertxService.LOG;
import static co.runrightfast.vertx.core.docker.weave.WeaveUtils.getWeaveClusterHostIPAddress;
import co.runrightfast.vertx.core.eventbus.VoidMessageCodec;
import static co.runrightfast.vertx.core.hazelcast.HazelcastConfigFactory.hazelcastConfigFactory;
import co.runrightfast.vertx.core.inject.qualifiers.VertxServiceConfig;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import com.google.common.util.concurrent.AbstractIdleService;
import com.hazelcast.core.HazelcastInstance;
import com.typesafe.config.Config;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.Match;
import io.vertx.ext.dropwizard.MatchType;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.INFO;
import javax.inject.Inject;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

/**
 *
 * @author alfio
 */
public final class VertxServiceImpl extends AbstractIdleService implements VertxService {

    private final Config config;

    private Vertx vertx;

    private VertxOptions vertxOptions;

    private final RunRightFastVerticleManager verticleManager;

    private final AppEventLogger appEventLogger;

    @Inject
    public VertxServiceImpl(
            @NonNull @VertxServiceConfig final Config config,
            @NonNull final Set<RunRightFastVerticleDeployment> deployments,
            @NonNull final AppEventLogger appEventLogger,
            @NonNull final EncryptionService encryptionService) {
        this.config = config;
        this.appEventLogger = appEventLogger;
        this.verticleManager = new RunRightFastVerticleManager(appEventLogger, encryptionService, deployments);
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    /**
     *
     * @return a copy of the VertxOptions that was used to create the Vertx instance
     */
    @Override
    public VertxOptions getVertxOptions() {
        final VertxOptions copy = new VertxOptions(vertxOptions);
        final MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
        if (metricsOptions != null) {
            if (metricsOptions instanceof DropwizardMetricsOptions) {
                copy.setMetricsOptions(new DropwizardMetricsOptions((DropwizardMetricsOptions) metricsOptions));
            } else {
                copy.setMetricsOptions(new DropwizardMetricsOptions(metricsOptions));
            }
        }
        return copy;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    protected void startUp() throws Exception {
        try {
            LOG.config(() -> ConfigUtils.renderConfig(config));
            this.vertxOptions = createVertxOptions();
            logVertxOptions();
            initVertx();
            hazelcastInstances.put(vertx, getHazelcastInstance());
            registerCodecs();
            deployVerticleManager();
            appEventLogger.accept(AppEvent.info(APP_STARTED).build());
        } catch (final Throwable t) {
            appEventLogger.accept(AppEvent.error(APP_START_FAILED)
                    .setException(t)
                    .build()
            );
            throw t;
        }
    }

    private void registerCodecs() {
        vertx.eventBus().registerDefaultCodec(co.runrightfast.vertx.core.messages.Void.class, new VoidMessageCodec());
    }

    private void logVertxOptions() {
        LOG.logp(CONFIG, getClass().getName(), "logVertxOptions", () -> {
            final JsonObject json = new JsonObject()
                    .put("BlockedThreadCheckInterval", vertxOptions.getBlockedThreadCheckInterval())
                    .put("ClusterHost", vertxOptions.getClusterHost())
                    .put("ClusterPingInterval", vertxOptions.getClusterPingInterval())
                    .put("ClusterPingReplyInterval", vertxOptions.getClusterPingReplyInterval())
                    .put("ClusterPort", vertxOptions.getClusterPort())
                    .put("EventLoopPoolSize", vertxOptions.getEventLoopPoolSize())
                    .put("HAGroup", vertxOptions.getHAGroup())
                    .put("InternalBlockingPoolSize", vertxOptions.getInternalBlockingPoolSize())
                    .put("MaxEventLoopExecuteTime", vertxOptions.getMaxEventLoopExecuteTime())
                    .put("MaxWorkerExecuteTime", vertxOptions.getMaxWorkerExecuteTime())
                    .put("QuorumSize", vertxOptions.getQuorumSize())
                    .put("WarningExceptionTime", vertxOptions.getWarningExceptionTime())
                    .put("WorkerPoolSize", vertxOptions.getWorkerPoolSize());

            final ClusterManager clusterManager = vertxOptions.getClusterManager();
            if (clusterManager != null) {
                json.put("clusterManagerClass", clusterManager.getClass().getName());
            }

            final MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
            if (metricsOptions != null) {
                json.put("MetricsOptions", toJsonObject(metricsOptions));
            }
            return json.encodePrettily();
        });
    }

    private void initVertx() {
        if (this.vertxOptions.isClustered()) {
            final CompletableFuture<AsyncResult<Vertx>> clusteredVertxResult = new CompletableFuture<>();
            Vertx.clusteredVertx(vertxOptions, clusteredVertxResult::complete);
            while (true) {
                try {
                    final AsyncResult<Vertx> result = clusteredVertxResult.get(10, TimeUnit.SECONDS);
                    if (result.succeeded()) {
                        this.vertx = result.result();
                        LOG.logp(INFO, getClass().getName(), "initVertx", "Vertx clustered instance has been created");
                    } else {
                        throw new RuntimeException("Failed to start a clustered Vertx instance", result.cause());
                    }
                    break;
                } catch (final ExecutionException ex) {
                    throw new RuntimeException("Failed to start a clustered Vertx instance", ex);
                } catch (final TimeoutException ex) {
                    LOG.logp(INFO, getClass().getName(), "initVertx", "Waiting for Vertx to start");
                } catch (final InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } else {
            this.vertx = Vertx.vertx(vertxOptions);
            LOG.logp(INFO, getClass().getName(), "initVertx", "Vertx instance has been created");
        }
    }

    private void deployVerticleManager() throws InterruptedException {
        final CompletableFuture<AsyncResult<String>> deployVerticleResult = new CompletableFuture<>();
        vertx.deployVerticle(verticleManager, deployVerticleResult::complete);
        while (true) {
            try {
                final AsyncResult<String> result = deployVerticleResult.get(10, TimeUnit.SECONDS);
                if (result.succeeded()) {
                    LOG.logp(INFO, getClass().getName(), "deployVerticleManager", result.result());
                } else {
                    throw new RuntimeException("Failed to deploy RunRightFastVerticleManager", result.cause());
                }
                break;
            } catch (final ExecutionException ex) {
                throw new RuntimeException("Failed to deploy RunRightFastVerticleManager", ex);
            } catch (final TimeoutException ex) {
                LOG.logp(INFO, getClass().getName(), "deployVerticleManager", "Waiting for RunRightFastVerticleManager deployment to complete");
            } catch (final InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private JsonObject toJsonObject(final MetricsOptions metricsOptions) {
        if (metricsOptions instanceof DropwizardMetricsOptions) {
            final DropwizardMetricsOptions dropwizardMetricsOptions = (DropwizardMetricsOptions) metricsOptions;
            final JsonObject json = new JsonObject().put("enabled", metricsOptions.isEnabled())
                    .put("jmxEnabled", dropwizardMetricsOptions.isJmxEnabled());

            toJsonObject(dropwizardMetricsOptions.getMonitoredEventBusHandlers()).ifPresent(jsonArray -> json.put("MonitoredEventBusHandlers", jsonArray));
            toJsonObject(dropwizardMetricsOptions.getMonitoredHttpClientUris()).ifPresent(jsonArray -> json.put("MonitoredHttpClientUris", jsonArray));
            toJsonObject(dropwizardMetricsOptions.getMonitoredHttpServerUris()).ifPresent(jsonArray -> json.put("MonitoredHttpServerUris", jsonArray));

            return json;
        } else {
            return new JsonObject().put("enabled", metricsOptions.isEnabled());
        }
    }

    private Optional<JsonArray> toJsonObject(final List<Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return Optional.empty();
        }

        final JsonArray jsonArray = new JsonArray();
        matches.stream()
                .map(match -> new JsonObject().put("value", match.getValue()).put("type", match.getType()))
                .forEach(jsonArray::add);

        return Optional.of(jsonArray);
    }

    @Override
    protected void shutDown() {
        if (vertx != null) {
            appEventLogger.accept(AppEvent.info(APP_STOPPING).build());
            final CompletableFuture<AsyncResult<Void>> closeResult = new CompletableFuture<>();
            vertx.close(closeResult::complete);
            while (true) {
                try {
                    closeResult.get(10, TimeUnit.SECONDS);
                    LOG.info("Vertx shutdown is complete.");
                    appEventLogger.accept(AppEvent.info(APP_STOPPED).build());
                    vertx = null;
                    vertxOptions = null;
                    break;
                } catch (final ExecutionException ex) {
                    appEventLogger.accept(AppEvent.info(APP_STOP_EXCEPTION).setException(ex).build());
                    throw new RuntimeException("shutdown failed", ex);
                } catch (final TimeoutException ex) {
                    LOG.logp(INFO, getClass().getName(), "shutDown", "Waiting for Vertx to shutdown");
                } catch (final InterruptedException ex) {
                    appEventLogger.accept(AppEvent.info(APP_STOP_EXCEPTION).setException(ex).build());
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private VertxOptions createVertxOptions() {
        final JsonObject vertxJsonObject = JsonUtils.toVertxJsonObject(ConfigUtils.toJsonObject(config.getConfig("VertxOptions")));
        vertxOptions = new VertxOptions(vertxJsonObject);
        getWeaveClusterHostIPAddress(config).ifPresent(vertxOptions::setClusterHost);

        if (vertxOptions.getMetricsOptions() != null && vertxOptions.getMetricsOptions().isEnabled()) {
            configureMetricsOptions();
        }

        if (vertxOptions.isClustered()) {
            configureClusterManager();
        }

        return vertxOptions;
    }

    /**
     * config structure:
     *
     * <code>
     * VertxOptions {
     *    metricsOptions {
     *       jmxEnabled = true
     *       jmxDomain = co.runrightfast.metrics
     *       eventbusHandlers = [
     *          { address="/eventbus-address-1", matchType="EQUALS"}
     *          { address="/eventbus-address-2/.*", matchType="REGEX"}
     *       ]
     *       monitoredHttpServerURIs = [
     *          { uri="/verticle/log-service", matchType="EQUALS"}
     *          { uri="/verticle/log-service/.*", matchType="REGEX"}
     *       ]
     *       monitoredHttpClientURIs = [
     *          { uri="/verticle/log-service", matchType="EQUALS"}
     *          { uri="/verticle/log-service/.*", matchType="REGEX"}
     *       ]
     *    }
     * }
     * </code>
     *
     */
    private void configureMetricsOptions() {
        final DropwizardMetricsOptions metricsOptions = new DropwizardMetricsOptions()
                .setEnabled(true)
                .setJmxEnabled(ConfigUtils.getBoolean(config, "VertxOptions", "metricsOptions", "jmxEnabled").orElse(Boolean.TRUE))
                .setRegistryName(VertxConstants.VERTX_METRIC_REGISTRY_NAME)
                .setJmxDomain(ConfigUtils.getString(config, "VertxOptions", "metricsOptions", "jmxDomain").orElse("co.runrightfast.vertx.metrics"));

        ConfigUtils.getConfigList(config, "VertxOptions", "metricsOptions", "eventbusHandlers").orElse(Collections.emptyList()).stream()
                .map(eventbusHandlerMatch -> {
                    final Match match = new Match();
                    match.setValue(eventbusHandlerMatch.getString("address"));
                    match.setType(MatchType.valueOf(eventbusHandlerMatch.getString("matchType")));
                    return match;
                }).forEach(metricsOptions::addMonitoredEventBusHandler);

        ConfigUtils.getConfigList(config, "VertxOptions", "metricsOptions", "monitoredHttpServerURIs").orElse(Collections.emptyList()).stream()
                .map(eventbusHandlerMatch -> {
                    final Match match = new Match();
                    match.setValue(eventbusHandlerMatch.getString("uri"));
                    match.setType(MatchType.valueOf(eventbusHandlerMatch.getString("matchType")));
                    return match;
                }).forEach(metricsOptions::addMonitoredHttpServerUri);

        ConfigUtils.getConfigList(config, "VertxOptions", "metricsOptions", "monitoredHttpClientURIs").orElse(Collections.emptyList()).stream()
                .map(eventbusHandlerMatch -> {
                    final Match match = new Match();
                    match.setValue(eventbusHandlerMatch.getString("uri"));
                    match.setType(MatchType.valueOf(eventbusHandlerMatch.getString("matchType")));
                    return match;
                }).forEach(metricsOptions::addMonitoredHttpClientUri);

        this.vertxOptions.setMetricsOptions(metricsOptions);
    }

    /**
     * The Hazelcast instance name will always start with "vertx". If an instance-name is specified, it will be appended to "vertx/", e.g., vertx/instance-name
     */
    private void configureClusterManager() {
        ConfigUtils.getConfig(config, "VertxOptions", "clusterManager", "hazelcast").map(c -> {
            final String hazelcastInstanceName = ConfigUtils.getString(c, "instance-name")
                    .map(name -> String.format("%s/%s", VERTX_HAZELCAST_INSTANCE_ID, name))
                    .orElse(VERTX_HAZELCAST_INSTANCE_ID);

            final com.hazelcast.config.Config hazelcastConfig = hazelcastConfigFactory(hazelcastInstanceName).apply(c.withFallback(config));
            return new HazelcastClusterManager(hazelcastConfig);
        }).ifPresent(vertxOptions::setClusterManager);
    }

    @Override
    public Map<String, RunRightFastVerticleDeployment> deployedVerticles() {
        if (this.verticleManager != null) {
            return this.verticleManager.getDeployedVerticles();
        }
        return Collections.emptyMap();
    }

    @Override
    public Set<RunRightFastVerticleDeployment> deployments() {
        return this.verticleManager.getDeployments();
    }

    @Override
    public Optional<HazelcastInstance> getHazelcastInstance() {
        if (!this.vertx.isClustered()) {
            return Optional.empty();
        }

        final ClusterManager clusterManager = this.vertxOptions.getClusterManager();
        if (clusterManager != null && clusterManager instanceof HazelcastClusterManager) {
            return Optional.of(((HazelcastClusterManager) clusterManager).getHazelcastInstance());
        }

        return Optional.empty();
    }

}
