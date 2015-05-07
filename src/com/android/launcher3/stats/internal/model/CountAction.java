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
 *     Handles the specific for sending a tracking event
 * </pre>
 *
 * @see {@link ITrackingAction}
 */
public class CountAction implements ITrackingAction {

    public static final String TRACKING_ACTION = "count";

    @Override
    public String toString() {
        return TRACKING_ACTION;
    }

    @Override
    public List<Bundle> createTrackingBundles(String trackingId, TrackingEvent.Category category,
                                              List<TrackingEvent> eventList) {

        Map<String, List<TrackingEvent>> eventPackageMap =
                new HashMap<String, List<TrackingEvent>>();

        for (TrackingEvent event : eventList) {
            String pkg = event.getMetaData(TrackingEvent.KEY_PACKAGE);
            pkg = (TextUtils.isEmpty(pkg)) ? trackingId : pkg;
            if (!eventPackageMap.containsKey(pkg)) {
                eventPackageMap.put(pkg, new ArrayList<TrackingEvent>());
            }
            eventPackageMap.get(pkg).add(event);
        }

        List<Bundle> bundleList = new ArrayList<Bundle>();
        for (Map.Entry<String, List<TrackingEvent>> entry : eventPackageMap.entrySet()) {
            Bundle bundle = TrackingBundle.createTrackingBundle(trackingId, category.name(),
                    TRACKING_ACTION);
            bundle.putInt(TrackingBundle.KEY_METADATA_VALUE, entry.getValue().size());
            String pkg = entry.getKey();
            if (!pkg.equals(trackingId)) {
                bundle.putString(TrackingBundle.KEY_METADATA_PACKAGE, pkg);
            }
            bundleList.add(bundle);
        }
        return bundleList;
    }
}
