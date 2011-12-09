package com.android.launcher2.preference;

import android.content.Context;
import android.content.SharedPreferences;

public final class PreferencesProvider {
    public static final String PREFERENCES_KEY = "com.android.launcher_preferences";

    public static final String PREFERENCES_CHANGED = "preferences_changed";

    public static class Interface {
        public static class Homescreen {
            public static boolean getShowSearchBar(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_homescreen_general_search", true);
            }
            public static boolean getResizeAnyWidget(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_homescreen_general_resize_any_widget", false);
            }
            public static boolean getShowScrollingIndicator(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_homescreen_indicator_enable", true);
            }
            public static boolean getFadeScrollingIndicator(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_homescreen_indicator_fade", true);
            }
        }

        public static class Drawer {
            public static boolean getJoinWidgetsApps(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_drawer_widgets_join_apps", true);
            }
            public static boolean getShowScrollingIndicator(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_drawer_indicator_enable", true);
            }
            public static boolean getFadeScrollingIndicator(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_drawer_indicator_fade", true);
            }
        }

        public static class Dock {

        }

        public static class Icons {

        }

        public static class General {
            public static boolean getAutoRotate(Context context) {
                final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_KEY, 0);
                return preferences.getBoolean("ui_general_orientation", false);
            }
        }
    }

    public static class Application {

    }
}
