/* Copyright (C) RunRightFast.co - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alfio Zappala azappala@azaptree.com, March 2014
 */
package co.runrightfast.core;

import javax.json.JsonObject;

/**
 *
 * @author alfio
 */
public interface JsonRepresentation {

    default String getType() {
        return getClass().getName();
    }

    JsonObject toJson();

}
