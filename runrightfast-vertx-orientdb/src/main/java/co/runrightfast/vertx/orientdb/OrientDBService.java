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
package co.runrightfast.vertx.orientdb;

import co.runrightfast.core.TypeReference;
import co.runrightfast.core.TypeSafeObjectRegistry;
import com.orientechnologies.orient.client.remote.OServerAdmin;

/**
 *
 * @author alfio
 */
public interface OrientDBService extends OrientDBPoolService {

    /**
     * Used to register the service with {@link TypeSafeObjectRegistry}
     */
    static final TypeReference<OrientDBService> ORIENTDB_SERVICE = new TypeReference<OrientDBService>() {
    };

    /**
     *
     * @return new instance is returned - it is the responsibility of the client to close the connection, i.e., via OServerAdmin.close(), when they are done
     * with it.
     */
    OServerAdmin getServerAdmin();

}
