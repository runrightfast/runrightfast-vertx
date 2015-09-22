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
package co.runrightfast.core.utils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;
import java.util.Base64;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.NonNull;

/**
 *
 * @author alfio
 */
public final class ProtobufUtils {

    public static final String TYPE_FIELD = "@type";

    private ProtobufUtils() {
    }

    public static JsonObject protobuMessageToJson(@NonNull final Message msg) {
        final JsonObjectBuilder json = Json.createObjectBuilder();
        final Descriptor desc = msg.getDescriptorForType();
        json.add(TYPE_FIELD, desc.getFullName());
        desc.getFields().stream()
                .filter(field -> {
                    if (field.isRepeated()) {
                        return msg.getRepeatedFieldCount(field) > 0;
                    }
                    return msg.hasField(field);
                })
                .forEach(field -> {
                    if (field.isMapField()) {
                        addMapField(json, msg, field);
                    } else if (field.isRepeated()) {
                        addRepeatedField(json, msg, field);
                    } else {
                        addField(json, msg, field);
                    }
                });
        return json.build();

    }

    private static void addMapField(final JsonObjectBuilder json, final Message msg, final Descriptors.FieldDescriptor field) {
        final JsonObjectBuilder map = Json.createObjectBuilder();
        final List<MapEntry<String, Object>> mapEntries = (List<MapEntry<String, Object>>) msg.getField(field);
        mapEntries.forEach(entry -> {
            final Object value = entry.getValue();
            if (value instanceof Message) {
                map.add(entry.getKey(), protobuMessageToJson((Message) value));
            } else if (value instanceof String) {
                map.add(entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                map.add(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                map.add(entry.getKey(), (Long) value);
            } else if (value instanceof Double) {
                map.add(entry.getKey(), (Double) value);
            } else if (value instanceof Float) {
                map.add(entry.getKey(), (Float) value);
            } else if (value instanceof Boolean) {
                map.add(entry.getKey(), (Boolean) value);
            } else if (value instanceof ByteString) {
                map.add(entry.getKey(), Base64.getEncoder().encodeToString(((ByteString) value).toByteArray()));
            }
        });
        json.add(field.getName(), map);
    }

    private static void addField(final JsonObjectBuilder json, final Message msg, final Descriptors.FieldDescriptor field) {
        switch (field.getJavaType()) {
            case BOOLEAN:
                json.add(field.getName(), (Boolean) msg.getField(field));
                break;
            case BYTE_STRING:
                final ByteString byteString = (ByteString) msg.getField(field);
                json.add(field.getName(), Base64.getEncoder().encodeToString(byteString.toByteArray()));
                break;
            case DOUBLE:
                json.add(field.getName(), (Double) msg.getField(field));
                break;
            case FLOAT:
                json.add(field.getName(), (Float) msg.getField(field));
                break;
            case INT:
                json.add(field.getName(), (Integer) msg.getField(field));
                break;
            case LONG:
                json.add(field.getName(), (Long) msg.getField(field));
                break;
            case MESSAGE:
                json.add(field.getName(), protobuMessageToJson((Message) msg.getField(field)));
                break;
            case STRING:
                json.add(field.getName(), (String) msg.getField(field));
                break;
        }
    }

    private static void addRepeatedField(final JsonObjectBuilder json, final Message msg, final Descriptors.FieldDescriptor field) {
        final int fieldCount = msg.getRepeatedFieldCount(field);
        final JsonArrayBuilder array = Json.createArrayBuilder();
        switch (field.getJavaType()) {
            case BOOLEAN:
                for (int i = 0; i < fieldCount; i++) {
                    array.add((Boolean) msg.getRepeatedField(field, i));
                }
                break;
            case BYTE_STRING:
                for (int i = 0; i < fieldCount; i++) {
                    final ByteString byteString = (ByteString) msg.getRepeatedField(field, i);
                    array.add(Base64.getEncoder().encodeToString(byteString.toByteArray()));
                }
                break;
            case DOUBLE:
                for (int i = 0; i < fieldCount; i++) {
                    array.add((Double) msg.getRepeatedField(field, i));
                }
                break;
            case FLOAT:
                for (int i = 0; i < fieldCount; i++) {
                    array.add((Float) msg.getRepeatedField(field, i));
                }
                break;
            case INT:
                for (int i = 0; i < fieldCount; i++) {
                    array.add((Integer) msg.getRepeatedField(field, i));
                }
                break;
            case LONG:
                for (int i = 0; i < fieldCount; i++) {
                    array.add((Long) msg.getRepeatedField(field, i));
                }
                break;
            case MESSAGE:
                for (int i = 0; i < fieldCount; i++) {
                    array.add(protobuMessageToJson((Message) msg.getRepeatedField(field, i)));
                }
                break;
            case STRING:
                for (int i = 0; i < fieldCount; i++) {
                    array.add((String) msg.getRepeatedField(field, i));
                }
                break;
        }
        json.add(field.getName(), array);
    }

}
