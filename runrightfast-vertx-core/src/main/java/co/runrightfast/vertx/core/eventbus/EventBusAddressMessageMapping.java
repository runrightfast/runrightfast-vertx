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

import static com.google.common.base.Preconditions.checkState;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 *
 * @author alfio
 * @param <REQUEST> Request message payload type
 * @param <RESPONSE> Response message payload type
 */
@Builder
@EqualsAndHashCode(of = {"address"})
public final class EventBusAddressMessageMapping<REQUEST extends com.google.protobuf.Message, RESPONSE extends com.google.protobuf.Message> {

    @Getter
    @NonNull
    private final String address;

    @Getter
    @NonNull
    private final REQUEST requestDefaultInstance;

    @Getter
    @NonNull
    private Optional<RESPONSE> responseDefaultInstance = Optional.empty();

    public void validate() {
        checkState(isNotBlank(address));
    }

    public JsonObject toJson() {
        final JsonObjectBuilder json = Json.createObjectBuilder()
                .add("address", address)
                .add("requestMessageType", requestDefaultInstance.getDescriptorForType().getFullName());
        responseDefaultInstance.ifPresent(instance -> json.add("responseMessageType", instance.getDescriptorForType().getFullName()));
        return json.build();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

}
