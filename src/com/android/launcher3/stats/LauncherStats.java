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

package com.android.launcher3.stats;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.stats.internal.db.DatabaseHelper;
import com.android.launcher3.stats.internal.model.TrackingEvent;

/**
 * <pre>
 *     Utility class made specifically for Launcher related events
 * </pre>
 */
public class LauncherStats {

    // Constants
    private static final String TAG = LauncherStats.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final int MSG_STORE_EVENT = 1000;
    public static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final String ORIGIN_HOMESCREEN = "homescreen";
    public static final String ORIGIN_APPDRAWER = "appdrawer";
    public static final String ORIGIN_TREB_LONGPRESS = "trebuchet_longpress";
    public static final String ORIGIN_CHOOSER = "theme_chooser";
    public static final String ORIGIN_SETTINGS = "settings";
    public static final String ORIGIN_DRAG_DROP = "drag_drop";
    public static final String ORIGIN_FOLDER = "folder";

    private static void log(String msg) throws IllegalArgumentException {
        if (TextUtils.isEmpty(msg)) {
            throw new IllegalArgumentException("'msg' cannot be null or empty!");
        }
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private static void loge(String msg) throws IllegalArgumentException {
        if (TextUtils.isEmpty(msg)) {
            throw new IllegalArgumentException("'msg' cannot be null or empty!");
        }
        Log.e(TAG, msg);
    }

    /**
     * <pre>
     *     This is a thread responsible for writing events to a database
     * </pre>
     *
     * @see {@link HandlerThread}
     */
    private static class WriteHandlerThread extends HandlerThread {
        public WriteHandlerThread() {
            super(WriteHandlerThread.class.getSimpleName());
        }
    }

    /**
     * <pre>
     *     Handler for issuing db writes
     * </pre>
     *
     * @see {@link Handler}
     */
    private static class WriteHandler extends Handler {

        public WriteHandler() {
            super(sHandlerThread.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            log("Handling message: " + msg.what);
            switch (msg.what) {
                case MSG_STORE_EVENT:
                    handleStoreEvent((TrackingEvent) msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    // Instance
    private static LauncherStats sInstance = null;

    // Members
    private static WriteHandlerThread sHandlerThread;
    private static WriteHandler sWriteHandler;
    private static DatabaseHelper sDatabaseHelper;

    /**
     * Send a message to the handler to store event data
     *
     * @param trackingEvent {@link TrackingEvent}
     */
    protected void sendStoreEventMessage(TrackingEvent trackingEvent) {
        log("Sending tracking event to handler: " + trackingEvent);
        Message msg = new Message();
        msg.what = MSG_STORE_EVENT;
        msg.obj = trackingEvent;
        sWriteHandler.sendMessage(msg);
    }

    /**
     * Handle the storing work
     *
     * @param trackingEvent {@link TrackingEvent}
     */
    private static void handleStoreEvent(TrackingEvent trackingEvent) {
        log("Handling store event: " + trackingEvent);
        if (trackingEvent != null) {
            sDatabaseHelper.writeEvent(trackingEvent);
        } else {
            loge("Tracking event was null!");
        }
    }

    /**
     * Used only for overlay extensions
     */
    protected LauncherStats() { }

    /**
     * Constructor
     *
     * @param context {@link Context} not null!
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    private LauncherStats(Context context) throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("'context' cannot be null!");
        }
        sDatabaseHelper = new DatabaseHelper(context);
        sHandlerThread = new WriteHandlerThread();
        sHandlerThread.start();
        sWriteHandler = new WriteHandler();
    }

    /**
     * Gets a singleton instance of the stats utility
     *
     * @param context {@link Context} not null!
     * @return {@link LauncherStats}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public static LauncherStats getInstance(Context context)
            throws IllegalArgumentException {
        if (sInstance == null) {
            sInstance = new LauncherStats(context);
        }
        return sInstance;
    }

    /**
     * Interface for posting a new widget add event
     *
     * @param pkg {@link String} package name of widget
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public void sendWidgetAddEvent(String pkg) throws IllegalArgumentException {
        if (TextUtils.isEmpty(pkg)) {
            throw new IllegalArgumentException("'pkg' cannot be null!");
        }
        TrackingEvent trackingEvent = new TrackingEvent(TrackingEvent.Category.WIDGET_ADD);
        trackingEvent.setMetaData(TrackingEvent.KEY_PACKAGE, pkg);
        sendStoreEventMessage(trackingEvent);
    }

    /**
     * Interface for posting a new widget removal event
     *
     * @param pkg {@link String} package name of widget
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public void sendWidgetRemoveEvent(String pkg) throws IllegalArgumentException {
        if (TextUtils.isEmpty(pkg)) {
            throw new IllegalArgumentException("'pkg' cannot be null!");
        }
        TrackingEvent trackingEvent = new TrackingEvent(TrackingEvent.Category.WIDGET_REMOVE);
        trackingEvent.setMetaData(TrackingEvent.KEY_PACKAGE, pkg);
        sendStoreEventMessage(trackingEvent);
    }

    /**
     * Interface for posting an app launch event
     *
     * @param origin {@link String} origin of application launch
     * @param pkg    {@link String} package of app launched
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public void sendAppLaunchEvent(String origin, String pkg) throws IllegalArgumentException {
        if (TextUtils.isEmpty(origin)) {
            throw new IllegalArgumentException("'origin' cannot be null!");
        }
        if (TextUtils.isEmpty(pkg)) {
            throw new IllegalArgumentException("'pkg' cannot be null!");
        }
        TrackingEvent trackingEvent = new TrackingEvent(TrackingEvent.Category.APP_LAUNCH);
        trackingEvent.setMetaData(TrackingEvent.KEY_ORIGIN, origin);
        trackingEvent.setMetaData(TrackingEvent.KEY_PACKAGE, pkg);
        sendStoreEventMessage(trackingEvent);
    }

    /**
     * Interface for sending a "settings opened" event
     *
     * @param origin {@link String} origin of the event
     */
    public void sendSettingsOpenedEvent(String origin) {
        TrackingEvent trackingEvent = new TrackingEvent(TrackingEvent.Category.SETTINGS_OPEN);
        trackingEvent.setMetaData(TrackingEvent.KEY_ORIGIN, origin);
        sendStoreEventMessage(trackingEvent);
    }

    /**
     * Interface for sending a "wallpaper changed" event
     *
     * @param origin {@link String} origin of the event
     */
    public void sendWallpaperChangedEvent(String origin) {
        TrackingEvent trackingEvent = new TrackingEvent(TrackingEvent.Category.WALLPAPER_CHANGE);
        trackingEvent.setMetaData(TrackingEvent.KEY_ORIGIN, origin);
        sendStoreEventMessage(trackingEvent);
    }

}
