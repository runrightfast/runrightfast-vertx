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
import static com.google.common.base.Preconditions.checkArgument;
import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 */
public interface JmxUtils {

    static ObjectName applicationMBeanObjectName(final String domain, @NonNull final Class<?> mbeanType) {
        checkArgument(isNotBlank(domain));
        try {
            return ObjectName.getInstance(domain, "type", mbeanType.getSimpleName());
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

}
