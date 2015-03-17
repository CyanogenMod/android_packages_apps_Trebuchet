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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class AppDrawerScrubber extends LinearLayout {

    private final int SCRUBBER_INDICATOR_DISPLAY_DURATION = 200;
    private final float SCRUBBER_INDICATOR_DISPLAY_TRANSLATIONY = 20f;

    private AppDrawerListAdapter mAdapter;
    private RecyclerView mListView;
    private TextView mScrubberIndicator;
    private SeekBar mSeekBar;
    private String[] mSections;
    private LinearLayoutManager mLayoutManager;

    public AppDrawerScrubber(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AppDrawerScrubber(Context context) {
        super(context);
        init(context);
    }

    public void updateSections() {
        mSections = (String[]) mAdapter.getSections();
        mSeekBar.setMax(mSections.length - 1);
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
                mSections != null;
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.scrub_layout, this);
        mSeekBar = (SeekBar) findViewById(R.id.scrubber);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if (!isReady()) {
                    return;
                }
                resetScrubber();

                String section = String.valueOf(mSections[progress]);

                if (mScrubberIndicator != null) {
                    float translateX = (progress * seekBar.getWidth()) / mSections.length;
                    translateX -= (mScrubberIndicator.getWidth() / 6); // offset for alignment
                    mScrubberIndicator.setTranslationX(translateX);
                    mScrubberIndicator.setText(section);
                }

                mLayoutManager.smoothScrollToPosition(mListView, null,
                        mAdapter.getPositionForSection(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                resetScrubber();
                if (mScrubberIndicator != null) {
                    mScrubberIndicator.setAlpha(1f);
                    mScrubberIndicator.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                resetScrubber();
                if (mScrubberIndicator != null) {
                    mScrubberIndicator.animate().alpha(0f).translationYBy(20f)
                            .setDuration(200).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mScrubberIndicator.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }

            private void resetScrubber() {
                if (mScrubberIndicator != null) {
                    mScrubberIndicator.animate().cancel();
                    mScrubberIndicator.setTranslationY(0f);
                }
            }
        });
    }
}