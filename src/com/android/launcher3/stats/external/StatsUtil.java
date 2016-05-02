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

import com.android.launcher3.LauncherApplication;
import com.android.launcher3.stats.util.Logger;
import com.cyanogen.ambient.analytics.Event;

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
        Logger.logd(TAG, "Stats collection: ENABLED!");

        if (!trackingBundle.containsKey(KEY_TRACKING_ID)) {
            Logger.logd(TAG, "No tracking id in bundle");
            return;
        } else {
            if (trackingBundle.containsKey(TrackingBundle.KEY_EVENT_CATEGORY)
                    && trackingBundle.containsKey(TrackingBundle.KEY_EVENT_ACTION)) {

                final Event.Builder builder = new Event.Builder(
                        trackingBundle.getString(TrackingBundle.KEY_EVENT_CATEGORY),
                        trackingBundle.getString(TrackingBundle.KEY_EVENT_ACTION));

                if (trackingBundle.containsKey(TrackingBundle.KEY_METADATA_ORIGIN)) {
                    builder.addField(TrackingBundle.KEY_METADATA_ORIGIN,
                            trackingBundle.getString(TrackingBundle.KEY_METADATA_ORIGIN));
                }
                if (trackingBundle.containsKey(TrackingBundle.KEY_METADATA_PACKAGE)) {
                    builder.addField(TrackingBundle.KEY_METADATA_PACKAGE,
                            trackingBundle.getString(TrackingBundle.KEY_METADATA_PACKAGE));
                }
                if (trackingBundle.containsKey(TrackingBundle.KEY_METADATA_VALUE)) {
                    builder.addField(TrackingBundle.KEY_METADATA_VALUE,
                            String.valueOf(trackingBundle.get(TrackingBundle.KEY_METADATA_VALUE)));
                }
                ((LauncherApplication)context.getApplicationContext()).sendEvent(builder.build());

                Logger.logd(TAG, trackingBundle.toString());
            } else {
                Logger.logd(TAG, "Not a valid tracking bundle");
            }
        }
    }

}
