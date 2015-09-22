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
package co.runrightfast.vertx.core.application;

import co.runrightfast.core.utils.ConfigUtils;
import static co.runrightfast.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import co.runrightfast.core.utils.JmxUtils;
import static co.runrightfast.core.utils.JmxUtils.RUNRIGHTFAST_JMX_DOMAIN;
import com.typesafe.config.Config;
import javax.management.ObjectName;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
@Builder
public final class RunRightFastApplication {

    @NonNull
    @Getter
    private final ApplicationId applicationId;

    @NonNull
    @Getter
    private final Config config;

    public String getJmxDefaultDomain() {
        return ConfigUtils.getString(config, CONFIG_NAMESPACE, "jmx", "default-domain").orElse(RUNRIGHTFAST_JMX_DOMAIN);
    }

    public ObjectName applicationMBeanObjectName() {
        return applicationMBeanObjectName(RunRightFastApplication.class);
    }

    /**
     *
     * @param mbeanType used to provide the MBean type for the ObjectName
     * @return ObjectName
     */
    public ObjectName applicationMBeanObjectName(@NonNull final Class<?> mbeanType) {
        return JmxUtils.applicationMBeanObjectName(getJmxDefaultDomain(), mbeanType);
    }

    /**
     * registers an MBean within the application's JMX domain
     *
     * @param mbean MBean instance
     * @param mbeanType the class name is used to set the type attribute
     */
    public void registerApplicationMBean(@NonNull final Object mbean, @NonNull final Class<?> mbeanType) {
        JmxUtils.registerApplicationMBean(getJmxDefaultDomain(), mbean, mbeanType);
    }
}
