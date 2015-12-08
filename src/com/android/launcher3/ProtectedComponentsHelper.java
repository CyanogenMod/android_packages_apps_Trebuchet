/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.content.ComponentName;
import android.content.Context;
import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;

public class ProtectedComponentsHelper {
    private static final int FILTER_APPS_SYSTEM_FLAG = 1;
    private static final int FILTER_APPS_DOWNLOADED_FLAG = 2;
    private static int sFilterApps = FILTER_APPS_SYSTEM_FLAG | FILTER_APPS_DOWNLOADED_FLAG;

    private static ArrayList<ComponentName> sProtectedApps = new ArrayList<ComponentName>();
    private static ArrayList<String> sProtectedPackages = new ArrayList<String>();

    /**
     * Gets the list of protected components from {@link CMSettings} and updates the existing list
     * of protected apps and packages
     * @param context Context
     */
    public static void updateProtectedComponentsLists(Context context) {
        String protectedComponents = CMSettings.Secure.getString(context.getContentResolver(),
                CMSettings.Secure.PROTECTED_COMPONENTS);
        protectedComponents = protectedComponents == null ? "" : protectedComponents;
        String [] flattened = protectedComponents.split("\\|");
        sProtectedApps = new ArrayList<ComponentName>(flattened.length);
        sProtectedPackages = new ArrayList<String>(flattened.length);
        for (String flat : flattened) {
            ComponentName cmp = ComponentName.unflattenFromString(flat);
            if (cmp != null) {
                sProtectedApps.add(cmp);
                sProtectedPackages.add(cmp.getPackageName());
            }
        }
    }

    /**
     * Checks if the given combination of {@link ComponentName} and flags is for a protected app
     */
    public static boolean isProtectedApp(int flags, ComponentName componentName) {
        boolean system = isSystemFlag(flags);
        return sProtectedApps.contains(componentName) || (system && !getShowSystemApps()) ||
                (!system && !getShowDownloadedApps());
    }

    /**
     * Checks if the given combination of package name and flags is for a protected package
     */
    public static boolean isProtectedPackage(int flags, String packageName) {
        boolean system = isSystemFlag(flags);
        return (sProtectedPackages.contains(packageName) || (system && !getShowSystemApps()) ||
                (!system && !getShowDownloadedApps()));
    }

    private static boolean isSystemFlag(int flags) {
        return (flags & AppInfo.DOWNLOADED_FLAG) == 0;
    }

    private static boolean getShowSystemApps() {
        return (sFilterApps & FILTER_APPS_SYSTEM_FLAG) != 0;
    }

    private static boolean getShowDownloadedApps() {
        return (sFilterApps & FILTER_APPS_DOWNLOADED_FLAG) != 0;
    }
}
