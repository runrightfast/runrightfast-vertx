/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.vertx.core.application.jmx;

import javax.management.MXBean;

/**
 *
 * @author alfio
 *
 */
@MXBean
public interface ApplicationMXBean {

    String getApplicationGroup();

    String getApplicationName();

    String getApplicationVersion();

    /**
     *
     * @return the application config in JSON format
     */
    String configAsJson();

    /**
     *
     * @return the application config in TypeSafe HCONF format
     */
    String configAsHConf();

    /**
     *
     * This is useful to help troubleshoot a config issue.
     *
     * @return the application config in TypeSafe HCONF format including comments and where the config properties, i.e., JVM system properties or which config
     * file
     */
    String configWithCommentsAndSourceInfo();

    String[] getVerticleIds();

    /**
     * Shutdown the application
     */
    void shutdown();

}
