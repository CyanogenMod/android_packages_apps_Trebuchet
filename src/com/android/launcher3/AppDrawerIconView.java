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

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * AppDrawerIconView - represents icons in the vertical app drawer.
 * Found to be more performant than the BubbleTextView used in the
 * legacy app drawer.
 */
public class AppDrawerIconView extends LinearLayout {

    TextView mLabel;
    ImageView mIcon;

    public AppDrawerIconView(Context context) {
        super(context);
    }

    public AppDrawerIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppDrawerIconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLabel = (TextView) findViewById(R.id.label);
        mIcon = (ImageView) findViewById(R.id.image);
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        mLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, grid.iconTextSizePx);
        mLabel.setShadowLayer(BubbleTextView.SHADOW_LARGE_RADIUS, 0.0f,
                BubbleTextView.SHADOW_Y_OFFSET, BubbleTextView.SHADOW_LARGE_COLOUR);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setAlpha(PagedViewIcon.PRESS_ALPHA);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setAlpha(1f);
                break;
        }
        return super.onTouchEvent(event);
    }
}