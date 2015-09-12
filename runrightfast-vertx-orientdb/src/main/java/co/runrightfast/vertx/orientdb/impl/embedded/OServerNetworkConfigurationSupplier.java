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
package co.runrightfast.vertx.orientdb.impl.embedded;

import static co.runrightfast.vertx.core.docker.weave.WeaveUtils.getWeaveClusterHostIPAddress;
import static co.runrightfast.vertx.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_ZERO;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerSocketFactoryConfiguration;
import com.orientechnologies.orient.server.network.OServerSSLSocketFactory;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_CLIENT_AUTH;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_KEYSTORE;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_KEYSTORE_PASSWORD;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_TRUSTSTORE;
import static com.orientechnologies.orient.server.network.OServerSSLSocketFactory.PARAM_NETWORK_SSL_TRUSTSTORE_PASSWORD;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 */
@Log
public class OServerNetworkConfigurationSupplier implements Supplier<OServerNetworkConfiguration> {

    public static final int DEFAULT_PORT = 2424;

    @Getter
    private Optional<NetworkSSLConfig> networkSSLConfig = Optional.empty();

    @Getter
    private int port = DEFAULT_PORT;

    public OServerNetworkConfigurationSupplier() {
    }

    public OServerNetworkConfigurationSupplier(final int port) {
        checkArgument(port > 0, MUST_BE_GREATER_THAN_ZERO, "port");
        this.port = port;
    }

    public OServerNetworkConfigurationSupplier(@NonNull final NetworkSSLConfig networkSSLConfig) {
        this.networkSSLConfig = Optional.of(networkSSLConfig);
        this.port = networkSSLConfig.getPort();
    }

    @Override
    public OServerNetworkConfiguration get() {
        if (networkSSLConfig.isPresent()) {
            return networkConfigWithSSL();
        }

        return networkConfig();
    }

    private OServerNetworkConfiguration networkConfig() {
        final OServerNetworkConfiguration network = new OServerNetworkConfiguration();
        network.protocols = ImmutableList.<OServerNetworkProtocolConfiguration>builder()
                .add(new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName()))
                .build();
        final OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = getWeaveClusterHostIPAddress().orElse("0.0.0.0");
        log.info(String.format("binaryListener.ipAddress = %s", binaryListener.ipAddress));
        binaryListener.protocol = "binary";
        binaryListener.portRange = Integer.toString(port);
        binaryListener.socket = "default";
        network.listeners = ImmutableList.of(binaryListener);
        return network;
    }

    private OServerNetworkConfiguration networkConfigWithSSL() {
        final OServerNetworkConfiguration network = new OServerNetworkConfiguration();

        final OServerSocketFactoryConfiguration sslConfig = new OServerSocketFactoryConfiguration("ssl", OServerSSLSocketFactory.class.getName());
        final NetworkSSLConfig ssl = networkSSLConfig.get();
        final ImmutableList.Builder<OServerParameterConfiguration> parameters = ImmutableList.<OServerParameterConfiguration>builder()
                .add(new OServerParameterConfiguration(PARAM_NETWORK_SSL_KEYSTORE, ssl.getServerKeyStorePath().toString()))
                .add(new OServerParameterConfiguration(PARAM_NETWORK_SSL_KEYSTORE_PASSWORD, ssl.getServerKeyStorePassword()));

        ssl.getServerTrustStorePath().ifPresent(trustStorePath -> {
            parameters.add(new OServerParameterConfiguration(PARAM_NETWORK_SSL_CLIENT_AUTH, "true"))
                    .add(new OServerParameterConfiguration(PARAM_NETWORK_SSL_TRUSTSTORE, trustStorePath.toString()))
                    .add(new OServerParameterConfiguration(PARAM_NETWORK_SSL_TRUSTSTORE_PASSWORD, ssl.getServerTrustStorePassword().get()));
        });

        sslConfig.parameters = parameters.build().stream().toArray(OServerParameterConfiguration[]::new);
        network.sockets = ImmutableList.of(sslConfig);

        network.protocols = ImmutableList.of(
                new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName())
        );

        final OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = getWeaveClusterHostIPAddress().orElse("0.0.0.0");
        binaryListener.protocol = "binary";
        binaryListener.portRange = Integer.toString(port);
        binaryListener.socket = sslConfig.name;
        network.listeners = ImmutableList.of(binaryListener);

        return network;
    }

}
