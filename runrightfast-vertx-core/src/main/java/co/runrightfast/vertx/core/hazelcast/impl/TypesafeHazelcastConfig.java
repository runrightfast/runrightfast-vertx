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
package co.runrightfast.vertx.core.hazelcast.impl;

import co.runrightfast.core.ConfigurationException.ConfigurationExceptionSupplier;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ListConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.MaxSizeConfig.MaxSizePolicy;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.config.MemberGroupConfig;
import com.hazelcast.config.MultiMapConfig;
import com.hazelcast.config.MultiMapConfig.ValueCollectionType;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.PartitionGroupConfig;
import com.hazelcast.config.PartitionGroupConfig.MemberGroupType;
import static com.hazelcast.config.PartitionGroupConfig.MemberGroupType.CUSTOM;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.SemaphoreConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.SetConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.TopicConfig;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * Subclasses can apply additional configuration via {@link TypesafeHazelcastConfig#applyAdditionalConfiguration(com.hazelcast.config.Config)}
 *
 * @author alfio
 */
public class TypesafeHazelcastConfig {

    @Getter
    protected final Config hazelcastConfig;

    /**
     * @param name Hazelcast instance name
     * @param config TypeSafe typeSafeConfig
     * @param serializerConfigs can be empty
     */
    public TypesafeHazelcastConfig(@NonNull final String name, @NonNull final com.typesafe.config.Config config, @NonNull final Set<SerializerConfig> serializerConfigs) {
        this.hazelcastConfig = createHazelcastConfig(
                name,
                config,
                serializerConfigs.isEmpty() ? Optional.empty() : Optional.of(ImmutableSet.copyOf(serializerConfigs))
        );
    }

    /**
     * @param name Hazelcast instance name
     * @param config TypeSafe typeSafeConfig
     */
    public TypesafeHazelcastConfig(@NonNull final String name, @NonNull final com.typesafe.config.Config config) {
        this.hazelcastConfig = createHazelcastConfig(
                name,
                config,
                Optional.empty()
        );
    }

    private Config createHazelcastConfig(@NonNull final String name, @NonNull final com.typesafe.config.Config typeSafeConfig, @NonNull final Optional<Set<SerializerConfig>> serializerConfigs) {
        checkArgument(StringUtils.isNotBlank(name));
        final Config _hazelcastConfig = new Config();
        _hazelcastConfig.setInstanceName(name);

        _hazelcastConfig.setGroupConfig(ConfigUtils.getConfig(typeSafeConfig, "group-config")
                .map(this::groupConfig)
                .orElseThrow(new ConfigurationExceptionSupplier("group-config is required"))
        );

        _hazelcastConfig.setNetworkConfig(ConfigUtils.getConfig(typeSafeConfig, "network-config")
                .map(this::networkConfig)
                .orElseThrow(new ConfigurationExceptionSupplier("network-config is required"))
        );

        ConfigUtils.getConfigList(typeSafeConfig, "map-configs").ifPresent(mapConfigs -> {
            mapConfigs.stream().map(this::mapConfig).forEach(_hazelcastConfig::addMapConfig);
        });

        ConfigUtils.getConfigList(typeSafeConfig, "multi-map-configs").ifPresent(mapConfigs -> {
            mapConfigs.stream().map(this::multiMapConfig).forEach(_hazelcastConfig::addMultiMapConfig);
        });

        ConfigUtils.getConfigList(typeSafeConfig, "queue-configs").ifPresent(queueConfigs -> {
            queueConfigs.stream().map(this::queueConfig).forEach(_hazelcastConfig::addQueueConfig);
        });

        ConfigUtils.getConfigList(typeSafeConfig, "topic-configs").ifPresent(queueConfigs -> {
            queueConfigs.stream().map(this::topicConfig).forEach(_hazelcastConfig::addTopicConfig);
        });

        ConfigUtils.getConfigList(typeSafeConfig, "list-configs").ifPresent(listConfigs -> {
            listConfigs.stream().map(this::listConfig).forEach(_hazelcastConfig::addListConfig);
        });

        ConfigUtils.getConfigList(typeSafeConfig, "set-configs").ifPresent(listConfigs -> {
            listConfigs.stream().map(this::getSetConfig).forEach(_hazelcastConfig::addSetConfig);
        });

        ConfigUtils.getConfigList(typeSafeConfig, "semaphore-configs").ifPresent(semaphoreConfigs -> {
            semaphoreConfigs.stream().map(this::semaphoreConfig).forEach(_hazelcastConfig::addSemaphoreConfig);
        });

        ConfigUtils.getConfigList(typeSafeConfig, "executor-configs").ifPresent(executorConfigs -> {
            executorConfigs.stream().map(this::executorConfig).forEach(_hazelcastConfig::addExecutorConfig);
        });

        _hazelcastConfig.setSerializationConfig(new SerializationConfig());
        serializerConfigs.ifPresent(configs -> configs.stream().forEach(serializerConfig -> _hazelcastConfig.getSerializationConfig().addSerializerConfig(serializerConfig)));

        ConfigUtils.getConfigList(typeSafeConfig, "partition-group-config").ifPresent(partitionGroupConfig -> {
            partitionGroupConfig.stream().map(this::partitionConfig).forEach(_hazelcastConfig::setPartitionGroupConfig);
        });

        // Application manages the lifecycle and registers a shutdown hook - we want to ensure this is the last service that is stopped
        _hazelcastConfig.setProperty("hazelcast.shutdownhook.enabled", "false");
        // mapping hazelcast.jmx.enabled to hazelcast.jmx because using Typesafe typeSafeConfig, hazelcast.jmx is an object and cannot be set to a boolean
        ConfigUtils.getBoolean(typeSafeConfig, "properties", "hazelcast", "jmx", "enabled").ifPresent(jmxEnabled -> _hazelcastConfig.setProperty("hazelcast.jmx", Boolean.toString(jmxEnabled)));

        ConfigUtils.getConfig(typeSafeConfig, "properties").ifPresent(properties -> {
            _hazelcastConfig.setProperties(ConfigUtils.toProperties(properties));
        });

        ConfigUtils.getConfig(typeSafeConfig, "member-attribute-config")
                .map(this::memberAttributeConfig)
                .ifPresent(_hazelcastConfig::setMemberAttributeConfig);

        applyAdditionalConfiguration(_hazelcastConfig);
        return _hazelcastConfig;
    }

    private MemberAttributeConfig memberAttributeConfig(final com.typesafe.config.Config c) {
        final MemberAttributeConfig memberAttributeConfig = new MemberAttributeConfig();
        memberAttributeConfig.setAttributes(c.root().unwrapped());
        return memberAttributeConfig;
    }

    private PartitionGroupConfig partitionConfig(final com.typesafe.config.Config c) {
        final PartitionGroupConfig partitionGroupConfig = new PartitionGroupConfig();
        partitionGroupConfig.setEnabled(true);
        partitionGroupConfig.setGroupType(ConfigUtils.getString(c, "group-type").map(MemberGroupType::valueOf).orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        if (partitionGroupConfig.getGroupType().equals(CUSTOM)) {
            ConfigUtils.getConfigList(c, "member-group-configs").ifPresent(memberGroupConfigs -> {
                memberGroupConfigs.stream().forEach(memberGroupConfig -> {
                    final MemberGroupConfig _memberGroupConfig = new MemberGroupConfig();
                    _memberGroupConfig.setInterfaces(ConfigUtils.getStringList(memberGroupConfig, "interfaces").orElseThrow(new ConfigurationExceptionSupplier("no interfaces were defined for the MemberGroupConfig")));
                    partitionGroupConfig.addMemberGroupConfig(_memberGroupConfig);
                });
            });
        }
        return partitionGroupConfig;
    }

    private ExecutorConfig executorConfig(final com.typesafe.config.Config c) {
        final ExecutorConfig executorConfig = new ExecutorConfig();
        executorConfig.setName(ConfigUtils.getString(c, "name").orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        ConfigUtils.getInt(c, "pool-size").ifPresent(executorConfig::setPoolSize);
        ConfigUtils.getInt(c, "queue-capacity").ifPresent(executorConfig::setQueueCapacity);
        ConfigUtils.getBoolean(c, "statistics-enabled").ifPresent(executorConfig::setStatisticsEnabled);
        return executorConfig;
    }

    private SemaphoreConfig semaphoreConfig(final com.typesafe.config.Config c) {
        final SemaphoreConfig semaphoreConfig = new SemaphoreConfig();
        semaphoreConfig.setName(ConfigUtils.getString(c, "name").orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        ConfigUtils.getInt(c, "backup-count").ifPresent(semaphoreConfig::setBackupCount);
        ConfigUtils.getInt(c, "async-backup-count").ifPresent(semaphoreConfig::setAsyncBackupCount);
        ConfigUtils.getInt(c, "initial-permits").ifPresent(semaphoreConfig::setInitialPermits);
        return semaphoreConfig;
    }

    private SetConfig getSetConfig(final com.typesafe.config.Config c) {
        final SetConfig setConfig = new SetConfig();
        setConfig.setName(ConfigUtils.getString(c, "name").orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        ConfigUtils.getInt(c, "backup-count").ifPresent(setConfig::setBackupCount);
        ConfigUtils.getInt(c, "async-backup-count").ifPresent(setConfig::setAsyncBackupCount);
        ConfigUtils.getInt(c, "max-size").ifPresent(setConfig::setMaxSize);
        ConfigUtils.getBoolean(c, "statistics-enabled").ifPresent(setConfig::setStatisticsEnabled);
        return setConfig;
    }

    private ListConfig listConfig(final com.typesafe.config.Config c) {
        final ListConfig listConfig = new ListConfig();
        listConfig.setName(ConfigUtils.getString(c, "name").orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        ConfigUtils.getInt(c, "backup-count").ifPresent(listConfig::setBackupCount);
        ConfigUtils.getInt(c, "async-backup-count").ifPresent(listConfig::setAsyncBackupCount);
        ConfigUtils.getInt(c, "max-size").ifPresent(listConfig::setMaxSize);
        ConfigUtils.getBoolean(c, "statistics-enabled").ifPresent(listConfig::setStatisticsEnabled);
        return listConfig;
    }

    private TopicConfig topicConfig(final com.typesafe.config.Config c) {
        final TopicConfig topicConfig = new TopicConfig();
        topicConfig.setName(ConfigUtils.getString(c, "name").orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        ConfigUtils.getBoolean(c, "global-ordering-enabled").ifPresent(topicConfig::setGlobalOrderingEnabled);
        ConfigUtils.getBoolean(c, "statistics-enabled").ifPresent(topicConfig::setStatisticsEnabled);
        return topicConfig;
    }

    private QueueConfig queueConfig(final com.typesafe.config.Config c) {
        final QueueConfig queueConfig = new QueueConfig();
        queueConfig.setName(ConfigUtils.getString(c, "name").orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        ConfigUtils.getInt(c, "backup-count").ifPresent(queueConfig::setBackupCount);
        ConfigUtils.getInt(c, "async-backup-count").ifPresent(queueConfig::setAsyncBackupCount);
        ConfigUtils.getInt(c, "empty-queue-ttl").ifPresent(queueConfig::setEmptyQueueTtl);
        ConfigUtils.getInt(c, "max-size").ifPresent(queueConfig::setMaxSize);
        ConfigUtils.getBoolean(c, "statistics-enabled").ifPresent(queueConfig::setStatisticsEnabled);
        return queueConfig;
    }

    private MultiMapConfig multiMapConfig(final com.typesafe.config.Config c) {
        final MultiMapConfig multiMapConfig = new MultiMapConfig();
        multiMapConfig.setName(ConfigUtils.getString(c, "name").orElseThrow(new ConfigurationExceptionSupplier("name is required")));
        ConfigUtils.getInt(c, "backup-count").ifPresent(multiMapConfig::setBackupCount);
        ConfigUtils.getInt(c, "async-backup-count").ifPresent(multiMapConfig::setAsyncBackupCount);
        ConfigUtils.getString(c, "value-collection-type").map(ValueCollectionType::valueOf).ifPresent(multiMapConfig::setValueCollectionType);
        ConfigUtils.getBoolean(c, "binary").ifPresent(multiMapConfig::setBinary);
        ConfigUtils.getBoolean(c, "statistics-enabled").ifPresent(multiMapConfig::setStatisticsEnabled);
        return multiMapConfig;
    }

    private MapConfig mapConfig(final com.typesafe.config.Config mapConfig) {
        final MapConfig c = new MapConfig();
        c.setName(ConfigUtils.getString(mapConfig, "name").orElseThrow(new ConfigurationExceptionSupplier("map name is required")));
        ConfigUtils.getString(mapConfig, "in-memory-format").map(InMemoryFormat::valueOf).ifPresent(c::setInMemoryFormat);
        ConfigUtils.getInt(mapConfig, "backup-count").ifPresent(c::setBackupCount);
        ConfigUtils.getInt(mapConfig, "async-backup-count").ifPresent(c::setAsyncBackupCount);
        ConfigUtils.getBoolean(mapConfig, "read-backup-data").ifPresent(c::setReadBackupData);
        ConfigUtils.getInt(mapConfig, "time-to-live-seconds").ifPresent(c::setTimeToLiveSeconds);
        ConfigUtils.getInt(mapConfig, "max-idle-seconds").ifPresent(c::setMaxIdleSeconds);
        ConfigUtils.getString(mapConfig, "eviction-policy").map(EvictionPolicy::valueOf).ifPresent(c::setEvictionPolicy);
        ConfigUtils.getConfig(mapConfig, "max-size-config").map(this::maxSizeConfig).ifPresent(c::setMaxSizeConfig);
        ConfigUtils.getInt(mapConfig, "eviction-percentage").ifPresent(c::setEvictionPercentage);
        ConfigUtils.getString(mapConfig, "merge-policy").ifPresent(c::setMergePolicy);
        ConfigUtils.getBoolean(mapConfig, "statistics-enabled").ifPresent(c::setStatisticsEnabled);
        return c;
    }

    private MaxSizeConfig maxSizeConfig(final com.typesafe.config.Config c) {
        final MaxSizeConfig maxSizeConfig = new MaxSizeConfig();
        maxSizeConfig.setSize(ConfigUtils.getInt(c, "size").orElseThrow(new ConfigurationExceptionSupplier("map max size is required")));
        ConfigUtils.getString(c, "max-size-policy").map(MaxSizePolicy::valueOf).ifPresent(maxSizeConfig::setMaxSizePolicy);
        return maxSizeConfig;
    }

    private NetworkConfig networkConfig(final com.typesafe.config.Config config) {
        final NetworkConfig networkConfig = new NetworkConfig()
                .setReuseAddress(ConfigUtils.getBoolean(config, "reuse-address").orElse(true))
                .setPort(ConfigUtils.getInt(config, "port").orElse(5701));

        ConfigUtils.getInt(config, "port-count").ifPresent(networkConfig::setPortCount);
        ConfigUtils.getBoolean(config, "port-auto-increment").ifPresent(networkConfig::setPortAutoIncrement);
        ConfigUtils.getStringList(config, "outbound-port-definitions").ifPresent(outboundPortDefinitions -> {
            outboundPortDefinitions.stream().forEach(networkConfig::addOutboundPortDefinition);
        });

        networkConfig.setJoin(joinConfig(config));
        ConfigUtils.getStringList(config, "interfaces").ifPresent(interfaces -> {
            final InterfacesConfig interfaceConfig = networkConfig.getInterfaces();
            interfaces.stream().forEach(interfaceConfig::addInterface);
        });

        return networkConfig;
    }

    /**
     * Only multicast and tcpip are currently supported.
     *
     * @return JoinConfig
     */
    private JoinConfig joinConfig(final com.typesafe.config.Config config) {
        final JoinConfig join = new JoinConfig();
        join.setTcpIpConfig(tcpIpConfig(config));
        join.setMulticastConfig(multicastConfig(config));
        return join;
    }

    private TcpIpConfig tcpIpConfig(final com.typesafe.config.Config config) {
        return ConfigUtils.getConfig(config, "join-config", "tcpip")
                .map(tcpipConfig -> {
                    final TcpIpConfig tcpip = new TcpIpConfig();
                    tcpip.setEnabled(true);
                    ConfigUtils.getString(tcpipConfig, "required-member").ifPresent(tcpip::setRequiredMember);
                    ConfigUtils.getStringList(tcpipConfig, "members").ifPresent(members -> {
                        members.stream().forEach(tcpip::addMember);
                    });
                    ConfigUtils.getInt(tcpipConfig, "connection-timeout-seconds").ifPresent(tcpip::setConnectionTimeoutSeconds);

                    return tcpip;
                })
                .orElseGet(() -> new TcpIpConfig().setEnabled(false));
    }

    private MulticastConfig multicastConfig(final com.typesafe.config.Config config) {
        return ConfigUtils.getConfig(config, "join-config", "multicast")
                .map(multicastConfig -> {
                    final MulticastConfig multicast = new MulticastConfig();
                    multicast.setEnabled(true);
                    ConfigUtils.getString(multicastConfig, "multicast-group").ifPresent(multicast::setMulticastGroup);
                    ConfigUtils.getInt(multicastConfig, "multicast-port").ifPresent(multicast::setMulticastPort);
                    ConfigUtils.getInt(multicastConfig, "multicast-time-to-live").ifPresent(multicast::setMulticastTimeToLive);
                    ConfigUtils.getInt(multicastConfig, "multicast-timeout-seconds").ifPresent(multicast::setMulticastTimeoutSeconds);
                    ConfigUtils.getStringList(multicastConfig, "trusted-interfaces").ifPresent(trustedInterfaces -> {
                        trustedInterfaces.stream().forEach(multicast::addTrustedInterface);
                    });
                    return multicast;
                })
                .orElseGet(() -> new MulticastConfig().setEnabled(false));
    }

    private GroupConfig groupConfig(final com.typesafe.config.Config config) {
        return new GroupConfig(config.getString("name"), config.getString("password"));
    }

    /**
     * Is invoked after all of the typeSafeConfig specified in the Typesafe typeSafeConfig is applied. This provides a hook to be able to configure aspects that
     * are not supported by the Typesafe typeSafeConfig.
     *
     * By default, this is a no-op method.
     *
     * @param hazelcastConfig typeSafeConfig
     */
    protected void applyAdditionalConfiguration(final Config hazelcastConfig) {
    }

}
