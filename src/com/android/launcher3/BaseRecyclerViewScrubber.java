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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * BaseRecyclerViewScrubber
 * <pre>
 *     This is the scrubber at the bottom of a BaseRecyclerView
 * </pre>
 *
 * @see {@link LinearLayout}
 */
public class BaseRecyclerViewScrubber extends LinearLayout {
    private BaseRecyclerView mBaseRecyclerView;
    private TextView mScrubberIndicator;
    private SeekBar mSeekBar;
    private AutoExpandTextView mScrubberText;
    private SectionContainer mSectionContainer;
    private ScrubberAnimationState mScrubberAnimationState;
    private Drawable mTransparentDrawable;
    private boolean mIsRtl;

    private static final int MSG_SET_TARGET = 1000;
    private static final int MSG_ANIMATE_PICK = MSG_SET_TARGET + 1;

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
         * @param msg {@link Message}
         * @param uptimeMillis {@link Long}
         *
         * @throws IllegalArgumentException {@link IllegalArgumentException}
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
        mBaseRecyclerView.scrollToSection(mSectionContainer.getSectionName(adapterIndex, mIsRtl));
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
            String sectionText = mSectionContainer.getSectionName(directionalIndex, mIsRtl);
            float translateX = (index * width) / (float) mSectionContainer.size();
            // if we are showing letters, grab the position based on the text view
            if (mSectionContainer.showLetters()) {
                translateX = mScrubberText.getPositionOfSection(index);
            }
            // center the x position
            translateX -= mScrubberIndicator.getMeasuredWidth() / 2;
            if (mIsRtl) {
                translateX = -translateX;
            }
            mScrubberIndicator.setTranslationX(translateX);
            mScrubberIndicator.setText(sectionText);
        }
    }

    /**
     * Constructor
     *
     * @param context {@link Context}
     * @param attrs {@link AttributeSet}
     */
    public BaseRecyclerViewScrubber(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor
     *
     * @param context {@link Context}
     */
    public BaseRecyclerViewScrubber(Context context) {
        super(context);
        init(context);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        mIsRtl = Utilities.isRtl(getResources());
        updateSections();
    }

    /**
     * Simple container class that tries to abstract out the knowledge of complex sections vs
     * simple string sections
     */
    private static class SectionContainer {
        private BaseRecyclerViewScrubberSection.
                RtlIndexArrayList<BaseRecyclerViewScrubberSection> mSections;
        private String[] mSectionNames;
        private final boolean mIsRtl;

        public SectionContainer(String[] sections, boolean isRtl) {
            mIsRtl = isRtl;
            mSections = BaseRecyclerViewScrubberSection.createSections(sections, isRtl);
            mSectionNames = sections;
            if (isRtl) {
                final int N = mSectionNames.length;
                for(int i = 0; i < N / 2; i++) {
                    String temp = mSectionNames[i];
                    mSectionNames[i] = mSectionNames[N - i - 1];
                    mSectionNames[N - i - 1] = temp;
                }
                Collections.reverse(mSections);
            }
        }

        public int size() {
            return showLetters() ? mSections.size() : mSectionNames.length;
        }

        public String getSectionName(int idx, boolean isRtl) {
            if (size() == 0) {
                return null;
            }
            return showLetters() ? mSections.get(idx, isRtl).getText() : mSectionNames[idx];
        }

        /**
         * Because the list section headers is not necessarily the same size as the scrubber
         * letters, we need to map from the larger list to the smaller list.
         * In the case that curIdx is not highlighted, it will use the directional index to
         * determine the adapter index
         * @return the mSectionNames index (aka the underlying adapter index).
         */
        public int getAdapterIndex(int prevIdx, int curIdx) {
            if (!showLetters() || size() == 0) {
                return curIdx;
            }

            // because we have some unhighlighted letters, we need to first get the directional
            // index before getting the adapter index
            return mSections.get(getDirectionalIndex(prevIdx, curIdx), mIsRtl).getAdapterIndex();
        }

        /**
         * Given the direction the user is scrolling in, return the closest index which is a
         * highlighted index
         */
        public int getDirectionalIndex(int prevIdx, int curIdx) {
            if (!showLetters() || size() == 0 || mSections.get(curIdx, mIsRtl).getHighlight()) {
                return curIdx;
            }

            if (prevIdx < curIdx) {
                if (mIsRtl) {
                    return mSections.get(curIdx).getPreviousIndex();
                } else {
                    return mSections.get(curIdx).getNextIndex();
                }
            } else {
                if (mIsRtl) {
                    return mSections.get(curIdx).getNextIndex();
                } else {
                    return mSections.get(curIdx).getPreviousIndex();
                }

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
            scrubberText.setSections(BaseRecyclerViewScrubberSection.getHighlightText(mSections));
        }
    }

    public void updateSections() {
        if (mBaseRecyclerView != null) {
            mSectionContainer = new SectionContainer(mBaseRecyclerView.getSectionNames(), mIsRtl);
            mSectionContainer.initializeScrubberText(mScrubberText);
            mSeekBar.setMax(mSectionContainer.size() - 1);

            // show a white line if there are no letters, otherwise show transparent
            Drawable d = mSectionContainer.showLetters() ? mTransparentDrawable
                    : getContext().getResources().getDrawable(R.drawable.seek_back);
            ((ViewGroup) mSeekBar.getParent()).setBackground(d);
        }
    }

    public void setRecycler(BaseRecyclerView baseRecyclerView) {
        mBaseRecyclerView = baseRecyclerView;
    }

    public void setScrubberIndicator(TextView scrubberIndicator) {
        mScrubberIndicator = scrubberIndicator;
    }

    private boolean isReady() {
        return mBaseRecyclerView != null &&
                mSectionContainer != null;
    }

    private void init(Context context) {
        mIsRtl = Utilities.isRtl(context.getResources());
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

                mBaseRecyclerView.setFastScrollDragging(mTouchingTrack);
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
            int adapterIndex = mSectionContainer.getDirectionalIndex(mLastIndex, index);
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
