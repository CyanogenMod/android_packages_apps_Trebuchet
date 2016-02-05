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
import android.net.ConnectivityManager;

import java.util.HashSet;
import java.util.Set;

public class NetworkConnectionReceiver extends BroadcastReceiver {
    public static final String INTENT_ACTION = ConnectivityManager.CONNECTIVITY_ACTION;

    private final Set<NetworkStateChangeListener> mListeners;

    interface NetworkStateChangeListener {
        void onNetworkConnected();
        void onNetworkDisconnected();
    }

    public NetworkConnectionReceiver() {
        mListeners = new HashSet<NetworkStateChangeListener>();
    }

    public void registerListener(final NetworkStateChangeListener listener) {
        mListeners.add(listener);
    }

    public void deregisterListener(final NetworkStateChangeListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(INTENT_ACTION)) return;

        boolean connected = Utilities.isNetworkConnected(context);
        for (NetworkStateChangeListener listener: mListeners) {
            if (connected) {
                listener.onNetworkConnected();
            } else {
                listener.onNetworkDisconnected();
            }
        }
    }
}
