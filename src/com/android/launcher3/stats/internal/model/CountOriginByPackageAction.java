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

import android.os.Bundle;
import android.text.TextUtils;
import com.android.launcher3.stats.external.TrackingBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 *     This is an action to send a count of events with common origins
 * </pre>
 */
public class CountOriginByPackageAction implements ITrackingAction {

    public static final String TRACKING_ACTION = "count_by_origin";

    @Override
    public String toString() {
        return TRACKING_ACTION;
    }

    @Override
    public List<Bundle> createTrackingBundles(String trackingId, TrackingEvent.Category category,
                                              List<TrackingEvent> eventList) {
        // Make an origin mapper
        Map<String, Map<String, List<TrackingEvent>>> originEventMap =
                new HashMap<String, Map<String, List<TrackingEvent>>>();

        // Parse the event list and categorize by origin
        for (TrackingEvent event : eventList) {
            // We are parsing for things with origin, if no origin is set, discard it!
            if (TextUtils.isEmpty(event.getMetaData(TrackingEvent.KEY_ORIGIN))) {
                continue;
            }
            String originKey = event.getMetaData(TrackingEvent.KEY_ORIGIN);
            if (!originEventMap.containsKey(originKey)) {
                HashMap<String, List<TrackingEvent>> newMap =
                        new HashMap<String, List<TrackingEvent>>();
                originEventMap.put(originKey, newMap);
            }
            String packageName = event.getMetaData(TrackingEvent.KEY_PACKAGE);
            // Set a default so our iteration picks it up and just discard package metadata
            packageName = (TextUtils.isEmpty(packageName)) ? trackingId : packageName;
            if (!originEventMap.get(originKey).containsKey(packageName)) {
                originEventMap.get(originKey).put(packageName, new ArrayList<TrackingEvent>());
            }
            originEventMap.get(originKey).get(packageName).add(event);
        }

        // Start building result tracking bundles
        List<Bundle> bundleList = new ArrayList<Bundle>();
        for (Map.Entry<String, Map<String, List<TrackingEvent>>> entry :
                originEventMap.entrySet()) {
            String origin = entry.getKey();
            for (Map.Entry<String, List<TrackingEvent>> entry2 : entry.getValue().entrySet()) {
                String pkg = entry2.getKey();
                List<TrackingEvent> events = entry2.getValue();
                Bundle bundle = TrackingBundle.createTrackingBundle(trackingId, category.name(),
                        TRACKING_ACTION);
                bundle.putString(TrackingBundle.KEY_METADATA_ORIGIN, origin);
                bundle.putInt(TrackingBundle.KEY_METADATA_VALUE, events.size());
                if (!trackingId.equals(pkg)) {
                    bundle.putString(TrackingBundle.KEY_METADATA_PACKAGE, pkg);
                }
                bundleList.add(bundle);
            }
        }
        return bundleList;
    }

}
