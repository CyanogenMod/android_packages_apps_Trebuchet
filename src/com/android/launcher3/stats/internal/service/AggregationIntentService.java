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

package com.android.launcher3.stats.internal.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherApplication;
import com.android.launcher3.stats.external.StatsUtil;
import com.android.launcher3.stats.external.TrackingBundle;
import com.android.launcher3.stats.internal.db.DatabaseHelper;
import com.android.launcher3.stats.internal.model.CountAction;
import com.android.launcher3.stats.internal.model.CountOriginByPackageAction;
import com.android.launcher3.stats.internal.model.ITrackingAction;
import com.android.launcher3.stats.internal.model.TrackingEvent;
import com.android.launcher3.stats.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *     Service that starts on a timer and handles aggregating events and sending them to
 *     CyanogenStats
 * </pre>
 *
 * @see {@link IntentService}
 */
public class AggregationIntentService extends IntentService {

    // Constants
    private static final String TAG = AggregationIntentService.class.getSimpleName();
    private static final String TRACKING_ID = "com.cyanogenmod.trebuchet";
    public static final String ACTION_AGGREGATE_AND_TRACK =
            "com.cyanogenmod.trebuchet.AGGREGATE_AND_TRACK";
    private static final List<ITrackingAction> TRACKED_ACTIONS = new ArrayList<ITrackingAction>() {
        {
            add(new CountAction());
            add(new CountOriginByPackageAction());
        }
    };
    private static final int INVALID_COUNT = -1;
    private static final String KEY_LAST_TIME_RAN = "last_time_stats_ran";
    public static final String PREF_KEY_PAGE_COUNT = "page_count";
    public static final String PREF_KEY_WIDGET_COUNT = "widget_count";

    // Members
    private DatabaseHelper mDatabaseHelper = null;
    private int mInstanceId = -1;
    private SharedPreferences mPrefs = null;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public AggregationIntentService() {
        super(AggregationIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isTrebuchetDefaultLauncher()) {
            // Cancel repeating schedule
            unscheduleService();
            // don't return b/c we still want to upload whatever metrics are left.
        }
        String action = intent.getAction();
        if (ACTION_AGGREGATE_AND_TRACK.equals(action)) {
            mPrefs = getSharedPreferences(LauncherAppState.getSharedPreferencesKey(),
                    Context.MODE_PRIVATE);
            mPrefs.edit().putLong(KEY_LAST_TIME_RAN, System.currentTimeMillis()).apply();
            mInstanceId = (int) System.currentTimeMillis();
            mDatabaseHelper = DatabaseHelper.createInstance(this);
            performAggregation();
            deleteTrackingEventsForInstance();
            handleNonEventMetrics();
        }
    }

    private void performAggregation() {

        // Iterate available categories
        for (TrackingEvent.Category category : TrackingEvent.Category.values()) {

            // Fetch the events from the database based on the category
            List<TrackingEvent> eventList =
                    mDatabaseHelper.getTrackingEventsByCategory(mInstanceId, category);

            Logger.logd(TAG, "Event list size: " + eventList.size());
            // Short circuit if no events for the category
            if (eventList.size() < 1) {
                continue;
            }

            // Now crunch the data into actionable events for the server
            for (ITrackingAction action : TRACKED_ACTIONS) {
                try {
                    for (Bundle bundle : action.createTrackingBundles(TRACKING_ID, category,
                            eventList)) {
                        performTrackingCall(bundle);
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "NPE fetching bundle list!", e);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument!", e);
                }
            }

        }
    }

    private void deleteTrackingEventsForInstance() {
        mDatabaseHelper.deleteEventsByInstanceId(mInstanceId);
    }

    /**
     * These are metrics that are not event based and need a snapshot every INTERVAL
     */
    private void handleNonEventMetrics() {
        sendPageCountStats();
        sendWidgetCountStats();

    }

    private void sendPageCountStats() {
        int pageCount = mPrefs.getInt(PREF_KEY_PAGE_COUNT, INVALID_COUNT);
        if (pageCount == INVALID_COUNT) {
            return;
        }
        Bundle bundle = TrackingBundle
                .createTrackingBundle(TRACKING_ID, TrackingEvent.Category.HOMESCREEN_PAGE.name(),
                        "count");
        bundle.putString(TrackingEvent.KEY_VALUE, String.valueOf(pageCount));
        StatsUtil.sendEvent(this, bundle);
    }

    private void sendWidgetCountStats() {
        int widgetCount = mPrefs.getInt(PREF_KEY_WIDGET_COUNT, INVALID_COUNT);
        if (widgetCount == INVALID_COUNT) {
            return;
        }
        Bundle bundle = TrackingBundle
                .createTrackingBundle(TRACKING_ID, TrackingEvent.Category.WIDGET.name(), "count");
        bundle.putString(TrackingEvent.KEY_VALUE, String.valueOf(widgetCount));
        StatsUtil.sendEvent(this, bundle);
    }

    private void performTrackingCall(Bundle bundle) throws IllegalArgumentException {
        StatsUtil.sendEvent(this, bundle);
    }

    private void unscheduleService() {
        Intent intent = new Intent(this, AggregationIntentService.class);
        intent.setAction(ACTION_AGGREGATE_AND_TRACK);
        PendingIntent pi = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
    }

    private boolean isTrebuchetDefaultLauncher() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        final String myPackageName = getPackageName();
        List<ComponentName> activities = new ArrayList<ComponentName>();
        final PackageManager packageManager = getPackageManager();

        // You can use name of your package here as third argument
        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                Logger.logd(TAG, "Trebuchet IS default launcher!");
                return true;
            }
        }
        Logger.logd(TAG, "Trebuchet IS NOT default launcher!");
        return false;
    }

    private static final long ALARM_INTERVAL = 86400000; // 1 day

    /**
     * Schedule an alarm service, will cancel existing
     *
     * @param context {@link Context}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public static void scheduleService(Context context) throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastTimeRan = prefs.getLong(KEY_LAST_TIME_RAN, 0);
        Intent intent = new Intent(context, AggregationIntentService.class);
        intent.setAction(ACTION_AGGREGATE_AND_TRACK);
        PendingIntent pi = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, lastTimeRan + ALARM_INTERVAL,
                ALARM_INTERVAL, pi);
    }

}
