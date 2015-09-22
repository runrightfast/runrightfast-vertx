/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core.jmx;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public abstract class MBeanSupport implements MBeanRegistration {

    protected final Logger log;

    protected final String CLASS_NAME = getClass().getName();

    /**
     *
     * @param mbeanInterface used to create a logger for the MBean based on its interface
     */
    protected MBeanSupport(@NonNull final Class<?> mbeanInterface) {
        this.log = Logger.getLogger(mbeanInterface.getName());
    }

    @Override
    public ObjectName preRegister(final MBeanServer server, final ObjectName name) {
        log.entering(CLASS_NAME, "preRegister");
        return name;
    }

    @Override
    public void postRegister(final Boolean registrationDone) {
        if (registrationDone) {
            log.logp(INFO, CLASS_NAME, "postRegister", "success");
        } else {
            log.logp(SEVERE, CLASS_NAME, "postRegister", "failure");
        }
    }

    @Override
    public void preDeregister() {
        log.entering(CLASS_NAME, "preDeregister");
    }

    @Override
    public void postDeregister() {
        log.entering(CLASS_NAME, "postDeregister");
    }

}
