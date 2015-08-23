/* Copyright 2015 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * A copy of the license is available in the repository's
 * https://github.com/Esri/arcgis-runtime-demos-android/blob/master/license.txt
 *
 * For information about licensing your deployed app, see
 * https://developers.arcgis.com/android/guide/license-your-app.htm
 *
 */

package com.esri.runtime.android.materialbasemaps.util;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton that implements a thread pool to execute tasks asynchronously.
 */
public class TaskExecutor {
    private static final int POOL_SIZE = 3;

    private static TaskExecutor sInstance;

    private final ExecutorService mPool = Executors.newFixedThreadPool(POOL_SIZE);

    private TaskExecutor() {
    }

    public static TaskExecutor getInstance() {
        if (sInstance == null) {
            sInstance = new TaskExecutor();
        }
        return sInstance;
    }

    public ExecutorService getThreadPool() {
        return mPool;
    }
}
