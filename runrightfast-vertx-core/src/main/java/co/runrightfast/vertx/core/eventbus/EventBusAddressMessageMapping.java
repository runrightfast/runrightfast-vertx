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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 * @param <REQUEST> Request message payload type
 * @param <RESPONSE> Response message payload type
 */
@EqualsAndHashCode(of = {"address"})
public final class EventBusAddressMessageMapping<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

    public static final class Builder<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

        private final EventBusAddressMessageMapping mapping = new EventBusAddressMessageMapping();

        private Builder() {
        }

        public Builder<REQUEST, RESPONSE> address(final String address) {
            mapping.address = address;
            return this;
        }

        public Builder<REQUEST, RESPONSE> requestDefaultInstance(final REQUEST req) {
            mapping.requestDefaultInstance = req;
            return this;
        }

        public Builder<REQUEST, RESPONSE> responseDefaultInstance(final RESPONSE response) {
            mapping.responseDefaultInstance = Optional.ofNullable(response);
            return this;
        }

        public EventBusAddressMessageMapping build() {
            mapping.validate();
            return mapping;
        }
    }

    public static <REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> Builder<REQUEST, RESPONSE> builder() {
        return new Builder<>();
    }

    @Getter
    private String address;

    @Getter
    private REQUEST requestDefaultInstance;

    @Getter
    private Optional<RESPONSE> responseDefaultInstance = Optional.empty();

    private EventBusAddressMessageMapping() {
    }

    public void validate() {
        checkNotNull(requestDefaultInstance);
        checkArgument(isNotBlank(address));
    }

    public JsonObject toJson() {
        final JsonObjectBuilder json = Json.createObjectBuilder()
                .add("address", address)
                .add("requestMessageType", requestDefaultInstance.getDescriptorForType().getFullName());
        getResponseDefaultInstance().ifPresent(instance -> json.add("responseMessageType", instance.getDescriptorForType().getFullName()));
        return json.build();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

}
