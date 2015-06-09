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

package com.android.launcher3.stats.external;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.android.launcher3.stats.util.Logger;

/**
 * StatsUtil
 * <pre>
 *     Utility for interfacing with CyanogenStats
 * </pre>
 */
public class StatsUtil {

    // Tag and logging
    private static final String TAG = StatsUtil.class.getSimpleName();

    // Constants
    private static final String KEY_TRACKING_ID = "tracking_id";
    private static final String ANALYTIC_INTENT = "com.cyngn.stats.action.SEND_ANALYTIC_EVENT";
    private static final String STATS_PACKAGE = "com.cyngn.stats";

    /**
     * Checks if stats collection is enabled
     *
     * @param context {@link android.content.Context}
     * @return {@link java.lang.Boolean}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public static boolean isStatsCollectionEnabled(Context context)
            throws IllegalArgumentException {
        return isStatsPackageInstalledAndSystemApp(context);
    }

    /**
     * Checks if the stats package is installed
     *
     * @param context {@link android.content.Context}
     * @return {@link Boolean {@link Boolean {@link Boolean {@link Boolean}}}}
     */
    private static boolean isStatsPackageInstalledAndSystemApp(Context context)
            throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(STATS_PACKAGE, 0);
            boolean isSystemApp = (pi.applicationInfo.flags &
                    (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            return pi.applicationInfo.enabled && isSystemApp;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "stats not found!");
            return false;
        }
    }

    /**
     * Send an event to CyangenStats
     *
     * @param context        {@link Context} not null
     * @param trackingBundle {@link Bundle}
     * @throws IllegalArgumentException
     */
    public static void sendEvent(Context context, Bundle trackingBundle)
            throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        if (trackingBundle == null) {
            throw new IllegalArgumentException("'trackingBundle' cannot be null!");
        }
        if (!isStatsCollectionEnabled(context)) {
            Logger.logd(TAG, "Stats collection: DISABLED!");
            return;
        }
        Logger.logd(TAG, "Stats collection: ENABLED!");

        Intent newIntent = new Intent(ANALYTIC_INTENT);

        if (!trackingBundle.containsKey(KEY_TRACKING_ID)) {
            Logger.logd(TAG, "No tracking id in bundle");
            return;
        } else {
            if (trackingBundle.containsKey(TrackingBundle.KEY_EVENT_CATEGORY)
                    && trackingBundle.containsKey(TrackingBundle.KEY_EVENT_ACTION)) {
                Logger.logd(TAG, trackingBundle.toString());
                newIntent.putExtras(trackingBundle);
                context.sendBroadcast(newIntent);
            } else {
                Logger.logd(TAG, "Not a valid tracking bundle");
            }
        }
    }

}
