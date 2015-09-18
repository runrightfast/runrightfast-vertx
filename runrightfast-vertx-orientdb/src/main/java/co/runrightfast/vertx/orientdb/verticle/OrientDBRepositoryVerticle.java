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
package co.runrightfast.vertx.orientdb.verticle;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.orientdb.OrientDBService;
import lombok.Getter;
import lombok.Setter;

/**
 * <h3>Design</h3>
 * Instances of this verticle will be managed by the {@link OrientDBVerticle}, i.e., they will be deployed by {@link OrientDBVerticle}, which means their
 * lifecyle will be tied to {@link OrientDBVerticle}. The {@link OrientDBService} will be injected before the verticle is started by {@link OrientDBVerticle}.
 * The deployments are defined by {@link OrientDBRepositoryVerticleDeployment}.
 *
 * This verticle is meant to manage an OrientDB based repository. Repository clients interact with the repository via the Vertx event bus and Protobuf messages.
 *
 * @author alfio
 */
public abstract class OrientDBRepositoryVerticle extends RunRightFastVerticle {

    @Getter
    @Setter
    protected OrientDBService orientDBService;

    public OrientDBRepositoryVerticle(final AppEventLogger appEventLogger) {
        super(appEventLogger);
    }

}
