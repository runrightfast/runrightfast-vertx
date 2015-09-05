/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<TypeReference<?>, Object> objects = new ConcurrentHashMap<>();

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

    public <A> Optional<A> put(@NonNull final ObjectRegistration<A> objectRegistration) {
        final Optional<A> formerValue = put(objectRegistration.getType(), objectRegistration.getObject());
        eventBus.post(objectRegistration);
        return formerValue;
    }

    /**
     *
     * @param type object type
     * @param obj object
     * @return object that was replaced
     */
    public <A> Optional<A> put(@NonNull final TypeReference<A> type, @NonNull final A obj) {
        checkArgument(Modifier.isPublic(type.getRawType().getModifiers()), "type interface must be public : %s", type);
        final Optional<A> formerValue = Optional.ofNullable((A) objects.put(type, obj));
        log.info(() -> String.format("registered object : %s -> %s", type, obj.getClass()));
        return formerValue;
    }

    /**
     *
     * @param <A> the object type
     * @param type type
     * @return the object that was removed
     */
    public <A> Optional<A> remove(@NonNull final TypeReference<?> type) {
        final Optional<A> removedObject = Optional.ofNullable((A) objects.remove(type));
        removedObject.ifPresent(a -> {
            log.info(() -> String.format("removed object : %s -> %s", type, a.getClass()));
            eventBus.post(new RemovedObject(type));
        });
        return removedObject;
    }

    public void clear() {
        final Set<TypeReference<?>> types = ImmutableSet.copyOf(objects.keySet());
        objects.clear();
        log.info("cleared all objects");
        types.stream().forEach(type -> eventBus.post(new RemovedObject(type)));
    }

    public <A> Optional<A> get(@NonNull final TypeReference<A> type) {
        return Optional.ofNullable((A) objects.get(type));
    }

    public Set<TypeReference<?>> getRegisteredTypes() {
        return objects.keySet();
    }

}
