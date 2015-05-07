/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.app.Application;
import com.android.launcher3.stats.LauncherStats;
import com.android.launcher3.stats.internal.service.AggregationIntentService;

public class LauncherApplication extends Application {
    public static boolean LAUNCHER_SHOW_UNREAD_NUMBER;
    public static boolean LAUNCHER_SHORTCUT_ENABLED;
    public static boolean SHOW_CTAPP_FEATURE;
    public static String PACKAGE_NAME = "";

    private static LauncherStats sLauncherStats = null;

    /**
     * Get the reference handle for LauncherStats commands
     *
     * @return {@link LauncherStats}
     */
    public static LauncherStats getLauncherStats() {
        return sLauncherStats;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PACKAGE_NAME = getPackageName();
        LAUNCHER_SHOW_UNREAD_NUMBER = getResources().getBoolean(
                R.bool.config_launcher_show_unread_number);
        LAUNCHER_SHORTCUT_ENABLED = getResources().getBoolean(
                R.bool.config_launcher_shortcut);
        SHOW_CTAPP_FEATURE = getResources().getBoolean(R.bool.config_launcher_page);
        LauncherAppState.setApplicationContext(this);
        LauncherAppState.getInstance();
        sLauncherStats = LauncherStats.createInstance(this);
        AggregationIntentService.scheduleService(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        LauncherAppState.getInstance().onTerminate();
    }

}
