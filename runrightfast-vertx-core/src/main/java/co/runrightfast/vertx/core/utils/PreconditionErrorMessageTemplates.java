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
package co.runrightfast.vertx.core.utils;

/**
 *
 * @author alfio
 */
public interface PreconditionErrorMessageTemplates {

    // string validation
    static final String MUST_NOT_BE_BLANK = "'%s' must not be blank";
    static final String PATTERN_DOES_NOT_MATCH = "'%s': pattern does not match: %s";

    // numeric validation
    static final String MUST_BE_GREATER_THAN_ZERO = "'%s' must be greater than 0";
    static final String MUST_BE_GREATER_THAN = "'%s' must be greater than %d";
    static final String MUST_BE_WITHIN_RANGE = "'%s' must be within the range (%d-%d)";

    // collection validation
    static final String MUST_NOT_BE_EMPTY = "'%s' must not be empty";

    // File / Dir validation
    static final String PATH_DOES_NOT_EXIST = "'%s': path does not exist : %s";
    static final String PATH_IS_NOT_A_DIRECTORY = "'%s': %s is not a directory";
    static final String PATH_IS_NOT_READABLE = "'%s': %s is not readable";
    static final String PATH_IS_NOT_WRITEABLE = "'%s': %s is not writable";

}
