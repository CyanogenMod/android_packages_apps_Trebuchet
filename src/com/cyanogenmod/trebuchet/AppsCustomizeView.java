/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.cyanogenmod.trebuchet;

import java.util.ArrayList;

public interface AppsCustomizeView {

    public enum ContentType {
        Apps,
        Widgets
    }

    public enum SortMode {
        Title,
        InstallDate
    }

    public void setup(Launcher launcher, DragController dragController);

    public ContentType getContentType();

    public void setContentType(ContentType type);

    public boolean isContentType(ContentType type);

    public SortMode getSortMode();

    public void showIndicator(boolean immediately);

    public void hideIndicator(boolean immediately);

    public void loadContent();

    public void loadContent(boolean immediately);

    public void onTabChanged(ContentType type);

    public void showAllAppsCling();

    public void setCurrentToApps();

    public void setCurrentToWidgets();

    public void setSortMode(SortMode mode);

    public void setApps(ArrayList<ApplicationInfo> list);

    public void addApps(ArrayList<ApplicationInfo> list);

    public void removeApps(ArrayList<ApplicationInfo> list);

    public void updateApps(ArrayList<ApplicationInfo> list);

    public void onPackagesUpdated();

    public void reset();

    public void clearAllWidgetPreviews();

    public int getSaveInstanceStateIndex();

    public void restore(int restoreIndex);

    public void dumpState();

    public void surrender();
}
