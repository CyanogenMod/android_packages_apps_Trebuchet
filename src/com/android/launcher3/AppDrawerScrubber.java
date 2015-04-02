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

import java.util.ArrayList;

public class AppDrawerScrubber extends LinearLayout {
    private AppDrawerListAdapter mAdapter;
    private RecyclerView mListView;
    private TextView mScrubberIndicator;
    private SeekBar mSeekBar;
    private AutoExpandTextView mScrubberText;
    private SectionContainer mSectionContainer;
    private LinearLayoutManager mLayoutManager;
    private ScrubberAnimationState mScrubberAnimationState;

    public AppDrawerScrubber(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AppDrawerScrubber(Context context) {
        super(context);
        init(context);
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
            if (!showLetters()) {
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
            if (!showLetters() || mSections.get(curIdx).getHighlight()) {
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
        Drawable d = mSectionContainer.showLetters() ? new ColorDrawable(Color.TRANSPARENT)
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
        mScrubberAnimationState = new ScrubberAnimationState();
        mSeekBar = (SeekBar) findViewById(R.id.scrubber);
        mScrubberText = (AutoExpandTextView) findViewById(R.id.scrubberText);
        mSeekBar.setOnSeekBarChangeListener(mScrubberAnimationState);
    }

    /**
     * Handles the animations of the scrubber indicator
     */
    private class ScrubberAnimationState implements SeekBar.OnSeekBarChangeListener {
        private static final long SCRUBBER_DISPLAY_DURATION = 150;
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
            // start from a scratch position when animating in
            mScrubberIndicator.animate().cancel();
            mScrubberIndicator.setPivotX(mScrubberIndicator.getMeasuredWidth() / 2);
            mScrubberIndicator.setPivotY(mScrubberIndicator.getMeasuredHeight() * 0.8f);
            mScrubberIndicator.setAlpha(SCRUBBER_ALPHA_START);
            mScrubberIndicator.setScaleX(SCRUBBER_SCALE_START);
            mScrubberIndicator.setScaleY(SCRUBBER_SCALE_START);
            mScrubberIndicator.setVisibility(View.VISIBLE);
            mAnimatingIn = true;

            mScrubberIndicator.animate()
                .alpha(SCRUBBER_ALPHA_END)
                .scaleX(SCRUBBER_SCALE_END)
                .scaleY(SCRUBBER_SCALE_END)
                .setDuration(SCRUBBER_DISPLAY_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimatingIn = false;
                        // if the user has stopped touching the seekbar, animate back out
                        if (!mTouchingTrack) {
                            animateOut();
                        }
                    }
                })
                .start();
        }

        private void animateOut() {
            mScrubberIndicator.animate()
                .alpha(SCRUBBER_ALPHA_START)
                .scaleX(SCRUBBER_SCALE_START)
                .scaleY(SCRUBBER_SCALE_START)
                .setDuration(SCRUBBER_DISPLAY_DURATION)
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

            if (mScrubberIndicator != null) {
                // get the index based on the direction the user is scrolling
                int directionalIndex = mSectionContainer.getDirectionalIndex(mLastIndex, index);
                String sectionText = mSectionContainer.getHeader(directionalIndex);

                float translateX = (index * seekBar.getWidth()) / (float)mSectionContainer.size();
                // if we are showing letters, grab the position based on the text view
                if (mSectionContainer.showLetters()) {
                    translateX = mScrubberText.getPositionOfSection(index);
                }

                // center the x position
                translateX -= mScrubberIndicator.getMeasuredWidth() / 2;

                mScrubberIndicator.setTranslationX(translateX);
                mScrubberIndicator.setText(sectionText);
            }

            // get the index of the underlying list
            int adapterIndex = mSectionContainer.getAdapterIndex(mLastIndex, index);
            int itemIndex = mAdapter.getPositionForSection(adapterIndex);

            // get any child's height since all children are the same height
            int itemHeight = 0;
            View child = mLayoutManager.getChildAt(0);
            if (child != null) {
                itemHeight = child.getMeasuredHeight();
            }

            if (itemHeight != 0) {
                // scroll to the item such that there are 2 rows beneath it from the bottom
                final int itemDiff = 2 * itemHeight;
                LinearSmoothScroller scroller = new LinearSmoothScroller(mListView.getContext()) {
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
                        return dy - itemDiff;
                    }
                };
                scroller.setTargetPosition(itemIndex);
                mLayoutManager.startSmoothScroll(scroller);
            }

            mAdapter.setSectionTarget(adapterIndex);

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
