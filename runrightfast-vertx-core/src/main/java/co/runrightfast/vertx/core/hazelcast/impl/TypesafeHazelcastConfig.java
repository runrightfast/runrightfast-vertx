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
import static com.google.common.base.Preconditions.checkNotNull;
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
import org.apache.commons.lang3.StringUtils;

/**
 *
 * Subclasses can apply additional configuration via {@link TypesafeHazelcastConfig#applyAdditionalConfiguration(com.hazelcast.config.Config)}
 *
 * @author alfio
 */
public class TypesafeHazelcastConfig {

    protected final String name;

    protected final com.typesafe.config.Config config;

    protected final Optional<Set<SerializerConfig>> serializerConfigs;

    /**
     * @param name Hazelcast instance name
     * @param config TypeSafe config
     * @param serializerConfigs optional set of serializer configs
     */
    public TypesafeHazelcastConfig(final String name, final com.typesafe.config.Config config, final Set<SerializerConfig> serializerConfigs) {
        checkArgument(StringUtils.isNotBlank(name));
        checkNotNull(config);
        checkNotNull(serializerConfigs);
        this.name = name;
        this.config = config;
        this.serializerConfigs = Optional.of(ImmutableSet.copyOf(serializerConfigs));
    }

    /**
     * @param name Hazelcast instance name
     * @param config TypeSafe config
     */
    public TypesafeHazelcastConfig(final String name, final com.typesafe.config.Config config) {
        checkArgument(StringUtils.isNotBlank(name));
        checkNotNull(config);
        this.name = name;
        this.config = config;
        this.serializerConfigs = Optional.empty();
    }

    public Config getConfig() {
        final Config hazelcastConfig = new Config();
        hazelcastConfig.setInstanceName(name);

        hazelcastConfig.setGroupConfig(ConfigUtils.getConfig(config, "group-config")
                .map(this::groupConfig)
                .orElseThrow(new ConfigurationExceptionSupplier("group-config is required"))
        );

        hazelcastConfig.setNetworkConfig(ConfigUtils.getConfig(config, "network-config")
                .map(this::networkConfig)
                .orElseThrow(new ConfigurationExceptionSupplier("network-config is required"))
        );

        ConfigUtils.getConfigList(config, "map-configs").ifPresent(mapConfigs -> {
            mapConfigs.stream().map(this::mapConfig).forEach(hazelcastConfig::addMapConfig);
        });

        ConfigUtils.getConfigList(config, "multi-map-configs").ifPresent(mapConfigs -> {
            mapConfigs.stream().map(this::multiMapConfig).forEach(hazelcastConfig::addMultiMapConfig);
        });

        ConfigUtils.getConfigList(config, "queue-configs").ifPresent(queueConfigs -> {
            queueConfigs.stream().map(this::queueConfig).forEach(hazelcastConfig::addQueueConfig);
        });

        ConfigUtils.getConfigList(config, "topic-configs").ifPresent(queueConfigs -> {
            queueConfigs.stream().map(this::topicConfig).forEach(hazelcastConfig::addTopicConfig);
        });

        ConfigUtils.getConfigList(config, "list-configs").ifPresent(listConfigs -> {
            listConfigs.stream().map(this::listConfig).forEach(hazelcastConfig::addListConfig);
        });

        ConfigUtils.getConfigList(config, "set-configs").ifPresent(listConfigs -> {
            listConfigs.stream().map(this::getSetConfig).forEach(hazelcastConfig::addSetConfig);
        });

        ConfigUtils.getConfigList(config, "semaphore-configs").ifPresent(semaphoreConfigs -> {
            semaphoreConfigs.stream().map(this::semaphoreConfig).forEach(hazelcastConfig::addSemaphoreConfig);
        });

        ConfigUtils.getConfigList(config, "executor-configs").ifPresent(executorConfigs -> {
            executorConfigs.stream().map(this::executorConfig).forEach(hazelcastConfig::addExecutorConfig);
        });

        hazelcastConfig.setSerializationConfig(new SerializationConfig());
        serializerConfigs.ifPresent(configs -> configs.stream().forEach(serializerConfig -> hazelcastConfig.getSerializationConfig().addSerializerConfig(serializerConfig)));

        ConfigUtils.getConfigList(config, "partition-group-config").ifPresent(partitionGroupConfig -> {
            partitionGroupConfig.stream().map(this::partitionConfig).forEach(hazelcastConfig::setPartitionGroupConfig);
        });

        // Application manages the lifecycle and registers a shutdown hook - we want to ensure this is the last service that this is the last service that is started
        hazelcastConfig.setProperty("hazelcast.shutdownhook.enabled", "false");
        // mapping hazelcast.jmx.enabled to hazelcast.jmx because using Typesafe config, hazelcast.jmx is an object and cannot be set to a boolean
        ConfigUtils.getBoolean(config, "properties", "hazelcast", "jmx", "enabled").ifPresent(jmxEnabled -> hazelcastConfig.setProperty("hazelcast.jmx", Boolean.toString(jmxEnabled)));

        ConfigUtils.getConfig(config, "properties").ifPresent(properties -> {
            hazelcastConfig.setProperties(ConfigUtils.toProperties(properties));
        });

        ConfigUtils.getConfig(config, "member-attribute-config")
                .map(this::memberAttributeConfig)
                .ifPresent(hazelcastConfig::setMemberAttributeConfig);

        applyAdditionalConfiguration(hazelcastConfig);
        return hazelcastConfig;
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

    protected GroupConfig groupConfig(final com.typesafe.config.Config config) {
        return new GroupConfig(config.getString("name"), config.getString("password"));
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

    /**
     * Is invoked after all of the config specified in the Typesafe config is applied. This provides a hook to be able to configure aspects that are not
     * supported by the Typesafe config.
     *
     * By default, this is a no-op method.
     *
     * @param hazelcastConfig config
     */
    protected void applyAdditionalConfiguration(final Config hazelcastConfig) {
    }

}
