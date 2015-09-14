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
package co.runrightfast.core.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author alfio
 */
@RequiredArgsConstructor
public class HazelcastInstanceWithNoopShutdown implements InvocationHandler {

    public static HazelcastInstance hazelcastInstanceWithNoopShutdown(final HazelcastInstance hazelcast) {
        return (HazelcastInstance) Proxy.newProxyInstance(
                HazelcastInstanceWithNoopShutdown.class.getClassLoader(),
                new Class[]{HazelcastInstance.class},
                new HazelcastInstanceWithNoopShutdown(hazelcast)
        );
    }

    @NonNull
    private final HazelcastInstance hazelcast;

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (method.getName().equals("shutdown")) {
            return null;
        }

        return method.invoke(hazelcast, args);
    }

}
