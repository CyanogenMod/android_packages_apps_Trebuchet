/*
 *  Copyright (c) 2015. The CyanogenMod Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.android.launcher3.stats.util;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

/**
 * <pre>
 *     Metrics debug logging
 * </pre>
 */
public class Logger {

    private static final String TAG = "TrebuchetStats";

    /**
     * Log a debug message
     *
     * @param tag {@link String}
     * @param msg {@link String }
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public static void logd(String tag, String msg) throws IllegalArgumentException {
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("'tag' cannot be empty!");
        }
        if (TextUtils.isEmpty(msg)) {
            throw new IllegalArgumentException("'msg' cannot be empty!");
        }
        if (isDebugging()) {
            Log.d(TAG, tag + " [ " + msg + " ]");
        }
    }

    private static boolean isDebugging() {
        return Log.isLoggable(TAG, Log.DEBUG);
    }

}
