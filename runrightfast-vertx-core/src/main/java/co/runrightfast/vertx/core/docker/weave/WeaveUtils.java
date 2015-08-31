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
package co.runrightfast.vertx.core.docker.weave;

import co.runrightfast.vertx.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Optional;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;

/**
 *
 * @author alfio
 */
public interface WeaveUtils {

    static final Logger LOG = Logger.getLogger(WeaveUtils.class.getName());

    static Optional<String> getWeaveClusterHostIPAddress(final Config config) {
        if (!ConfigUtils.getBoolean(config, "weave", "enabled").orElse(Boolean.FALSE)) {
            return Optional.empty();
        }
        try {
            final String interfaceName = ConfigUtils.getString(config, "weave", "network-interface").orElse("ethwe");
            final NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface == null) {
                LOG.log(WARNING, "Weave network interface was not found : {0}", interfaceName);
                return Optional.empty();
            }
            return networkInterface.getInterfaceAddresses().stream()
                    .filter(address -> address.getAddress() instanceof Inet4Address)
                    .findFirst()
                    .map(address -> address.getAddress().getHostAddress());
        } catch (final SocketException ex) {
            LOG.logp(SEVERE, WeaveUtils.class.getName(), "getWeaveClusterHostIPAddress", "failed", ex);
            return Optional.empty();
        }
    }

    static Optional<String> getWeaveClusterHostIPAddress(final String interfaceName) {
        try {
            final NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface == null) {
                LOG.log(WARNING, "Weave network interface was not found : {0}", interfaceName);
                return Optional.empty();
            }
            return networkInterface.getInterfaceAddresses().stream()
                    .filter(address -> address.getAddress() instanceof Inet4Address)
                    .findFirst()
                    .map(address -> address.getAddress().getHostAddress());
        } catch (final SocketException ex) {
            LOG.logp(SEVERE, WeaveUtils.class.getName(), "getWeaveClusterHostIPAddress", "failed", ex);
            return Optional.empty();
        }
    }

    /**
     * Using 'ethwe" as the interface name
     *
     * @return weave host ip address
     */
    static Optional<String> getWeaveClusterHostIPAddress() {
        return getWeaveClusterHostIPAddress("ethwe");
    }
}
