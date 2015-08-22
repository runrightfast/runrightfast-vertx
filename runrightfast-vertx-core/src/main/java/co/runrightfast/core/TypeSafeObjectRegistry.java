/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * An object registry that maps an interface to an object.
 *
 * The main reason this was created is because Dagger2 has no way to retrieve objects from a central location.
 *
 * @author alfio
 */
@Log
public final class TypeSafeObjectRegistry {

    public static final TypeSafeObjectRegistry GLOBAL_OBJECT_REGISTRY = new TypeSafeObjectRegistry();

    @Data
    public static final class ObjectRegistration<A> {

        @NonNull
        private final TypeReference<A> type;

        @NonNull
        private final A object;

    }

    @Data
    public static final class RemovedObject {

        @NonNull
        private final TypeReference<?> type;

    }

    private Map<TypeReference<?>, Object> objects = ImmutableMap.of();

    private final EventBus eventBus = new EventBus();

    public TypeSafeObjectRegistry() {
    }

    /**
     * can be used to register for {@link ObjectRegistration} and {@link RemovedObject} event
     *
     * @return
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    public synchronized Optional<Object> put(final ObjectRegistration<?> objectRegistration) {
        final Optional<Object> formerValue = put(objectRegistration.getType(), objectRegistration.getObject());
        eventBus.post(objectRegistration);
        return formerValue;
    }

    /**
     *
     * @param type object type
     * @param obj object
     * @return object that was replaced
     */
    private synchronized Optional<Object> put(final TypeReference<?> type, final Object obj) {
        checkNotNull(type);
        checkNotNull(obj);
        checkArgument(Modifier.isPublic(type.getRawType().getModifiers()), "type interface must be public : %s", type);
        checkArgument(type.getRawType().isInstance(obj), "obj is not an instance of %s", type);
        final Map<TypeReference<?>, Object> temp = new HashMap<>(objects);
        final Optional<Object> formerValue = Optional.ofNullable(temp.put(type, obj));
        objects = ImmutableMap.copyOf(temp);
        log.info(() -> String.format("registered object : %s -> %s", type, obj.getClass()));
        return formerValue;
    }

    /**
     *
     * @param <A> the object type
     * @param type type
     * @return the object that was removed
     */
    public synchronized <A> Optional<A> remove(final TypeReference<?> type) {
        final Map<TypeReference<?>, Object> temp = new HashMap<>(objects);
        final Optional<A> removedObject = Optional.ofNullable((A) temp.remove(type));
        removedObject.ifPresent(a -> {
            objects = ImmutableMap.copyOf(temp);
            log.info(() -> String.format("removed object : %s -> %s", type, a.getClass()));
            eventBus.post(new RemovedObject(type));
        });
        return removedObject;
    }

    public synchronized void clear() {
        final Set<TypeReference<?>> types = objects.keySet();
        objects = ImmutableMap.of();
        log.info("cleared all objects");
        types.stream().forEach(type -> eventBus.post(new RemovedObject(type)));
    }

    public <A> Optional<A> get(final TypeReference<A> type) {
        checkNotNull(type);
        return Optional.ofNullable((A) objects.get(type));
    }

    public Set<TypeReference<?>> getRegisteredTypes() {
        return objects.keySet();
    }

}
