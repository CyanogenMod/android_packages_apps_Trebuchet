/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.launcher3.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsProvider {
    public static final String SETTINGS_KEY = "trebuchet_preferences";

    public static final String SETTINGS_UI_HOMESCREEN_DEFAULT_SCREEN_ID = "ui_homescreen_default_screen_id";
    public static final String SETTINGS_UI_HOMESCREEN_SEARCH = "ui_homescreen_search";
    public static final String SETTINGS_UI_HOMESCREEN_HIDE_ICON_LABELS = "ui_homescreen_general_hide_icon_labels";
    public static final String SETTINGS_UI_HOMESCREEN_SCROLLING_WALLPAPER_SCROLL = "ui_homescreen_scrolling_wallpaper_scroll";
    public static final String SETTINGS_UI_HOMESCREEN_REMOTE_FOLDER = "ui_homescreen_remote_folder";
    public static final String SETTINGS_UI_DRAWER_REMOTE_APPS = "ui_drawer_remote_apps";
    public static final String SETTINGS_UI_DYNAMIC_GRID_SIZE = "ui_dynamic_grid_size";
    public static final String SETTINGS_UI_HOMESCREEN_ROWS = "ui_homescreen_rows";
    public static final String SETTINGS_UI_HOMESCREEN_COLUMNS = "ui_homescreen_columns";
    public static final String SETTINGS_UI_DRAWER_HIDE_ICON_LABELS = "ui_drawer_hide_icon_labels";
    public static final String SETTINGS_UI_DRAWER_STYLE_USE_COMPACT = "ui_drawer_style_compact";
    public static final String SETTINGS_UI_DRAWER_DARK = "ui_drawer_dark";
    public static final String SETTINGS_UI_USE_SCROLLER = "ui_scroller";
    public static final String SETTINGS_UI_USE_HORIZONTAL_SCRUBBER = "ui_horizontal_scrubber";
    public static final String SETTINGS_UI_DRAWER_SEARCH = "ui_drawer_search";
    public static final String SETTINGS_UI_GENERAL_ICONS_LARGE = "ui_general_icons_large";
    public static final String SETTINGS_UI_ALLOW_ROTATION = "ui_allow_rotation";

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);
    }

    public static int getIntCustomDefault(Context context, String key, int def) {
        return get(context).getInt(key, def);
    }

    public static int getInt(Context context, String key, int resource) {
        return getIntCustomDefault(context, key, context.getResources().getInteger(resource));
    }

    public static long getLongCustomDefault(Context context, String key, long def) {
        return get(context).getLong(key, def);
    }

    public static long getLong(Context context, String key, int resource) {
        return getLongCustomDefault(context, key, context.getResources().getInteger(resource));
    }

    public static boolean getBooleanCustomDefault(Context context, String key, boolean def) {
        return get(context).getBoolean(key, def);
    }

    public static boolean getBoolean(Context context, String key, int resource) {
        return getBooleanCustomDefault(context, key, context.getResources().getBoolean(resource));
    }

    public static String getStringCustomDefault(Context context, String key, String def) {
        return get(context).getString(key, def);
    }

    public static String getString(Context context, String key, int resource) {
        return getStringCustomDefault(context, key, context.getResources().getString(resource));
    }

    public static void putString(Context context, String key, String value) {
        get(context).edit().putString(key, value).commit();
    }

    public static void putInt(Context context, String key, int value) {
        get(context).edit().putInt(key, value).commit();
    }

    public static boolean changeBoolean(Context context, String key, int defaultRes) {
        boolean def = context.getResources().getBoolean(defaultRes);
        boolean val = !SettingsProvider.getBooleanCustomDefault(context, key, def);
        putBoolean(context, key, val);
        return val;
    }

    public static void putBoolean(Context context, String key, int res) {
        boolean val = context.getResources().getBoolean(res);
        putBoolean(context, key, val);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        get(context).edit().putBoolean(key, value).commit();
    }
}
