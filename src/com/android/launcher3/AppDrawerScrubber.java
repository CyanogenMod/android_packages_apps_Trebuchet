/*
 * Copyright (C) 2013 The CyanogenMod Project
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
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.IllegalArgumentException;
import java.util.ArrayList;

/**
 * AppDrawerScrubber
 * <pre>
 *     This is the scrubber at the bottom of the app drawer layout for navigating the application
 *     list
 * </pre>
 *
 * @see {@link android.widget.LinearLayout}
 */
public class AppDrawerScrubber extends LinearLayout {
    private AppDrawerListAdapter mAdapter;
    private RecyclerView mListView;
    private TextView mScrubberIndicator;
    private SeekBar mSeekBar;
    private AutoExpandTextView mScrubberText;
    private SectionContainer mSectionContainer;
    private LinearLayoutManager mLayoutManager;
    private ScrubberAnimationState mScrubberAnimationState;
    private Drawable mTransparentDrawable;
    private AppDrawerSmoothScroller mLinearSmoothScroller;

    private static final int MSG_SET_TARGET = 1000;
    private static final int MSG_SMOOTH_SCROLL = MSG_SET_TARGET + 1;
    private static final int MSG_ANIMATE_PICK = MSG_SMOOTH_SCROLL + 1;

    /**
     * UiHandler
     * <pre>
     *     Using a handler for sending signals to perform certain actions.  The reason for
     *     using this is to be able to remove and replace a signal if signals are being
     *     sent too fast (e.g. user scrubbing like crazy). This allows the touch loop to
     *     complete then later run the animations in their own loops.
     * </pre>
     */
    private class UiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_TARGET:
                    int adapterIndex = msg.arg1;
                    performSetTarget(adapterIndex);
                    break;
                case MSG_ANIMATE_PICK:
                    int index = msg.arg1;
                    int width = msg.arg2;
                    int lastIndex = (Integer)msg.obj;
                    performAnimatePickMessage(index, width, lastIndex);
                    break;
                case MSG_SMOOTH_SCROLL:
                    int itemDiff = msg.arg1;
                    int itemIndex = msg.arg2;
                    performSmoothScroll(itemDiff, itemIndex);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        /**
         * Overidden to remove identical calls if they are called subsequently fast enough.
         *
         * This is the final point that is public in the call chain.  Other calls to sendMessageXXX
         * will eventually call this function which calls "enqueueMessage" which is private.
         *
         * @param msg {@link android.os.Message}
         * @param uptimeMillis {@link java.lang.Long}
         *
         * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
         */
        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) throws
                IllegalArgumentException {
            if (msg == null) {
                throw new IllegalArgumentException("'msg' cannot be null!");
            }
            if (hasMessages(msg.what)) {
                removeMessages(msg.what);
            }
            return super.sendMessageAtTime(msg, uptimeMillis);
        }

    }
    private Handler mUiHandler = new UiHandler();
    private void sendSetTargetMessage(int adapterIndex) {
        Message msg = mUiHandler.obtainMessage(MSG_SET_TARGET);
        msg.what = MSG_SET_TARGET;
        msg.arg1 = adapterIndex;
        mUiHandler.sendMessage(msg);
    }
    private void performSetTarget(int adapterIndex) {
        if (mAdapter != null) {
            mAdapter.setSectionTarget(adapterIndex);
        }
    }
    private void sendAnimatePickMessage(int index, int width, int lastIndex) {
        Message msg = mUiHandler.obtainMessage(MSG_ANIMATE_PICK);
        msg.what = MSG_ANIMATE_PICK;
        msg.arg1 = index;
        msg.arg2 = width;
        msg.obj = lastIndex;
        mUiHandler.sendMessage(msg);
    }
    private void performAnimatePickMessage(int index, int width, int lastIndex) {
        if (mScrubberIndicator != null) {
            // get the index based on the direction the user is scrolling
            int directionalIndex = mSectionContainer.getDirectionalIndex(lastIndex, index);
            String sectionText = mSectionContainer.getHeader(directionalIndex);
            float translateX = (index * width) / (float) mSectionContainer.size();
            // if we are showing letters, grab the position based on the text view
            if (mSectionContainer.showLetters()) {
                translateX = mScrubberText.getPositionOfSection(index);
            }
            // center the x position
            translateX -= mScrubberIndicator.getMeasuredWidth() / 2;
            mScrubberIndicator.setTranslationX(translateX);
            mScrubberIndicator.setText(sectionText);
        }
    }
    private void sendSmoothScrollMessage(int itemDiff, int itemIndex) {
        Message msg = mUiHandler.obtainMessage(MSG_SMOOTH_SCROLL);
        msg.what = MSG_SMOOTH_SCROLL;
        msg.arg1 = itemDiff;
        msg.arg2 = itemIndex;
        mUiHandler.sendMessage(msg);
    }
    private void performSmoothScroll(int itemDiff, int itemIndex) {
        if (mLinearSmoothScroller == null) {
            mLinearSmoothScroller = new AppDrawerSmoothScroller(mContext);
        }
        mLinearSmoothScroller.setItemDiff(itemDiff);
        mLinearSmoothScroller.setTargetPosition(itemIndex);
        mLayoutManager.startSmoothScroll(mLinearSmoothScroller);
    }

    /**
     * Constructor
     *
     * @param context {@link android.content.Context}
     * @param attrs {@link android.util.AttributeSet}
     */
    public AppDrawerScrubber(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor
     *
     * @param context {@link android.content.Context}
     */
    public AppDrawerScrubber(Context context) {
        super(context);
        init(context);
    }

    /**
     * AppDrawerSmoothScroller
     * <pre>
     *     This is a smooth scroller with the ability to set an item diff
     * </pre>
     *
     * @see {@link android.support.v7.widget.LinearSmoothScroller}
     */
    private class AppDrawerSmoothScroller extends LinearSmoothScroller {

        // Members
        private int mItemDiff = 0;

        public AppDrawerSmoothScroller(Context context) {
            super(context);
        }

        @Override
        protected int getVerticalSnapPreference() {
            // position the item against the end of the list view
            return SNAP_TO_END;
        }

        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return mLayoutManager.computeScrollVectorForPosition(targetPosition);
        }

        @Override
        public int calculateDyToMakeVisible(View view, int snapPreference) {
            int dy = super.calculateDyToMakeVisible(view, snapPreference);
            return dy - mItemDiff;
        }

        /**
         * Set the item difference
         *
         * @param itemDiff
         */
        public void setItemDiff(int itemDiff) {
            mItemDiff = itemDiff;
        }

        /**
         * Get the item difference
         *
         * @return {@link java.lang.Integer}
         */
        public int getItemDiff() {
            return mItemDiff;
        }

    }

    /**
     * Simple container class that tries to abstract out the knowledge of complex sections vs
     * simple string sections
     */
    private static class SectionContainer {
        private ArrayList<AppDrawerScrubberSections> mSections;
        private String[] mHeaders;

        public SectionContainer(String[] headers) {
            mSections = AppDrawerScrubberSections.createSections(headers);
            mHeaders = headers;
        }

        public int size() {
            return showLetters() ? mSections.size() : mHeaders.length;
        }

        public String getHeader(int idx) {
            if (size() == 0) {
                return null;
            }
            return showLetters() ? mSections.get(idx).getText() : mHeaders[idx];
        }

        /**
         * Because the list section headers is not necessarily the same size as the scrubber
         * letters, we need to map from the larger list to the smaller list.
         * In the case that curIdx is not highlighted, it will use the directional index to
         * determine the adapter index
         * @return the mHeaders index (aka the underlying adapter index).
         */
        public int getAdapterIndex(int prevIdx, int curIdx) {
            if (!showLetters() || size() == 0) {
                return curIdx;
            }

            // because we have some unhighlighted letters, we need to first get the directional
            // index before getting the adapter index
            return mSections.get(getDirectionalIndex(prevIdx, curIdx)).getAdapterIndex();
        }

        /**
         * Given the direction the user is scrolling in, return the closest index which is a
         * highlighted index
         */
        public int getDirectionalIndex(int prevIdx, int curIdx) {
            if (!showLetters() || size() == 0 || mSections.get(curIdx).getHighlight()) {
                return curIdx;
            }

            if (prevIdx < curIdx) {
                return mSections.get(curIdx).getNextIndex();
            } else {
                return mSections.get(curIdx).getPreviousIndex();
            }
        }

        /**
         * @return true if the scrubber is showing characters as opposed to a line
         */
        public boolean showLetters() {
            return mSections != null;
        }

        /**
         * Initializes the scrubber text with the proper characters
         */
        public void initializeScrubberText(AutoExpandTextView scrubberText) {
            scrubberText.setSections(AppDrawerScrubberSections.getHighlightText(mSections));
        }
    }

    public void updateSections() {
        mSectionContainer = new SectionContainer((String[]) mAdapter.getSections());
        mSectionContainer.initializeScrubberText(mScrubberText);
        mSeekBar.setMax(mSectionContainer.size() - 1);

        // show a white line if there are no letters, otherwise show transparent
        Drawable d = mSectionContainer.showLetters() ? mTransparentDrawable
            : getContext().getResources().getDrawable(R.drawable.seek_back);
        ((ViewGroup)mSeekBar.getParent()).setBackground(d);

    }

    public void setSource(RecyclerView listView) {
        mListView = listView;
        mAdapter = (AppDrawerListAdapter) listView.getAdapter();
        mLayoutManager = (LinearLayoutManager) listView.getLayoutManager();
    }

    public void setScrubberIndicator(TextView scrubberIndicator) {
        mScrubberIndicator = scrubberIndicator;
    }

    private boolean isReady() {
        return mListView != null &&
                mAdapter != null &&
                mSectionContainer != null;
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.scrub_layout, this);
        mTransparentDrawable = new ColorDrawable(Color.TRANSPARENT);
        mScrubberAnimationState = new ScrubberAnimationState();
        mSeekBar = (SeekBar) findViewById(R.id.scrubber);
        mScrubberText = (AutoExpandTextView) findViewById(R.id.scrubberText);
        mSeekBar.setOnSeekBarChangeListener(mScrubberAnimationState);
    }

    /**
     * Handles the animations of the scrubber indicator
     */
    private class ScrubberAnimationState implements SeekBar.OnSeekBarChangeListener {
        private static final long SCRUBBER_DISPLAY_DURATION_IN = 60;
        private static final long SCRUBBER_DISPLAY_DURATION_OUT = 150;
        private static final long SCRUBBER_DISPLAY_DELAY_IN = 0;
        private static final long SCRUBBER_DISPLAY_DELAY_OUT = 200;
        private static final float SCRUBBER_SCALE_START = 0f;
        private static final float SCRUBBER_SCALE_END = 1f;
        private static final float SCRUBBER_ALPHA_START = 0f;
        private static final float SCRUBBER_ALPHA_END = 1f;

        private boolean mTouchingTrack = false;
        private boolean mAnimatingIn = false;
        private int mLastIndex = -1;

        private void touchTrack(boolean touching) {
            mTouchingTrack = touching;

            if (mScrubberIndicator != null) {
                if (mTouchingTrack) {
                    animateIn();
                } else if (!mAnimatingIn) { // finish animating in before animating out
                    animateOut();
                }

                mAdapter.setDragging(mTouchingTrack);
            }
        }

        private void animateIn() {
            if (mScrubberIndicator == null) {
                return;
            }
            // start from a scratch position when animating in
            mScrubberIndicator.animate().cancel();
            mScrubberIndicator.setPivotX(mScrubberIndicator.getMeasuredWidth() / 2);
            mScrubberIndicator.setPivotY(mScrubberIndicator.getMeasuredHeight() * 0.9f);
            mScrubberIndicator.setAlpha(SCRUBBER_ALPHA_START);
            mScrubberIndicator.setScaleX(SCRUBBER_SCALE_START);
            mScrubberIndicator.setScaleY(SCRUBBER_SCALE_START);
            mScrubberIndicator.setVisibility(View.VISIBLE);
            mAnimatingIn = true;

            mScrubberIndicator.animate()
                .alpha(SCRUBBER_ALPHA_END)
                .scaleX(SCRUBBER_SCALE_END)
                .scaleY(SCRUBBER_SCALE_END)
                .setStartDelay(SCRUBBER_DISPLAY_DELAY_IN)
                .setDuration(SCRUBBER_DISPLAY_DURATION_IN)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimatingIn = false;
                        // if the user has stopped touching the seekbar, animate back out
                        if (!mTouchingTrack) {
                            animateOut();
                        }
                    }
                }).start();
        }

        private void animateOut() {
            if (mScrubberIndicator == null) {
                return;
            }
            mScrubberIndicator.animate()
                .alpha(SCRUBBER_ALPHA_START)
                .scaleX(SCRUBBER_SCALE_START)
                .scaleY(SCRUBBER_SCALE_START)
                .setStartDelay(SCRUBBER_DISPLAY_DELAY_OUT)
                .setDuration(SCRUBBER_DISPLAY_DURATION_OUT)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mScrubberIndicator.setVisibility(View.INVISIBLE);
                    }
                });
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int index, boolean fromUser) {
            if (!isReady()) {
                return;
            }
            progressChanged(seekBar, index, fromUser);
        }

        private void progressChanged(SeekBar seekBar, int index, boolean fromUser) {

            sendAnimatePickMessage(index, seekBar.getWidth(), mLastIndex);

            // get the index of the underlying list
            int adapterIndex = mSectionContainer.getAdapterIndex(mLastIndex, index);
            int itemIndex = mAdapter.getPositionForSection(adapterIndex);

            // get any child's height since all children are the same height
            int itemHeight = 0;
            View child = mLayoutManager.getChildAt(0);
            if (child != null) {
                itemHeight = child.getMeasuredHeight();
            }

            // Start smooth scroll from this Looper loop
            if (itemHeight != 0) {
                // scroll to the item such that there are 2 rows beneath it from the bottom
                final int itemDiff = 2 * itemHeight;
                sendSmoothScrollMessage(itemDiff, itemIndex);
            }

            // Post set target index on queue to get processed by Looper later
            sendSetTargetMessage(adapterIndex);

            mLastIndex = index;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            touchTrack(true);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            touchTrack(false);
        }
    }
}
