/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.vertx.core.application.jmx.impl;

import co.runrightfast.vertx.core.application.RunRightFastVertxApplicationService;
import co.runrightfast.vertx.core.application.jmx.ApplicationMXBean;
import co.runrightfast.vertx.core.application.jmx.MBeanSupport;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import com.typesafe.config.Config;
import lombok.Builder;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@Builder
public final class ApplicationMXBeanImpl extends MBeanSupport implements ApplicationMXBean {

    private final RunRightFastVertxApplicationService appService;

    public ApplicationMXBeanImpl(@NonNull final RunRightFastVertxApplicationService appService) {
        super(ApplicationMXBean.class);
        this.appService = appService;
    }

    @Override
    public String getApplicationGroup() {
        return appService.getApp().runRightFastApplication().getApplicationId().getGroup();
    }

    @Override
    public String getApplicationName() {
        return appService.getApp().runRightFastApplication().getApplicationId().getName();
    }

    @Override
    public String getApplicationVersion() {
        return appService.getApp().runRightFastApplication().getApplicationId().getVersion();
    }

    @Override
    public String configAsJson() {
        final Config config = appService.getApp().runRightFastApplication().getConfig();
        return ConfigUtils.renderConfig(config, false, false, true);
    }

    @Override
    public String configAsHConf() {
        final Config config = appService.getApp().runRightFastApplication().getConfig();
        return ConfigUtils.renderConfig(config, false, false, false);
    }

    @Override
    public String configWithCommentsAndSourceInfo() {
        final Config config = appService.getApp().runRightFastApplication().getConfig();
        return ConfigUtils.renderConfig(config, true, true, false);
    }

    @Override
    public void shutdown() {
        ServiceUtils.stopAsync(appService);
    }

}
