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
package co.runrightfast.core;

import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import java.util.concurrent.CompletableFuture;
import lombok.extern.java.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class VertxDnsClientTest {

    private static Vertx vertx;

    @BeforeClass
    public static void beforeClass() {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void afterClass() {
        if (vertx != null) {
            vertx.close();
        }
    }

    @Test
    public void testDnsClient() throws Exception {
        final DnsClient client = vertx.createDnsClient(53, "8.8.8.8");
        final CompletableFuture<String> future = new CompletableFuture<>();
        client.lookup("vertx.io", result -> {
            if (result.succeeded()) {
                future.complete(result.result());
            } else {
                future.completeExceptionally(result.cause());
            }
        });

        log.info(future.get());
    }

}
