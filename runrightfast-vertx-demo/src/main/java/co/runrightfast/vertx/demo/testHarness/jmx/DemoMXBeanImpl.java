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
package co.runrightfast.vertx.demo.testHarness.jmx;

import co.runrightfast.vertx.core.RunRightFastVerticleId;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static java.util.logging.Level.SEVERE;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 */
@RequiredArgsConstructor
@Log
public final class DemoMXBeanImpl implements DemoMXBean {

    private final Vertx vertx;

    @Override
    public String getVerticleDeployments() {
        final RunRightFastVerticleId verticleManagerId = RunRightFastVerticleManager.VERTICLE_ID;
        final CompletableFuture<com.google.protobuf.Message> future = new CompletableFuture();
        final String address = EventBusAddress.eventBusAddress(verticleManagerId, "get-verticle-deployments");
        vertx.eventBus().send(
                address,
                GetVerticleDeployments.Request.newBuilder().build(),
                responseHandler(future, GetVerticleDeployments.Response.class)
        );

        final com.google.protobuf.Message response;
        try {
            response = future.get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
        return JsonUtils.toVertxJsonObject(ProtobufUtils.protobuMessageToJson(response)).encodePrettily();
    }

    private <A extends com.google.protobuf.Message> Handler<AsyncResult<Message<A>>> responseHandler(final CompletableFuture future, final Class<A> messageType) {
        return result -> {
            if (result.succeeded()) {
                future.complete(result.result().body());
            } else {
                log.logp(SEVERE, getClass().getName(), String.format("responseHandler.failure::%s", messageType.getName()), "request failed", result.cause());
                future.completeExceptionally(result.cause());
            }
        };
    }

}
