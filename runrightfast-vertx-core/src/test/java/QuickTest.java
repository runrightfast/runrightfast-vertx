
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

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
/**
 *
 * @author alfio
 */
public class QuickTest {

    private static final Logger LOG = Logger.getLogger(QuickTest.class.getName());

    @Test
    public void testJoin() {
        LOG.info("aaa");
    }

    public void testCreatingMultipleMetersWithSameName() {
        final MetricRegistry metrics = new MetricRegistry();

        final Meter m1 = metrics.meter("meter");
        final Meter m2 = metrics.meter("meter");

        assertThat(m1, is(sameInstance(m2)));

    }

}
