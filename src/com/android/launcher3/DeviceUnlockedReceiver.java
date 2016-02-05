/*
 * Copyright (C) 2016 The CyanogenMod Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.HashSet;
import java.util.Set;

public class DeviceUnlockedReceiver extends BroadcastReceiver {
    public static final String INTENT_ACTION = Intent.ACTION_USER_PRESENT;

    private final Set<DeviceUnlockedListener> mListeners;

    interface DeviceUnlockedListener {
        void onDeviceUnlocked();
    }

    public DeviceUnlockedReceiver() {
        mListeners = new HashSet<DeviceUnlockedListener>();
    }

    public void registerListener(final DeviceUnlockedListener listener) {
        mListeners.add(listener);
    }

    public void deregisterListener(final DeviceUnlockedListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(INTENT_ACTION)) return;

        for (DeviceUnlockedListener listener: mListeners) {
            listener.onDeviceUnlocked();
        }
    }
}
