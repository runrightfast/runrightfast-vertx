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
package co.runrightfast.vertx.orientdb.lifecycle;

import co.runrightfast.core.application.event.AppEvent;
import co.runrightfast.core.application.event.AppEventLogger;
import static co.runrightfast.vertx.orientdb.OrientDBEvents.ODATABASE_CLOSE;
import static co.runrightfast.vertx.orientdb.OrientDBEvents.ODATABASE_CREATE;
import static co.runrightfast.vertx.orientdb.OrientDBEvents.ODATABASE_CREATE_CLASS;
import static co.runrightfast.vertx.orientdb.OrientDBEvents.ODATABASE_DROP;
import static co.runrightfast.vertx.orientdb.OrientDBEvents.ODATABASE_DROP_CLASS;
import static co.runrightfast.vertx.orientdb.OrientDBEvents.ODATABASE_OPEN;
import co.runrightfast.vertx.orientdb.OrientDBEvents.ODatabaseLifecycleClassEvent;
import co.runrightfast.vertx.orientdb.OrientDBEvents.ODatabaseLifecycleEvent;
import com.orientechnologies.orient.core.db.ODatabaseInternal;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

/**
 *
 * @author alfio
 */
@Log
@RequiredArgsConstructor
public class RunRightFastOrientDBLifeCycleListener implements ODatabaseLifecycleListener {

    private final AppEventLogger appEventLogger;

    @Override
    public PRIORITY getPriority() {
        return PRIORITY.REGULAR;
    }

    @Override
    public void onCreate(final ODatabaseInternal odi) {
        appEventLogger.accept(AppEvent.info(ODATABASE_CREATE)
                .setData(new ODatabaseLifecycleEvent(odi.getName()))
                .build()
        );
    }

    @Override
    public void onOpen(final ODatabaseInternal odi) {
        appEventLogger.accept(AppEvent.info(ODATABASE_OPEN)
                .setData(new ODatabaseLifecycleEvent(odi.getName()))
                .build()
        );
    }

    @Override
    public void onClose(final ODatabaseInternal odi) {
        appEventLogger.accept(AppEvent.info(ODATABASE_CLOSE)
                .setData(new ODatabaseLifecycleEvent(odi.getName()))
                .build()
        );
    }

    @Override
    public void onDrop(final ODatabaseInternal odi) {
        appEventLogger.accept(AppEvent.info(ODATABASE_DROP)
                .setData(new ODatabaseLifecycleEvent(odi.getName()))
                .build()
        );
    }

    @Override
    public void onCreateClass(final ODatabaseInternal odi, final OClass oclass) {
        appEventLogger.accept(AppEvent.info(ODATABASE_CREATE_CLASS)
                .setData(new ODatabaseLifecycleClassEvent(odi.getName(), oclass.getName()))
                .build()
        );
    }

    @Override
    public void onDropClass(final ODatabaseInternal odi, final OClass oclass) {
        appEventLogger.accept(AppEvent.info(ODATABASE_DROP_CLASS)
                .setData(new ODatabaseLifecycleClassEvent(odi.getName(), oclass.getName()))
                .build()
        );
    }

}
