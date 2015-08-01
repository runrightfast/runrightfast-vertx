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
package co.runrightfast.vertx.core.utils;

import co.runrightfast.core.ApplicationException;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.application.jmx.MBeanRegistration;
import static com.google.common.base.Preconditions.checkArgument;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public interface JmxUtils {

    static final String RUNRIGHTFAST_JMX_DOMAIN = "co.runrightfast";

    static String verticleJmxDomain(@NonNull final RunRightFastVerticleId verticleId, final String... subDomains) {
        final StringBuilder sb = new StringBuilder(80)
                .append(RUNRIGHTFAST_JMX_DOMAIN)
                .append(String.format("/%s-%s-%s",
                                verticleId.getGroup(),
                                verticleId.getName(),
                                verticleId.getVersion()
                        ));
        if (ArrayUtils.isNotEmpty(subDomains)) {
            Arrays.stream(subDomains).forEach(subDomain -> sb.append('/').append(subDomain));
        }
        return sb.toString();
    }

    /**
     *
     * @param domain JMX domain
     * @param mbeanType used to add a type attribute
     * @return ObjectName
     */
    static ObjectName applicationMBeanObjectName(final String domain, @NonNull final Class<?> mbeanType) {
        checkArgument(isNotBlank(domain));
        try {
            return ObjectName.getInstance(domain, "type", mbeanType.getSimpleName());
        } catch (final MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    static ObjectName applicationMBeanObjectName(final String domain, @NonNull final Class<?> mbeanType, final String name) {
        checkArgument(isNotBlank(domain));
        checkArgument(isNotBlank(name));
        try {
            @SuppressWarnings("UseOfObsoleteCollectionType")
            final Hashtable<String, String> attributes = new Hashtable<>();
            attributes.put("type", mbeanType.getSimpleName());
            attributes.put("name", name);
            return ObjectName.getInstance(domain, attributes);
        } catch (final MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    static void registerApplicationMBean(final String domain, @NonNull final Object mbean, final Class<?> mbeanType) {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mbeanServer.registerMBean(mbean, applicationMBeanObjectName(domain, mbeanType));
        } catch (final InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex) {
            throw new ApplicationException(String.format("registerApplicationMBean() failed for: %s", mbean.getClass().getName()), ex);
        }
    }

    static void unregisterApplicationMBean(final String domain, final Class<?> mbeanType) {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            mbeanServer.unregisterMBean(applicationMBeanObjectName(domain, mbeanType));
        } catch (final InstanceNotFoundException | MBeanRegistrationException ex) {
            final Logger log = Logger.getLogger(JmxUtils.class.getName());
            log.logp(WARNING, JmxUtils.class.getName(), "unregisterApplicationMBean", "failed", ex);
        }
    }

    static void registerMBeans(final String domain, final Set<MBeanRegistration<?>> mBeanRegistrations) {
        if (CollectionUtils.isEmpty(mBeanRegistrations)) {
            return;
        }
        checkArgument(StringUtils.isNotBlank(domain));
        mBeanRegistrations.stream().forEach(reg -> registerApplicationMBean(domain, reg.getMbean(), reg.getClass()));
    }

    static void unregisterMBeans(final String domain, final Set<MBeanRegistration<?>> mBeanRegistrations) {
        if (CollectionUtils.isEmpty(mBeanRegistrations)) {
            return;
        }
        checkArgument(StringUtils.isNotBlank(domain));
        mBeanRegistrations.stream().forEach(reg -> unregisterApplicationMBean(domain, reg.getMbeanType()));
    }

}
