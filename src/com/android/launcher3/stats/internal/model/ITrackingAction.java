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

import java.util.List;

/**
 * <pre>
 *     This is an action we want to perfrom from a report.
 *
 *     e.g.
 *          1. I want to get the COUNT of widgets added
 *          2. I want to get the origin of app launches
 * </pre>
 */
public interface ITrackingAction {

    /**
     * Creates a new bundle used to tracking events
     *
     * @param trackingId {@link String}
     * @param category {@link com.android.launcher3.stats.internal.model.TrackingEvent.Category}
     * @param eventList {@link List}
     * @return {@link List}
     */
    List<Bundle> createTrackingBundles(String trackingId, TrackingEvent.Category category,
                                            List<TrackingEvent> eventList);

}
