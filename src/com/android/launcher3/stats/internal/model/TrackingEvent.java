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

package com.android.launcher3.stats.internal.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.stats.external.TrackingBundle;
import com.android.launcher3.stats.internal.db.TrackingEventContract;
import com.android.launcher3.stats.util.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 *     Model of an event to track
 * </pre>
 */
public class TrackingEvent {

    // Constants
    private static final String TAG = TrackingEvent.class.getSimpleName();

    // Members
    private Category mCategory;
    private final Map<String, String> mMetaData = new HashMap<String, String>();

    public enum Category {
        APP_LAUNCH,
        WIDGET_ADD,
        WIDGET_REMOVE,
        SETTINGS_OPEN,
        WALLPAPER_CHANGE,
        HOMESCREEN_PAGE,
        WIDGET,

        // Remote folder specific
        REMOTE_FOLDER_DISABLED,
        REMOTE_FOLDER_OPENED,
        REMOTE_FOLDER_INFO_OPENED,
        REMOTE_APP_OPENED,
        REMOTE_APP_INSTALLED,
        REMOTE_SYNC_TIME
    }

    public static final String KEY_ORIGIN = TrackingBundle.KEY_METADATA_ORIGIN;
    public static final String KEY_VALUE = TrackingBundle.KEY_METADATA_VALUE;
    public static final String KEY_PACKAGE = TrackingBundle.KEY_METADATA_PACKAGE;

    /**
     * Constructor
     *
     * @param category {@link TrackingEvent.Category}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public TrackingEvent(Category category) throws IllegalArgumentException {
        if (category == null) {
            throw new IllegalArgumentException("'category' cannot be null or empty!");
        }
        mCategory = category;
    }

    /**
     * Constructor
     *
     * @param cursor {@link Cursor}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public TrackingEvent(Cursor cursor) throws IllegalArgumentException {
        if (cursor == null) {
            throw new IllegalArgumentException("'cursor' cannot be null!");
        }
        mCategory = Category.valueOf(cursor.getString(cursor.getColumnIndex(
                TrackingEventContract.EVENT_COLUMN_CATEGORY)));
        String metadata = cursor.getString(cursor.getColumnIndex(
                TrackingEventContract.EVENT_COLUMN_METADATA));
        if (!TextUtils.isEmpty(metadata)) {
            String[] parts = metadata.split(",");
            for (String part : parts) {
                try {
                    String key = part.split("=")[0];
                    String val = part.split("=")[1];
                    mMetaData.put(key, val);
                } catch (IndexOutOfBoundsException e) {
                    Log.w(TAG, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Get the category
     *
     * @return {@link TrackingEvent.Category}
     */
    public Category getCategory() {
        return mCategory;
    }

    /**
     * Get the set of meta data keys
     *
     * @return {@link Set}
     */
    public Set<String> getMetaDataKeySet() {
        return mMetaData.keySet();
    }

    /**
     * Set some meta data
     *
     * @param key   {@link String}
     * @param value {@link String}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public void setMetaData(String key, String value) throws IllegalArgumentException {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("'key' cannot be null or empty!");
        }
        if (TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException("'value' cannot be null or empty!");
        }
        mMetaData.put(key, value);
    }

    /**
     * Get some meta data value
     *
     * @param key {@link String}
     * @return {@link String}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public String getMetaData(String key) throws IllegalArgumentException {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("'key' cannot be null or empty!");
        }
        if (mMetaData.containsKey(key)) {
            return mMetaData.get(key);
        }
        return null;
    }

    /**
     * Remove some meta data
     *
     * @param key {@link String}
     * @return {@link String} or null
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public String removeMetaData(String key) throws IllegalArgumentException {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("'key' cannot be null or empty!");
        }
        if (mMetaData.containsKey(key)) {
            return mMetaData.remove(key);
        }
        return null;
    }

    /**
     * Converts this object into content values for use with sqlite
     *
     * @return {@link ContentValues}
     */
    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TrackingEventContract.EVENT_COLUMN_CATEGORY, mCategory.name());
        StringBuilder sb = new StringBuilder();
        for (String key : mMetaData.keySet()) {
            sb.append(key).append("=").append(mMetaData.get(key)).append(",");
        }
        if (sb.length() > 0) {
            String metadata = sb.toString();
            metadata = metadata.substring(0, metadata.length() - 1);
            Logger.logd(TAG, "MetaData: " + metadata);
            contentValues.put(TrackingEventContract.EVENT_COLUMN_METADATA, metadata);
        }
        return contentValues;
    }

    /**
     * Convert this object into a tracking bundle
     *
     * @param trackingId {@link String}
     * @param action     {@link ITrackingAction}
     * @return {@link Bundle}
     */
    public Bundle toTrackingBundle(String trackingId, ITrackingAction action) {
        Bundle bundle = TrackingBundle.createTrackingBundle(trackingId, mCategory.name(),
                action.toString());
        return bundle;
    }

}
