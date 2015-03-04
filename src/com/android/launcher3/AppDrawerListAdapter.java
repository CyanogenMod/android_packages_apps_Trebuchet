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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import com.android.launcher3.locale.LocaleSetManager;
import com.android.launcher3.locale.LocaleUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * AppDrawerListAdapter - list adapter for the vertical app drawer
 */
public class AppDrawerListAdapter extends RecyclerView.Adapter<AppDrawerListAdapter.ViewHolder>
        implements View.OnLongClickListener, DragSource, SectionIndexer {

    private static final String NUMERIC_OR_SPECIAL_HEADER = "#";

    private ArrayList<AppItemIndexedInfo> mHeaderList;
    private LayoutInflater mLayoutInflater;

    private Launcher mLauncher;
    private DeviceProfile mDeviceProfile;
    private LinkedHashMap<String, Integer> mSectionHeaders;
    private LinearLayout.LayoutParams mIconParams;
    private Rect mIconRect;
    private LocaleSetManager mLocaleSetManager;

    public enum DrawerType {
        Drawer(0),
        Pager(1);

        private final int mValue;
        private DrawerType(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static DrawerType getModeForValue(int value) {
            switch (value) {
                case 1:
                    return Pager;
                default :
                    return Drawer;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public AutoFitTextView mTextView;
        public ViewGroup mLayout;
        public ViewHolder(View itemView) {
            super(itemView);
            mTextView = (AutoFitTextView) itemView.findViewById(R.id.drawer_item_title);
            mLayout = (ViewGroup) itemView.findViewById(R.id.drawer_item_flow);
        }
    }

    public AppDrawerListAdapter(Launcher launcher) {
        mLauncher = launcher;
        mHeaderList = new ArrayList<AppItemIndexedInfo>();
        mDeviceProfile = LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile();
        mLayoutInflater = LayoutInflater.from(launcher);

        mLocaleSetManager = new LocaleSetManager(mLauncher);
        mLocaleSetManager.updateLocaleSet(mLocaleSetManager.getSystemLocaleSet());
        initParams();
    }

    private void initParams() {
        mIconParams = new
                LinearLayout.LayoutParams(mDeviceProfile.folderCellWidthPx,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        mIconRect = new Rect(0, 0, grid.allAppsIconSizePx, grid.allAppsIconSizePx);
    }

    /**
     * Create and populate mHeaderList (buckets for app sorting)
     * @param info
     */
    public void populateByCharacter(ArrayList<AppInfo> info) {
        if (info == null || info.size() <= 0) {
            Collections.sort(mHeaderList);
            return;
        }

        // Create a clone of AppInfo ArrayList to preserve data
        ArrayList<AppInfo> tempInfo = (ArrayList<AppInfo>) info.clone();

        ArrayList<AppInfo> appInfos = new ArrayList<AppInfo>();

        // get next app
        AppInfo app = tempInfo.get(0);

        // get starting character
        LocaleUtils localeUtils = LocaleUtils.getInstance();
        int bucketIndex = localeUtils.getBucketIndex(app.title.toString());
            String startString
                = localeUtils.getBucketLabel(bucketIndex);
        if (TextUtils.isEmpty(startString)) {
            startString = NUMERIC_OR_SPECIAL_HEADER;
            bucketIndex = localeUtils.getBucketIndex(startString);
        }

        // now iterate through
        for (AppInfo info1 : tempInfo) {
            int newBucketIndex = localeUtils.getBucketIndex(info1.title.toString());

            String newChar
                    = localeUtils.getBucketLabel(newBucketIndex);
            if (TextUtils.isEmpty(newChar)) {
                newChar = NUMERIC_OR_SPECIAL_HEADER;
            }
            // if same character
            if (newChar.equals(startString)) {
                // add it
                appInfos.add(info1);
            }
        }

        Collections.sort(appInfos, LauncherModel.getAppNameComparator());

        for (int i = 0; i < appInfos.size(); i += mDeviceProfile.numColumnsBase) {
            int endIndex = (int) Math.min(i + mDeviceProfile.numColumnsBase, appInfos.size());
            ArrayList<AppInfo> subList = new ArrayList<AppInfo>(appInfos.subList(i, endIndex));
            AppItemIndexedInfo indexInfo;
            indexInfo = new AppItemIndexedInfo(startString, bucketIndex, subList, i != 0);
            mHeaderList.add(indexInfo);
        }

        for (AppInfo remove : appInfos) {
            // remove from mApps
            tempInfo.remove(remove);
        }
        populateByCharacter(tempInfo);
    }

    public void setApps(ArrayList<AppInfo> list) {
        if (!LauncherAppState.isDisableAllApps()) {
            mHeaderList.clear();
            populateByCharacter(list);
            populateSectionHeaders();
            mLauncher.updateScrubber();
            this.notifyDataSetChanged();
        }
    }

    private void populateSectionHeaders() {
        if (mSectionHeaders == null || mSectionHeaders.size() != mHeaderList.size()) {
            mSectionHeaders = new LinkedHashMap<String, Integer>();
        }
        int count = 0;
        for (int i = 0; i < mHeaderList.size(); i++) {
            AppItemIndexedInfo info = mHeaderList.get(i);
            if (!mHeaderList.get(i).isChild) {
                mSectionHeaders.put(String.valueOf(mHeaderList.get(i).mStartString), count);
            }
            if (info.mInfo.size() < mDeviceProfile.numColumnsBase) {
                count++;
            } else {
                count += info.mInfo.size() / mDeviceProfile.numColumnsBase;
            }
        }
    }

    private void reset() {
        ArrayList<AppInfo> infos = getAllApps();
        setApps(infos);
    }

    private ArrayList<AppInfo> getAllApps() {
        ArrayList<AppInfo> indexedInfos = new ArrayList<AppInfo>();

        for (int j = 0; j < mHeaderList.size(); ++j) {
            AppItemIndexedInfo indexedInfo = mHeaderList.get(j);
            for (AppInfo info : indexedInfo.mInfo) {
                indexedInfos.add(info);
            }
        }
        return indexedInfos;
    }

    public void updateApps(ArrayList<AppInfo> list) {
        // We remove and re-add the updated applications list because it's properties may have
        // changed (ie. the title), and this will ensure that the items will be in their proper
        // place in the list.
        if (!LauncherAppState.isDisableAllApps()) {
            removeAppsWithoutInvalidate(list);
            addAppsWithoutInvalidate(list);
            reset();
        }
    }


    public void addApps(ArrayList<AppInfo> list) {
        if (!LauncherAppState.isDisableAllApps()) {
            addAppsWithoutInvalidate(list);
            reset();
        }
    }

    private void addAppsWithoutInvalidate(ArrayList<AppInfo> list) {
        // We add it in place, in alphabetical order
        LocaleUtils localeUtils = LocaleUtils.getInstance();

        int count = list.size();
        for (int i = 0; i < count; ++i) {
            AppInfo info = list.get(i);
            boolean found = false;
            AppItemIndexedInfo lastInfoForSection = null;
            int bucketIndex = localeUtils.getBucketIndex(info.title.toString());
            String start = localeUtils.getBucketLabel(bucketIndex);
            if (TextUtils.isEmpty(start)) {
                start = NUMERIC_OR_SPECIAL_HEADER;
                bucketIndex = localeUtils.getBucketIndex(start);
            }
            for (int j = 0; j < mHeaderList.size(); ++j) {
                AppItemIndexedInfo indexedInfo = mHeaderList.get(j);
                if (start.equals(indexedInfo.mStartString)) {
                    Collections.sort(indexedInfo.mInfo, LauncherModel.getAppNameComparator());
                    int index =
                            Collections.binarySearch(indexedInfo.mInfo,
                                    info, LauncherModel.getAppNameComparator());
                    if (index >= 0) {
                        found = true;
                        break;
                    } else {
                        lastInfoForSection = indexedInfo;
                    }
                }
            }
            if (!found) {
                if (lastInfoForSection != null) {
                    lastInfoForSection.mInfo.add(info);
                } else {
                    // we need to create a new section
                    ArrayList<AppInfo> newInfos = new ArrayList<AppInfo>();
                    newInfos.add(info);
                    AppItemIndexedInfo newInfo =
                            new AppItemIndexedInfo(start, bucketIndex, newInfos, false);
                    mHeaderList.add(newInfo);
                    Collections.sort(mHeaderList);
                }
            }
        }
    }

    public void removeApps(ArrayList<AppInfo> appInfos) {
        if (!LauncherAppState.isDisableAllApps()) {
            removeAppsWithoutInvalidate(appInfos);
            //recreate everything
            reset();
        }
    }

    private void removeAppsWithoutInvalidate(ArrayList<AppInfo> list) {
        // loop through all the apps and remove apps that have the same component
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            AppInfo info = list.get(i);
            for (int j = 0; j < mHeaderList.size(); ++j) {
                AppItemIndexedInfo indexedInfo = mHeaderList.get(j);
                ArrayList<AppInfo> clonedIndexedInfoApps =
                        (ArrayList<AppInfo>) indexedInfo.mInfo.clone();
                int index =
                        findAppByComponent(clonedIndexedInfoApps, info);
                if (index > -1) {
                    indexedInfo.mInfo.remove(info);
                }
            }
        }
    }

    private int findAppByComponent(List<AppInfo> list, AppInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            AppInfo info = list.get(i);
            if (info.intent.getComponent().equals(removeComponent)) {
                return i;
            }
        }
        return -1;
    }

    /*
     * AllAppsView implementation
     */
    public void setup(Launcher launcher) {
        mLauncher = launcher;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.app_drawer_item, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.mTextView.setPadding(0, 0, 0, mDeviceProfile.iconTextSizePx + 10);
        for (int i = 0; i < mDeviceProfile.numColumnsBase; i++) {
            AppDrawerIconView icon = (AppDrawerIconView) mLayoutInflater.inflate(
                    R.layout.drawer_icon, holder.mLayout, false);
            icon.setLayoutParams(mIconParams);
            icon.setOnClickListener(mLauncher);
            icon.setOnLongClickListener(this);
            int padding = (int) mLauncher.getResources()
                    .getDimension(R.dimen.vertical_app_drawer_icon_padding);
            icon.setPadding(padding, padding, padding, padding);
            holder.mLayout.addView(icon);
        }
        return holder;
    }

    @Override
    public int getItemCount() {
        return mHeaderList.size();
    }

    public AppItemIndexedInfo getItemAt(int position) {
        if (position < mHeaderList.size())
            return mHeaderList.get(position);
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppItemIndexedInfo indexedInfo = mHeaderList.get(position);
        holder.mTextView.setVisibility(indexedInfo.isChild ? View.INVISIBLE : View.VISIBLE);
        if (!indexedInfo.isChild) {
            if (indexedInfo.mStartString.equals(NUMERIC_OR_SPECIAL_HEADER)) {
                holder.mTextView.setText(NUMERIC_OR_SPECIAL_HEADER);
            } else {
                holder.mTextView.setText(String.valueOf(indexedInfo.mStartString));
            }
        }
        final int size = indexedInfo.mInfo.size();
        for (int i = 0; i < holder.mLayout.getChildCount(); i++) {
            AppDrawerIconView icon = (AppDrawerIconView) holder.mLayout.getChildAt(i);
            if (i >= size) {
                icon.setVisibility(View.INVISIBLE);
            } else {
                icon.setVisibility(View.VISIBLE);
                AppInfo info = indexedInfo.mInfo.get(i);
                icon.setTag(info);
                Drawable d = Utilities.createIconDrawable(info.iconBitmap);
                d.setBounds(mIconRect);
                icon.mIcon.setImageDrawable(d);
                icon.mLabel.setText(info.title);
            }
        }
        holder.itemView.setTag(indexedInfo);
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof AppDrawerIconView) {
            beginDraggingApplication(v);
            mLauncher.enterSpringLoadedDragMode();
        }
        return false;
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete,
                                boolean success) {
        // Return early and wait for onFlingToDeleteCompleted if this was the result of a fling
        if (isFlingToDelete) return;

        endDragging(target, false, success);

        // Display an error message if the drag failed due to there not being enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    layout.calculateSpans(itemInfo);
                    showOutOfSpaceMessage =
                            !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage(false);
            }

            d.deferDragViewCleanupPostAnimation = false;
        }
    }

    /**
     * Clean up after dragging.
     *
     * @param target where the item was dragged to (can be null if the item was flung)
     */
    private void endDragging(View target, boolean isFlingToDelete, boolean success) {
        if (isFlingToDelete || !success || (target != mLauncher.getWorkspace() &&
                !(target instanceof DeleteDropTarget) && !(target instanceof Folder))) {
            // Exit spring loaded mode if we have not successfully dropped or have not handled the
            // drop in Workspace
            mLauncher.getWorkspace().removeExtraEmptyScreenDelayed(true, new Runnable() {
                @Override
                public void run() {
                    mLauncher.exitSpringLoadedDragMode();
                    mLauncher.unlockScreenOrientation(false);
                }
            }, 0, true);
        } else {
            mLauncher.unlockScreenOrientation(false);
        }
    }

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return true;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return (float) mDeviceProfile.allAppsIconSizePx / mDeviceProfile.iconSizePx;
    }

    private void beginDraggingApplication(View v) {
        mLauncher.getWorkspace().beginDragShared(v, this);
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // We just dismiss the drag when we fling, so cleanup here
    }

    public class AppItemIndexedInfo implements Comparable {
        private boolean isChild;
        private String mStartString;
        private int mStringIndex;
        private ArrayList<AppInfo> mInfo;

        private AppItemIndexedInfo(String startString, int bucketIndex, ArrayList<AppInfo> info,
                boolean isChild) {
            this.mStartString = startString;
            this.mStringIndex = bucketIndex;
            this.mInfo = info;
            this.isChild = isChild;

            if (mStartString.equals(NUMERIC_OR_SPECIAL_HEADER)) {
                this.mStringIndex = 0;
            }
        }

        public String getString() {
            return mStartString;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof AppItemIndexedInfo) {
                int otherBucketIndex = ((AppItemIndexedInfo) o).mStringIndex;
                return Integer.compare(mStringIndex, otherBucketIndex);
            }
            return 0;
        }
    }

    @Override
    public Object[] getSections() {
        return mSectionHeaders.keySet().toArray(new String[mSectionHeaders.size()]);
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionHeaders.get(getSections()[sectionIndex]);
    }

    @Override
    public int getSectionForPosition(int position) {
        return mSectionHeaders.get(mHeaderList.get(position).mStartString);
    }
}
