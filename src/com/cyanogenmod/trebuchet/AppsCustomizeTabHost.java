/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.cyanogenmod.trebuchet.preference.PreferencesProvider;

import java.util.ArrayList;

public class AppsCustomizeTabHost extends TabHost implements LauncherTransitionable,
        TabHost.OnTabChangeListener {
    static final String LOG_TAG = "AppsCustomizeTabHost";

    private static final String APPS_TAB_TAG = "APPS";
    private static final String WIDGETS_TAB_TAG = "WIDGETS";

    private final LayoutInflater mLayoutInflater;
    private ViewGroup mTabs;
    private ViewGroup mTabsContainer;
    private AppsCustomizeView mAppsCustomizePane;
    private boolean mSuppressContentCallback = false;
    private FrameLayout mAnimationBuffer;
    private LinearLayout mContent;

    private Launcher mLauncher;

    private boolean mInTransition;
    private boolean mResetAfterTransition;
    private Animator mLauncherTransition;

    // Preferences
    private boolean mFadeScrollingIndicator;

    public AppsCustomizeTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);

        mLauncher = (Launcher) context;

        // Preferences
        mFadeScrollingIndicator = PreferencesProvider.Interface.Drawer.Indicator.getFadeScrollingIndicator(context);
    }

    /**
     * Convenience methods to select specific tabs.  We want to set the content type immediately
     * in these cases, but we note that we still call setCurrentTabByTag() so that the tab view
     * reflects the new content (but doesn't do the animation and logic associated with changing
     * tabs manually).
     */
    private void setContentTypeImmediate(AppsCustomizeView.ContentType type) {
        mAppsCustomizePane.hideIndicator(false);
        mAppsCustomizePane.setContentType(type);
    }
    void selectAppsTab() {
        setContentTypeImmediate(AppsCustomizeView.ContentType.Apps);
        setCurrentTabByTag(APPS_TAB_TAG);
    }
    void selectWidgetsTab() {
        setContentTypeImmediate(AppsCustomizeView.ContentType.Widgets);
        mAppsCustomizePane.setCurrentToWidgets();

        setCurrentTabByTag(WIDGETS_TAB_TAG);
    }

    /**
     * Setup the tab host and create all necessary tabs.
     */
    @Override
    protected void onFinishInflate() {
        // Setup the tab host
        setup();

        final ViewGroup tabsContainer = (ViewGroup) findViewById(R.id.tabs_container);
        final TabWidget tabs = (TabWidget) findViewById(com.android.internal.R.id.tabs);
        final AppsCustomizeView appsCustomizePane = (AppsCustomizeView)
                findViewById(R.id.apps_customize_pane_content);
        mTabs = tabs;
        mTabsContainer = tabsContainer;
        mAppsCustomizePane = appsCustomizePane;
        mAnimationBuffer = (FrameLayout) findViewById(R.id.animation_buffer);
        mContent = (LinearLayout) findViewById(R.id.apps_customize_content);
        if (tabs == null || mAppsCustomizePane == null) throw new Resources.NotFoundException();

        // Configure the tabs content factory to return the same paged view (that we change the
        // content filter on)
        TabContentFactory contentFactory = new TabContentFactory() {
            public View createTabContent(String tag) {
                return (View) appsCustomizePane;
            }
        };

        // Create the tabs
        TextView tabView;
        String label;
        label = mContext.getString(R.string.all_apps_button_label);
        tabView = (TextView) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        tabView.setText(label);
        tabView.setContentDescription(label);
        tabView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    mLauncher.onLongClickAppsTab(v);
                    return true;
                }
        });
        addTab(newTabSpec(APPS_TAB_TAG).setIndicator(tabView).setContent(contentFactory));
        label = mContext.getString(R.string.widgets_tab_label);
        tabView = (TextView) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        tabView.setText(label);
        tabView.setContentDescription(label);
        addTab(newTabSpec(WIDGETS_TAB_TAG).setIndicator(tabView).setContent(contentFactory));
        setOnTabChangedListener(this);

        // Setup the key listener to jump between the last tab view and the market icon
        AppsCustomizeTabKeyEventListener keyListener = new AppsCustomizeTabKeyEventListener();
        View lastTab = tabs.getChildTabViewAt(tabs.getTabCount() - 1);
        lastTab.setOnKeyListener(keyListener);
        View shopButton = findViewById(R.id.market_button);
        shopButton.setOnKeyListener(keyListener);

        // Soft menu button
        View overflowMenuButton = findViewById(R.id.overflow_menu_button);
        overflowMenuButton.setOnKeyListener(keyListener);

        // Hide the tab bar until we measure
        mTabsContainer.setAlpha(0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean remeasureTabWidth = (mTabs.getLayoutParams().width <= 0);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Set the width of the tab list to the content width
        if (remeasureTabWidth) {
            int contentWidth = ((View) mAppsCustomizePane).getMeasuredWidth();
            if (contentWidth > 0 && mTabs.getLayoutParams().width != contentWidth) {
                // Set the width and show the tab bar
                mTabs.getLayoutParams().width = contentWidth;
                post(new Runnable() {
                    public void run() {
                        mTabs.requestLayout();
                        mTabsContainer.setAlpha(1f);
                    }
                });
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Intercept all touch events up to the bottom of the AppsCustomizePane so they do not fall
        // through to the workspace and trigger showWorkspace()
        if (event.getY() < ((View) mAppsCustomizePane).getBottom()) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onTabChanged(String tabId) {
        final AppsCustomizeView.ContentType type = getContentTypeForTabTag(tabId);
        if (mSuppressContentCallback) {
            mSuppressContentCallback = false;
            return;
        }

        mAppsCustomizePane.onTabChanged(type);

    }

    public void setCurrentTabFromContent(AppsCustomizeView.ContentType type) {
        mSuppressContentCallback = true;
        setCurrentTabByTag(getTabTagForContentType(type));
    }

    /**
     * Returns the content type for the specified tab tag.
     */
    public AppsCustomizeView.ContentType getContentTypeForTabTag(String tag) {
        if (tag.equals(APPS_TAB_TAG)) {
            return AppsCustomizeView.ContentType.Apps;
        } else if (tag.equals(WIDGETS_TAB_TAG)) {
            return AppsCustomizeView.ContentType.Widgets;
        }
        return AppsCustomizeView.ContentType.Apps;
    }

    /**
     * Returns the tab tag for a given content type.
     */
    public String getTabTagForContentType(AppsCustomizeView.ContentType type) {
        if (type == AppsCustomizeView.ContentType.Apps) {
            return APPS_TAB_TAG;
        } else if (type == AppsCustomizeView.ContentType.Widgets) {
            return WIDGETS_TAB_TAG;
        }
        return APPS_TAB_TAG;
    }

    /**
     * Disable focus on anything under this view in the hierarchy if we are not visible.
     */
    @Override
    public int getDescendantFocusability() {
        if (getVisibility() != View.VISIBLE) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        }
        return super.getDescendantFocusability();
    }

    void reset() {
        if (mInTransition) {
            // Defer to after the transition to reset
            mResetAfterTransition = true;
        } else {
            // Reset immediately
            mAppsCustomizePane.reset();
        }
    }

    private void enableAndBuildHardwareLayer() {
        // isHardwareAccelerated() checks if we're attached to a window and if that
        // window is HW accelerated-- we were sometimes not attached to a window
        // and buildLayer was throwing an IllegalStateException
        if (isHardwareAccelerated()) {
            // Turn on hardware layers for performance
            setLayerType(LAYER_TYPE_HARDWARE, null);

            // force building the layer, so you don't get a blip early in an animation
            // when the layer is created layer
            buildLayer();
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mLauncherTransition != null) {
            enableAndBuildHardwareLayer();
            mLauncherTransition.start();
            mLauncherTransition = null;
        }
    }

    /* LauncherTransitionable overrides */
    @Override
    public boolean onLauncherTransitionStart(Launcher l, Animator animation, boolean toWorkspace) {
        mInTransition = true;
        boolean delayLauncherTransitionUntilLayout = false;
        boolean animated = (animation != null);
        mLauncherTransition = null;

        // if the content wasn't visible before, delay the launcher animation until after a call
        // to layout -- this prevents a blip
        if (animated && mContent.getVisibility() == GONE) {
            mLauncherTransition = animation;
            delayLauncherTransitionUntilLayout = true;
        }
        mContent.setVisibility(VISIBLE);

        if (!toWorkspace) {
            // Make sure the current page is loaded (we start loading the side pages after the
            // transition to prevent slowing down the animation)
            mAppsCustomizePane.loadContent(true);
        }
        if (animated && !delayLauncherTransitionUntilLayout) {
            enableAndBuildHardwareLayer();
        }

        if (!toWorkspace && !LauncherApplication.isScreenLarge()) {
            mAppsCustomizePane.showIndicator(false);
        }
        if (mResetAfterTransition) {
            mAppsCustomizePane.reset();
            mResetAfterTransition = false;
        }
        return delayLauncherTransitionUntilLayout;
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, Animator animation, boolean toWorkspace) {
        mInTransition = false;
        if (animation != null) {
            setLayerType(LAYER_TYPE_NONE, null);
        }

        if (!toWorkspace) {
            // Dismiss the workspace cling and show the all apps cling (if not already shown)
            l.dismissWorkspaceCling(null);
            mAppsCustomizePane.showAllAppsCling();
            // Make sure adjacent pages are loaded (we wait until after the transition to
            // prevent slowing down the animation)
            mAppsCustomizePane.loadContent();

            if (!LauncherApplication.isScreenLarge() && mFadeScrollingIndicator) {
                mAppsCustomizePane.hideIndicator(false);
            }
        }
    }

    public void onResume() {
        if (getVisibility() == VISIBLE) {
            mContent.setVisibility(VISIBLE);
            // We unload the widget previews when the UI is hidden, so need to reload pages
            // Load the current page synchronously, and the neighboring pages asynchronously
            mAppsCustomizePane.loadContent(true);
            mAppsCustomizePane.loadContent();
        }
    }

    public void onTrimMemory() {
        mContent.setVisibility(GONE);
        // Clear the widget pages of all their subviews - this will trigger the widget previews
        // to delete their bitmaps
        mAppsCustomizePane.clearAllWidgetPreviews();
    }

    boolean isTransitioning() {
        return mInTransition;
    }
}
