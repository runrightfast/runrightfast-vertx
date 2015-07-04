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

import static com.google.common.base.Preconditions.checkArgument;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 * @author alfio
 * @param <T> type
 */
public abstract class TypeReference<T> {

    private final Type type;

    private final Class<?> rawType;

    protected TypeReference() {
        final Type superclass = getClass().getGenericSuperclass();
        checkArgument(!(superclass instanceof Class), "Missing type parameter.");
        this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];

        if (this.type instanceof ParameterizedType) {
            this.rawType = (Class<?>) ((ParameterizedType) this.type).getRawType();
        } else {
            this.rawType = (Class<?>) this.type;
        }
    }

    public Class<?> getRawType() {
        return rawType;
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof TypeReference) ? ((TypeReference) o).type.equals(this.type)
                : false;
    }

    @Override
    public int hashCode() {
        return this.type.hashCode();
    }

    @Override
    public String toString() {
        return type.getTypeName();
    }
}
