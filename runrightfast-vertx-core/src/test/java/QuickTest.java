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

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class QuickTest {

    @Test
    public void testCreatingMultipleMetersWithSameName() {
        final MetricRegistry metrics = new MetricRegistry();

        final Meter m1 = metrics.meter("meter");
        final Meter m2 = metrics.meter("meter");

        assertThat(m1, is(sameInstance(m2)));
    }

    @Test
    public void testISOInstantFormat() {
        final String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        log.info(timestamp);
        log.info(Instant.parse(timestamp).toString());
    }

    /**
     * Shows that that when the 2 healthchecks with the same name are registered, then the latest one will replace the previous one.
     */
    @Test
    public void testRegisteringSameHealthcheckMultipleTimes() {
        final HealthCheckRegistry registry = new HealthCheckRegistry();
        final String healthCheckName = "health-check";
        final HealthCheck healthCheck1 = new HealthCheck() {

            @Override
            protected HealthCheck.Result check() throws Exception {
                return HealthCheck.Result.healthy();
            }
        };
        final HealthCheck healthCheck2 = new HealthCheck() {

            @Override
            protected HealthCheck.Result check() throws Exception {
                return HealthCheck.Result.healthy();
            }
        };
        registry.register(healthCheckName, healthCheck1);
        registry.register(healthCheckName, healthCheck2);

        assertThat(registry.getNames().size(), is(1));
    }

    @Test
    public void testAddingDuplicateElementsToImmutableSet() {
        final Object o = new Object();
        final ImmutableSet set = ImmutableSet.builder().add(o).add(o).build();
        assertThat(set.size(), is(1));
    }

    @Test
    public void testJavaExec() throws IOException, InterruptedException {
        final String command = "ip -4 -o addr show dev eth0 2> /dev/null | awk '{split($4,a,\"/\") ;print a[1]}'";

        //final Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
        final Process process = new ProcessBuilder().command("sh", "-c", command).start();
        log.info("ip address = " + getOutput(process));

        log.info("ip -4 -o addr show dev eth0 2 -> " + getOutput(Runtime.getRuntime().exec("ip -4 -o addr show dev eth0 2")));

    }

    private String getOutput(final Process process) throws IOException, InterruptedException {
        process.waitFor();
        try (final InputStream is = process.getInputStream()) {
            return IOUtils.toString(is);
        }
    }

    @Test
    public void testNetworkInterface() throws SocketException {
        final NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
        for (final Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements();) {
            log.info("etho address: " + addresses.nextElement());
        }

        networkInterface.getInterfaceAddresses().stream().forEach(address -> {
            log.info(String.format("%s : interface address: %s", address.getAddress().getClass().getSimpleName(), address.getAddress().getHostAddress()));
        });
    }

    /**
     * It appears to be ok to start multiple JMX reporters for the same MetricRegistry within the same domain.
     */
    @Test
    public void testStartingDuplicateMetricsJmxReporter() {
        final MetricRegistry metricRegistry = new MetricRegistry();

        final List<JmxReporter> reporters = new LinkedList<>();
        try {
            for (int i = 0; i < 2; i++) {
                reporters.add(jmxReporter(metricRegistry));
            }
        } finally {
            reporters.stream().forEach(JmxReporter::stop);
        }

        metricRegistry.counter("counter").inc();
        log.info("counter = " + metricRegistry.counter("counter").getCount());
    }

    private JmxReporter jmxReporter(final MetricRegistry metricRegistry) {
        final JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry)
                .inDomain("testDuplicateMetricsJmxReporter")
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .build();
        jmxReporter.start();
        log.info("started JmxReporter");
        return jmxReporter;
    }

    @Test
    public void testJsonArrayToString() {
        log.info(Json.createArrayBuilder().add(1).add(2).build().toString());
    }

    @Test
    public void testGson() {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final Foo foo = Foo.builder().count(10).success(true).message("message 1").message("message 2").build();
        log.info(gson.toJson(foo));
    }

    @Builder
    static class Foo {

        private int count;

        private boolean success;

        @Singular
        private List<String> messages;

    }

}
