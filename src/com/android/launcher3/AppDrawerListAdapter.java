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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import com.android.launcher3.locale.LocaleSetManager;
import com.android.launcher3.locale.LocaleUtils;
import com.android.launcher3.settings.SettingsProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * AppDrawerListAdapter - list adapter for the vertical app drawer
 */
public class AppDrawerListAdapter extends RecyclerView.Adapter<AppDrawerListAdapter.ViewHolder>
        implements View.OnLongClickListener, DragSource, SectionIndexer {

    private static final String TAG = AppDrawerListAdapter.class.getSimpleName();
    private static final String NUMERIC_OR_SPECIAL_HEADER = "#";

    /**
     * Tracks both the section index and the positional item index for the sections
     * section:     0 0 0 1 1 2 3 4 4
     * itemIndex:   0 1 2 3 4 5 6 7 8
     * Sections:    A A A B B C D E E
     */
    private static class SectionIndices {
        public int mSectionIndex;
        public int mItemIndex;
        public SectionIndices(int sectionIndex, int itemIndex) {
            mSectionIndex = sectionIndex;
            mItemIndex = itemIndex;
        }
    }

    private ArrayList<AppItemIndexedInfo> mHeaderList;
    private LayoutInflater mLayoutInflater;

    private Launcher mLauncher;
    private DeviceProfile mDeviceProfile;
    private LinkedHashMap<String, SectionIndices> mSectionHeaders;
    private LinearLayout.LayoutParams mIconParams;
    private Rect mIconRect;
    private int mNumColumns = 0;
    private int mExtraPadding = 0;
    private LocaleSetManager mLocaleSetManager;

    private ArrayList<ComponentName> mProtectedApps;

    private boolean mHideIconLabels;

    private ItemAnimatorSet mItemAnimatorSet;

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
        public View mContainerView;
        public View mFadingBackgroundBackView;
        public View mFadingBackgroundFrontView;
        public ViewHolder(View itemView) {
            super(itemView);
            mContainerView = itemView;
            mFadingBackgroundBackView = itemView.findViewById(R.id.fading_background_back);
            mFadingBackgroundFrontView = itemView.findViewById(R.id.fading_background_front);
            mTextView = (AutoFitTextView) itemView.findViewById(R.id.drawer_item_title);
            mTextView.bringToFront();
            mLayout = (ViewGroup) itemView.findViewById(R.id.drawer_item_flow);
        }
    }

    /**
     * This class handles animating the different items when the user scrolls through the drawer
     * quickly
     */
    private class ItemAnimatorSet {
        private static final long ANIMATION_DURATION = 200;
        private static final float MAX_SCALE = 2f;
        private static final float MIN_SCALE = 1f;
        private static final float FAST_SCROLL = 0.3f;
        private static final int NO_SECTION_TARGET = -1;

        private final float YDPI;
        private final HashSet<ViewHolder> mViewHolderSet;
        private final Interpolator mInterpolator;
        private final View.OnLayoutChangeListener mLayoutChangeListener;

        private boolean mDragging;
        private boolean mExpanding;
        private boolean mPendingShrink;
        private long mStartTime;
        private int mScrollState;
        private float mFastScrollSpeed;
        private float mLastScrollSpeed;

        // If the user is scrubbing, we want to highlight the target section differently,
        // so we use this to track where the user is currently scrubbing to
        private int mSectionTarget;

        public ItemAnimatorSet(Context ctx) {
            mDragging = false;
            mExpanding = false;
            mPendingShrink = false;
            mScrollState = RecyclerView.SCROLL_STATE_IDLE;
            mSectionTarget = NO_SECTION_TARGET;
            mViewHolderSet = new HashSet<>();
            mInterpolator = new DecelerateInterpolator();
            YDPI = ctx.getResources().getDisplayMetrics().ydpi;

            mLayoutChangeListener = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    // set the pivot of the text view
                    v.setPivotX(v.getMeasuredWidth() / 3);
                    v.setPivotY(v.getMeasuredHeight() / 2);
                }
            };
        }

        public void add(ViewHolder holder) {
            mViewHolderSet.add(holder);
            holder.mTextView.addOnLayoutChangeListener(mLayoutChangeListener);

            createAnimationHook(holder);
        }

        public void remove(ViewHolder holder) {
            mViewHolderSet.remove(holder);
            holder.mTextView.removeOnLayoutChangeListener(mLayoutChangeListener);
        }

        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState != mScrollState) {
                mScrollState = newState;
                mFastScrollSpeed = 0;
                checkAnimationState();

                // If the user is dragging, clear the section target
                if (mScrollState == RecyclerView.SCROLL_STATE_DRAGGING
                        && mSectionTarget != NO_SECTION_TARGET) {
                    setSectionTarget(NO_SECTION_TARGET);
                }
            }
        }

        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (mScrollState == RecyclerView.SCROLL_STATE_SETTLING) {
                mLastScrollSpeed = Math.abs(dy / YDPI);
                // get the max of the current scroll speed and the previous fastest scroll speed
                mFastScrollSpeed = Math.max(mFastScrollSpeed, mLastScrollSpeed);
                checkAnimationState();
            }
        }

        public void setDragging(boolean dragging) {
            mDragging = dragging;
            checkAnimationState();
        }

        private void checkAnimationState() {
            // if the user is dragging or if we're settling at a fast speed, then show animation
            showAnimation(mDragging ||
                    (mScrollState == RecyclerView.SCROLL_STATE_SETTLING &&
                    mFastScrollSpeed >= FAST_SCROLL));
        }

        private void showAnimation(boolean expanding) {
            if (mExpanding != expanding) {
                // if near the top or bottom and flick to that side of the list, the scroll speed
                // will hit 0 and the animation will cut straight to shrinking. This code
                // is here to allow the expand animation to complete in that specific scenario
                // before shrinking
                // if the user isn't dragging, the scroll state is idle, the last scroll is fast and
                // the expand animation is still playing, then mark pending shrink as true
                if (!mDragging
                        && mScrollState == RecyclerView.SCROLL_STATE_IDLE
                        && mLastScrollSpeed > FAST_SCROLL
                        && System.currentTimeMillis() - mStartTime < ANIMATION_DURATION) {
                    mPendingShrink = true;
                    return;
                }

                mExpanding = expanding;
                mPendingShrink = false;
                mStartTime = System.currentTimeMillis();

                for (ViewHolder holder : mViewHolderSet) {
                    createAnimationHook(holder);
                }
            }
        }

        public void createAnimationHook(final ViewHolder holder) {
            holder.mTextView.animate().cancel();
            holder.mTextView.animate()
                    .setUpdateListener(new ItemAnimator(holder, mItemAnimatorSet))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            animateStart(holder, animation);
                        }

                        @Override
                        public void onAnimationEnd(final Animator animation) {
                            animateEnd(holder, animation);
                        }

                        @Override
                        public void onAnimationCancel(final Animator animation) {
                            animateCancel(holder, animation);
                        }
                    })
                    .setDuration(ANIMATION_DURATION)
                    .start();
        }

        public void animateStart(ViewHolder holder, Animator animation) {
            setLayerType(holder, View.LAYER_TYPE_HARDWARE);
        }

        public void animateEnd(ViewHolder holder, Animator animation) {
            animate(holder, animation, 1f);
            setLayerType(holder, View.LAYER_TYPE_NONE);
        }

        public void animateCancel(ViewHolder holder, Animator animation) {
            animate(holder, animation, 1f);
            setLayerType(holder, View.LAYER_TYPE_NONE);

        }

        private void setLayerType(ViewHolder holder, int layerType) {
            holder.mFadingBackgroundBackView.setLayerType(layerType, null);
            holder.mFadingBackgroundFrontView.setLayerType(layerType, null);
        }

        public void animate(ViewHolder holder, Animator animation) {
            long diffTime = System.currentTimeMillis() - mStartTime;

            float percentage = Math.min(diffTime / (float) ANIMATION_DURATION, 1f);

            animate(holder, animation, percentage);

            if (diffTime >= ANIMATION_DURATION) {
                if (animation != null) {
                    animation.cancel();
                }

                if (mPendingShrink) {
                    mPendingShrink = false;
                    mLastScrollSpeed = 0;
                    checkAnimationState();
                }

            }
        }

        public void animate(ViewHolder holder, Animator animation, float percentage) {
            percentage = mInterpolator.getInterpolation(percentage);

            if (!mExpanding) {
                percentage = 1 - percentage;
            }

            // Scale header text letters
            final float targetScale = (MAX_SCALE - MIN_SCALE) * percentage + MIN_SCALE;
            holder.mTextView.setScaleX(targetScale);
            holder.mTextView.setScaleY(targetScale);

            // Perform animation
            if (getSectionForPosition(holder.getPosition()) == mSectionTarget) {
                holder.mFadingBackgroundFrontView.setVisibility(View.INVISIBLE);
                holder.mFadingBackgroundBackView.setAlpha(percentage);
                holder.mFadingBackgroundBackView.setVisibility(View.VISIBLE);
            } else {
                holder.mFadingBackgroundBackView.setVisibility(View.INVISIBLE);
                holder.mFadingBackgroundFrontView.setAlpha(percentage);
                holder.mFadingBackgroundFrontView.setVisibility(View.VISIBLE);
            }

        }

        /**
         * Sets the section index to highlight different from the rest when scrubbing
         */
        public void setSectionTarget(int sectionIndex) {
            mSectionTarget = sectionIndex;
            for (ViewHolder holder : mViewHolderSet) {
                animate(holder, null);
            }
        }
    }

    private class ItemAnimator implements ValueAnimator.AnimatorUpdateListener {
        private ViewHolder mViewHolder;
        private ItemAnimatorSet mAnimatorSet;

        public ItemAnimator(final ViewHolder holder, final ItemAnimatorSet animatorSet) {
            mViewHolder = holder;
            mAnimatorSet = animatorSet;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mItemAnimatorSet.animate(mViewHolder, animation);
        }
    }

    public AppDrawerListAdapter(Launcher launcher) {
        mLauncher = launcher;
        mHeaderList = new ArrayList<AppItemIndexedInfo>();
        mLayoutInflater = LayoutInflater.from(launcher);

        mLocaleSetManager = new LocaleSetManager(mLauncher);
        mLocaleSetManager.updateLocaleSet(mLocaleSetManager.getSystemLocaleSet());
        mItemAnimatorSet = new ItemAnimatorSet(launcher);
        initParams();

        updateProtectedAppsList(mLauncher);
    }

    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        mItemAnimatorSet.onScrollStateChanged(recyclerView, newState);
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        mItemAnimatorSet.onScrolled(recyclerView, dx, dy);
    }


    public void setDragging(boolean dragging) {
        mItemAnimatorSet.setDragging(dragging);
    }

    /**
     * Sets the section index to highlight different from the rest when scrubbing
     */
    public void setSectionTarget(int sectionIndex) {
        mItemAnimatorSet.setSectionTarget(sectionIndex);
    }

    private void initParams() {
        mDeviceProfile = LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile();

        int iconWidth = mDeviceProfile.allAppsIconSizePx + 2 * mDeviceProfile.edgeMarginPx;

        // set aside the space needed for the container character
        int neededWidth = mLauncher.getResources()
                .getDimensionPixelSize(R.dimen.app_drawer_char_width);
        int availableWidth = mDeviceProfile.availableWidthPx - neededWidth;
        mNumColumns = (int) Math.floor(availableWidth / iconWidth);
        int leftOverPx = availableWidth % iconWidth;
        // The leftOverPx need to be divided into parts that can be applied as margins to the app
        // icons. Since we cannot add to the far left or far right margin, break up the leftOverPx
        // into (numColumns - 1) parts. Divide this value by 2 so that we divide the value equally
        // between each appIcon in the row.
        mExtraPadding = (leftOverPx / (mNumColumns - 1)) / 2;

        mIconParams = new LinearLayout.LayoutParams(iconWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        mIconParams.topMargin = mDeviceProfile.edgeMarginPx;
        mIconParams.bottomMargin = mDeviceProfile.edgeMarginPx;
        mIconParams.gravity = Gravity.CENTER;
        mIconRect = new Rect(0, 0, mDeviceProfile.allAppsIconSizePx,
                mDeviceProfile.allAppsIconSizePx);

        mHideIconLabels = SettingsProvider.getBoolean(mLauncher,
                SettingsProvider.SETTINGS_UI_DRAWER_HIDE_ICON_LABELS,
                R.bool.preferences_interface_drawer_hide_icon_labels_default);
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

        for (int i = 0; i < appInfos.size(); i += mNumColumns) {
            int endIndex = (int) Math.min(i + mNumColumns, appInfos.size());
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
            initParams();

            filterProtectedApps(list);

            mHeaderList.clear();
            populateByCharacter(list);
            populateSectionHeaders();
            mLauncher.updateScrubber();
            this.notifyDataSetChanged();
        }
    }

    private void populateSectionHeaders() {
        if (mSectionHeaders == null || mSectionHeaders.size() != mHeaderList.size()) {
            mSectionHeaders = new LinkedHashMap<>();
        }

        int sectionIndex = 0;
        for (int i = 0; i < mHeaderList.size(); i++) {
            if (!mHeaderList.get(i).isChild) {
                mSectionHeaders.put(String.valueOf(mHeaderList.get(i).mStartString),
                        new SectionIndices(sectionIndex, i));
                sectionIndex++;
            }
        }
    }

    public void reset() {
        ArrayList<AppInfo> infos = getAllApps();

        mLauncher.mAppDrawer.getLayoutManager().removeAllViews();

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
        ViewGroup.LayoutParams params = holder.mTextView.getLayoutParams();

        // set the margin parameter to account for the text size of the icons so that the text view
        // is based on the icon size only
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
            marginParams.setMargins(marginParams.leftMargin, marginParams.topMargin,
                    marginParams.rightMargin, marginParams.bottomMargin);
            holder.mTextView.setLayoutParams(marginParams);
        }

        for (int i = 0; i < mNumColumns; i++) {
            AppDrawerIconView icon = (AppDrawerIconView) mLayoutInflater.inflate(
                    R.layout.drawer_icon, holder.mLayout, false);
            icon.setOnClickListener(mLauncher);
            icon.setOnLongClickListener(this);
            holder.mLayout.addView(icon);
        }
        return holder;
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        mItemAnimatorSet.add(holder);
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        mItemAnimatorSet.remove(holder);
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

        holder.mTextView.setPivotX(0);
        holder.mTextView.setPivotY(holder.mTextView.getHeight() / 2);

        final int size = indexedInfo.mInfo.size();

        int childSize = holder.mLayout.getChildCount();
        for (int i = 0; i < childSize; i++) {
            AppDrawerIconView icon = (AppDrawerIconView) holder.mLayout.getChildAt(i);
            icon.setLayoutParams(mIconParams);
            int extraStart = mExtraPadding;
            int extraEnd = mExtraPadding;
            // Do not amend the starting and ending padding, only the padding between icons
            if (i == 0) {
                extraStart = 0;
            } else if (i == childSize - 1) {
                extraEnd = 0;
            }

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) icon.getLayoutParams();
            lp.leftMargin = extraStart;
            lp.rightMargin = extraEnd;
            icon.setLayoutParams(lp);

            if (i >= size) {
                icon.setVisibility(View.INVISIBLE);
            } else {
                icon.setVisibility(View.VISIBLE);
                AppInfo info = indexedInfo.mInfo.get(i);
                icon.setTag(info);
                Drawable d = Utilities.createIconDrawable(info.iconBitmap);
                d.setBounds(mIconRect);
                icon.mIcon.setImageDrawable(d);
                icon.mIcon.setPadding(mDeviceProfile.iconDrawablePaddingPx,
                        mDeviceProfile.iconDrawablePaddingPx,
                        mDeviceProfile.iconDrawablePaddingPx,
                        mDeviceProfile.iconDrawablePaddingPx);
                icon.mLabel.setText(info.title);
                icon.mLabel.setVisibility(mHideIconLabels ? View.INVISIBLE : View.VISIBLE);
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
        if (mSectionHeaders.isEmpty()) {
            return 0;
        }
        return mSectionHeaders.get(getSections()[sectionIndex]).mItemIndex;
    }

    @Override
    public int getSectionForPosition(int position) {
        if (mSectionHeaders == null) {
            return 0;
        }

        position = (position < 0) ? 0 : position;
        position = (position > mHeaderList.size()) ? mHeaderList.size() : position;

        int index = 0;
        AppItemIndexedInfo info = mHeaderList.get(position);
        if (info != null) {
            SectionIndices indices = mSectionHeaders.get(info.mStartString);
            if (indices != null) {
                index = indices.mSectionIndex;
            } else {
                Log.w(TAG, "SectionIndices are null");
            }
        } else {
            Log.w(TAG, "AppItemIndexedInfo is null");
        }
        return index;
    }

    private void filterProtectedApps(ArrayList<AppInfo> list) {
        updateProtectedAppsList(mLauncher);

        Iterator<AppInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            AppInfo appInfo = iterator.next();
            if (mProtectedApps.contains(appInfo.componentName)) {
                iterator.remove();
            }
        }
    }

    private void updateProtectedAppsList(Context context) {
        String protectedComponents = Settings.Secure.getString(context.getContentResolver(),
                LauncherModel.SETTINGS_PROTECTED_COMPONENTS);
        protectedComponents = protectedComponents == null ? "" : protectedComponents;
        String [] flattened = protectedComponents.split("\\|");
        mProtectedApps = new ArrayList<ComponentName>(flattened.length);
        for (String flat : flattened) {
            ComponentName cmp = ComponentName.unflattenFromString(flat);
            if (cmp != null) {
                mProtectedApps.add(cmp);
            }
        }
    }
}
