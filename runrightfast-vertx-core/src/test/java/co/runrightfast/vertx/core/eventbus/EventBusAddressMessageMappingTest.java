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
package co.runrightfast.vertx.core.eventbus;

import co.runrightfast.core.utils.JsonUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import javax.json.JsonObject;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class EventBusAddressMessageMappingTest {

    /**
     * Test of validate method, of class EventBusAddressMessageMapping.
     */
    @Test
    public void test_with_request_response() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .responseDefaultInstance(GetVerticleDeployments.Response.getDefaultInstance())
                .build();

        final GetVerticleDeployments.Response responseDefaultInstance = mapping.getResponseDefaultInstance().get();
        final GetVerticleDeployments.Request requestDefaultInstance = mapping.getRequestDefaultInstance();

        assertThat(responseDefaultInstance, is(GetVerticleDeployments.Response.getDefaultInstance()));
        assertThat(requestDefaultInstance, is(GetVerticleDeployments.Request.getDefaultInstance()));
        assertThat(mapping.getAddress(), is("/xyz"));
    }

    @Test
    public void test_with_request_only() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .build();

        assertThat(mapping.getResponseDefaultInstance().isPresent(), is(false));
        final GetVerticleDeployments.Request requestDefaultInstance = mapping.getRequestDefaultInstance();
        assertThat(requestDefaultInstance, is(GetVerticleDeployments.Request.getDefaultInstance()));
        assertThat(mapping.getAddress(), is("/xyz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_missing_address() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void test_missing_request() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .build();
    }

    @Test
    public void testToJson_with_response() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .responseDefaultInstance(GetVerticleDeployments.Response.getDefaultInstance())
                .build();

        final JsonObject json = mapping.toJson();

        assertThat(json.containsKey("responseMessageType"), is(true));
        assertThat(json.containsKey("requestMessageType"), is(true));
        assertThat(json.getString("requestMessageType"), is(GetVerticleDeployments.Request.getDefaultInstance().getDescriptorForType().getFullName()));
        assertThat(json.getString("responseMessageType"), is(GetVerticleDeployments.Response.getDefaultInstance().getDescriptorForType().getFullName()));
        assertThat(json.getString("address"), is("/xyz"));
    }

    /**
     * Test of toJson method, of class EventBusAddressMessageMapping.
     */
    @Test
    public void testToJson_with_no_response() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .build();

        final JsonObject json = mapping.toJson();

        assertThat(json.containsKey("responseMessageType"), is(false));
        assertThat(json.containsKey("requestMessageType"), is(true));
        assertThat(json.getString("requestMessageType"), is(GetVerticleDeployments.Request.getDefaultInstance().getDescriptorForType().getFullName()));
        assertThat(json.getString("address"), is("/xyz"));
    }

    /**
     * Test of toString method, of class EventBusAddressMessageMapping.
     */
    @Test
    public void testToString() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .responseDefaultInstance(GetVerticleDeployments.Response.getDefaultInstance())
                .build();

        final JsonObject json = mapping.toJson();
        final JsonObject json2 = JsonUtils.parse(mapping.toString());
        assertThat(json.getString("address"), is(json2.getString("address")));
        assertThat(json.getString("responseMessageType"), is(json2.getString("responseMessageType")));
        assertThat(json.getString("requestMessageType"), is(json2.getString("requestMessageType")));
    }

    /**
     * Test of equals method, of class EventBusAddressMessageMapping.
     */
    @Test
    public void testEquals() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping1 = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .responseDefaultInstance(GetVerticleDeployments.Response.getDefaultInstance())
                .build();

        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping2 = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .build();

        // only the address field is used for equality
        assertThat(mapping1, is(equalTo(mapping2)));
    }

    /**
     * Test of hashCode method, of class EventBusAddressMessageMapping.
     */
    @Test
    public void testHashCode() {
        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping1 = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .responseDefaultInstance(GetVerticleDeployments.Response.getDefaultInstance())
                .build();

        final EventBusAddressMessageMapping<GetVerticleDeployments.Request, GetVerticleDeployments.Response> mapping2 = EventBusAddressMessageMapping.<GetVerticleDeployments.Request, GetVerticleDeployments.Response>builder()
                .address("/xyz")
                .requestDefaultInstance(GetVerticleDeployments.Request.getDefaultInstance())
                .build();

        // only the address field is used for the hascode
        assertThat(mapping1.hashCode(), is(mapping2.hashCode()));
    }

}
