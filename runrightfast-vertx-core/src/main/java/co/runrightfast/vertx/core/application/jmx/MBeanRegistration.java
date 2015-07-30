/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.vertx.core.application.jmx;

import com.google.common.base.MoreObjects;

/**
 *
 * @author alfio
 */
public final class MBeanRegistration<A> {

    final A mbean;
    final Class<?> mbeanType;

    public MBeanRegistration(final A mbean, final Class<A> mbeanType) {
        this.mbean = mbean;
        this.mbeanType = mbeanType;
    }

    public MBeanRegistration(final A mbean, final Class<A> mbeanType, final Class<?> registeredType) {
        this.mbean = mbean;
        this.mbeanType = registeredType;
    }

    public A getMbean() {
        return mbean;
    }

    public Class<?> getMbeanType() {
        return mbeanType;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mbean.class", mbean.getClass().getName())
                .add("mbeanType", mbeanType.getName())
                .toString();
    }

}
