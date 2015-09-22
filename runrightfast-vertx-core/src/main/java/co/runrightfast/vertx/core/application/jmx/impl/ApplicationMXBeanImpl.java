/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.vertx.core.application.jmx.impl;

import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.application.RunRightFastVertxApplicationLauncher;
import co.runrightfast.vertx.core.application.jmx.ApplicationMXBean;
import co.runrightfast.core.jmx.MBeanSupport;
import co.runrightfast.core.utils.ConfigUtils;
import co.runrightfast.core.utils.ServiceUtils;
import com.typesafe.config.Config;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public final class ApplicationMXBeanImpl extends MBeanSupport implements ApplicationMXBean {

    private final RunRightFastVertxApplicationLauncher appLauncher;

    private final String[] verticleIds;

    public ApplicationMXBeanImpl(@NonNull final RunRightFastVertxApplicationLauncher appLauncher) {
        super(ApplicationMXBean.class);
        this.appLauncher = appLauncher;
        this.verticleIds = appLauncher.getApp().vertxService().deployments().stream().map(deployment -> {
            final RunRightFastVerticleId id = deployment.getRunRightFastVerticleId();
            return id.toJson().toString();
        }).toArray(String[]::new);
    }

    @Override
    public String getApplicationGroup() {
        return appLauncher.getApp().runRightFastApplication().getApplicationId().getGroup();
    }

    @Override
    public String getApplicationName() {
        return appLauncher.getApp().runRightFastApplication().getApplicationId().getName();
    }

    @Override
    public String getApplicationVersion() {
        return appLauncher.getApp().runRightFastApplication().getApplicationId().getVersion();
    }

    @Override
    public String configAsJson() {
        final Config config = appLauncher.getApp().runRightFastApplication().getConfig();
        return ConfigUtils.renderConfig(config, false, false, true);
    }

    @Override
    public String configAsHConf() {
        final Config config = appLauncher.getApp().runRightFastApplication().getConfig();
        return ConfigUtils.renderConfig(config, false, false, false);
    }

    @Override
    public String configWithCommentsAndSourceInfo() {
        final Config config = appLauncher.getApp().runRightFastApplication().getConfig();
        return ConfigUtils.renderConfig(config, true, true, false);
    }

    @Override
    public void shutdown() {
        ServiceUtils.stopAsync(appLauncher);
    }

    @Override
    public String[] getVerticleIds() {
        return verticleIds;
    }

}
