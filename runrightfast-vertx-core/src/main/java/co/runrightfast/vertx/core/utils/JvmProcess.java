/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.vertx.core.utils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alfio
 */
public final class JvmProcess {

    private JvmProcess() {
    }

    public static final String HOST;

    public static final String JVM_ID = ManagementFactory.getRuntimeMXBean().getName();

    static {
        String _host = "UNKNOWN";
        try {
            _host = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException ex) {
            try {
                _host = InetAddress.getLocalHost().getHostAddress();
            } catch (final UnknownHostException ex2) {
                Logger.getLogger(JvmProcess.class.getSimpleName()).logp(Level.SEVERE, JvmProcess.class.getName(), "static-initializer", "failed to get host name", ex2);
            }
        }
        HOST = _host;
    }
}
