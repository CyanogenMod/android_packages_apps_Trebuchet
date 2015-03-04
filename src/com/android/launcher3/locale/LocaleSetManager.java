/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2009 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.launcher3.locale;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;

import java.util.Locale;

import libcore.icu.ICU;

public class LocaleSetManager {
    private static final String TAG = LocaleSetManager.class.getSimpleName();

    private LocaleSet mCurrentLocales;
    private final Context mContext;

    public LocaleSetManager(final Context context) {
        mContext = context;
    }

    /**
     * Sets up the locale set
     * @param localeSet value to set it to
     */
    public void updateLocaleSet(LocaleSet localeSet) {
        Log.d(TAG, "Locale Changed from: " + mCurrentLocales + " to " + localeSet);
        mCurrentLocales = localeSet;
        LocaleUtils.getInstance().setLocales(mCurrentLocales);
    }

    /**
     * This takes an old and new locale set and creates a combined locale set.  If they share a
     * primary then the old one is returned
     * @return the combined locale set
     */
    private static LocaleSet getCombinedLocaleSet(LocaleSet oldLocales, Locale newLocale) {
        Locale prevLocale = null;

        if (oldLocales != null) {
            prevLocale = oldLocales.getPrimaryLocale();
            // If primary locale is unchanged then no change to locale set.
            if (newLocale.equals(prevLocale)) {
                return oldLocales;
            }
        }

        // Otherwise, construct a new locale set based on the new locale
        // and the previous primary locale.
        return new LocaleSet(newLocale, prevLocale).normalize();
    }

    /**
     * @return the system locale set
     */
    public LocaleSet getSystemLocaleSet() {
        final Locale curLocale = getLocale();
        return getCombinedLocaleSet(mCurrentLocales, curLocale);
    }

    @VisibleForTesting
    protected Locale getLocale() {
        return Locale.getDefault();
    }
}
