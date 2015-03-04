/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.text.TextUtils;
import com.google.common.annotations.VisibleForTesting;
import java.util.Locale;

public class LocaleSet {
    private static final String CHINESE_LANGUAGE = Locale.CHINESE.getLanguage().toLowerCase();
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();
    private static final String KOREAN_LANGUAGE = Locale.KOREAN.getLanguage().toLowerCase();

    private static class LocaleWrapper {
        private final Locale mLocale;
        private final String mLanguage;
        private final boolean mLocaleIsCJK;

        private static boolean isLanguageCJK(String language) {
            return CHINESE_LANGUAGE.equals(language) ||
                    JAPANESE_LANGUAGE.equals(language) ||
                    KOREAN_LANGUAGE.equals(language);
        }

        public LocaleWrapper(Locale locale) {
            mLocale = locale;
            if (mLocale != null) {
                mLanguage = mLocale.getLanguage().toLowerCase();
                mLocaleIsCJK = isLanguageCJK(mLanguage);
            } else {
                mLanguage = null;
                mLocaleIsCJK = false;
            }
        }

        public boolean hasLocale() {
            return mLocale != null;
        }

        public Locale getLocale() {
            return mLocale;
        }

        public boolean isLocale(Locale locale) {
            return mLocale == null ? (locale == null) : mLocale.equals(locale);
        }

        public boolean isLocaleCJK() {
            return mLocaleIsCJK;
        }

        public boolean isLanguage(String language) {
            return mLanguage == null ? (language == null)
                    : mLanguage.equalsIgnoreCase(language);
        }

        public String toString() {
            return mLocale != null ? mLocale.toLanguageTag() : "(null)";
        }
    }

    public static LocaleSet getDefault() {
        return new LocaleSet(Locale.getDefault());
    }

    public LocaleSet(Locale locale) {
        this(locale, null);
    }

    /**
     * Returns locale set for a given set of IETF BCP-47 tags separated by ';'.
     * BCP-47 tags are what is used by ICU 52's toLanguageTag/forLanguageTag
     * methods to represent individual Locales: "en-US" for Locale.US,
     * "zh-CN" for Locale.CHINA, etc. So eg "en-US;zh-CN" specifies the locale
     * set LocaleSet(Locale.US, Locale.CHINA).
     *
     * @param localeString One or more BCP-47 tags separated by ';'.
     * @return LocaleSet for specified locale string, or default set if null
     * or unable to parse.
     */
    public static LocaleSet getLocaleSet(String localeString) {
        // Locale.toString() generates strings like "en_US" and "zh_CN_#Hans".
        // Locale.toLanguageTag() generates strings like "en-US" and "zh-Hans-CN".
        // We can only parse language tags.
        if (localeString != null && localeString.indexOf('_') == -1) {
            final String[] locales = localeString.split(";");
            final Locale primaryLocale = Locale.forLanguageTag(locales[0]);
            // ICU tags undefined/unparseable locales "und"
            if (primaryLocale != null &&
                    !TextUtils.equals(primaryLocale.toLanguageTag(), "und")) {
                if (locales.length > 1 && locales[1] != null) {
                    final Locale secondaryLocale = Locale.forLanguageTag(locales[1]);
                    if (secondaryLocale != null &&
                            !TextUtils.equals(secondaryLocale.toLanguageTag(), "und")) {
                        return new LocaleSet(primaryLocale, secondaryLocale);
                    }
                }
                return new LocaleSet(primaryLocale);
            }
        }
        return getDefault();
    }

    private final LocaleWrapper mPrimaryLocale;
    private final LocaleWrapper mSecondaryLocale;

    public LocaleSet(Locale primaryLocale, Locale secondaryLocale) {
        mPrimaryLocale = new LocaleWrapper(primaryLocale);
        mSecondaryLocale = new LocaleWrapper(
                mPrimaryLocale.equals(secondaryLocale) ? null : secondaryLocale);
    }

    public LocaleSet normalize() {
        final Locale primaryLocale = getPrimaryLocale();
        if (primaryLocale == null) {
            return getDefault();
        }
        Locale secondaryLocale = getSecondaryLocale();
        // disallow both locales with same language (redundant and/or conflicting)
        // disallow both locales CJK (conflicting rules)
        if (secondaryLocale == null ||
                isPrimaryLanguage(secondaryLocale.getLanguage()) ||
                (isPrimaryLocaleCJK() && isSecondaryLocaleCJK())) {
            return new LocaleSet(primaryLocale);
        }
        // unnecessary to specify English as secondary locale (redundant)
        if (isSecondaryLanguage(Locale.ENGLISH.getLanguage())) {
            return new LocaleSet(primaryLocale);
        }
        return this;
    }

    public boolean hasSecondaryLocale() {
        return mSecondaryLocale.hasLocale();
    }

    public Locale getPrimaryLocale() {
        return mPrimaryLocale.getLocale();
    }

    public Locale getSecondaryLocale() {
        return mSecondaryLocale.getLocale();
    }

    public boolean isPrimaryLocale(Locale locale) {
        return mPrimaryLocale.isLocale(locale);
    }

    public boolean isSecondaryLocale(Locale locale) {
        return mSecondaryLocale.isLocale(locale);
    }

    private static final String SCRIPT_SIMPLIFIED_CHINESE = "Hans";
    private static final String SCRIPT_TRADITIONAL_CHINESE = "Hant";

    @VisibleForTesting
    public static boolean isLocaleSimplifiedChinese(Locale locale) {
        // language must match
        if (locale == null || !TextUtils.equals(locale.getLanguage(), CHINESE_LANGUAGE)) {
            return false;
        }
        // script is optional but if present must match
        if (!TextUtils.isEmpty(locale.getScript())) {
            return locale.getScript().equals(SCRIPT_SIMPLIFIED_CHINESE);
        }
        // if no script, must match known country
        return locale.equals(Locale.SIMPLIFIED_CHINESE);
    }

    public boolean isPrimaryLocaleSimplifiedChinese() {
        return isLocaleSimplifiedChinese(getPrimaryLocale());
    }

    public boolean isSecondaryLocaleSimplifiedChinese() {
        return isLocaleSimplifiedChinese(getSecondaryLocale());
    }

    @VisibleForTesting
    public static boolean isLocaleTraditionalChinese(Locale locale) {
        // language must match
        if (locale == null || !TextUtils.equals(locale.getLanguage(), CHINESE_LANGUAGE)) {
            return false;
        }
        // script is optional but if present must match
        if (!TextUtils.isEmpty(locale.getScript())) {
            return locale.getScript().equals(SCRIPT_TRADITIONAL_CHINESE);
        }
        // if no script, must match known country
        return locale.equals(Locale.TRADITIONAL_CHINESE);
    }

    public boolean isPrimaryLocaleTraditionalChinese() {
        return isLocaleTraditionalChinese(getPrimaryLocale());
    }

    public boolean isSecondaryLocaleTraditionalChinese() {
        return isLocaleTraditionalChinese(getSecondaryLocale());
    }

    public boolean isPrimaryLocaleCJK() {
        return mPrimaryLocale.isLocaleCJK();
    }

    public boolean isSecondaryLocaleCJK() {
        return mSecondaryLocale.isLocaleCJK();
    }

    public boolean isPrimaryLanguage(String language) {
        return mPrimaryLocale.isLanguage(language);
    }

    public boolean isSecondaryLanguage(String language) {
        return mSecondaryLocale.isLanguage(language);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof LocaleSet) {
            final LocaleSet other = (LocaleSet) object;
            return other.isPrimaryLocale(mPrimaryLocale.getLocale())
                    && other.isSecondaryLocale(mSecondaryLocale.getLocale());
        }
        return false;
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(mPrimaryLocale.toString());
        if (hasSecondaryLocale()) {
            builder.append(";");
            builder.append(mSecondaryLocale.toString());
        }
        return builder.toString();
    }
}
