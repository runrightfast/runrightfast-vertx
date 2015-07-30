/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.vertx.core.application.jmx.impl;

import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.core.application.jmx.ApplicationMXBean;
import co.runrightfast.vertx.core.application.jmx.MBeanSupport;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import java.util.concurrent.CountDownLatch;
import lombok.Builder;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@Builder
public final class ApplicationMXBeanImpl extends MBeanSupport implements ApplicationMXBean {

    private final ApplicationId applicationId;

    private final Config config;

    private final CountDownLatch applicationShutdownLatch;

    public ApplicationMXBeanImpl(
            @NonNull final ApplicationId applicationId,
            @NonNull final Config config,
            @NonNull final CountDownLatch applicationShutdownLatch) {
        super(ApplicationMXBean.class);
        this.config = config;
        this.applicationId = applicationId;
        this.applicationShutdownLatch = applicationShutdownLatch;
    }

    @Override
    public String getApplicationGroup() {
        return applicationId.getGroup();
    }

    @Override
    public String getApplicationName() {
        return applicationId.getName();
    }

    @Override
    public String getApplicationVersion() {
        return applicationId.getVersion();
    }

    @Override
    public String configAsJson() {
        return ConfigUtils.renderConfig(config, false, false, true);
    }

    @Override
    public String configAsHConf() {
        return ConfigUtils.renderConfig(config, false, false, false);
    }

    @Override
    public String configWithCommentsAndSourceInfo() {
        return ConfigUtils.renderConfig(config, true, true, false);
    }

    @Override
    public void shutdown() {
        applicationShutdownLatch.countDown();
    }

}
