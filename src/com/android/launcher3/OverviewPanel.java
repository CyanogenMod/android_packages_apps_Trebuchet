package com.android.launcher3;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class OverviewPanel extends SlidingUpPanelLayout implements Insettable {
    public OverviewPanel(Context context) {
        super(context);
    }

    public OverviewPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverviewPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public void setInsets(Rect insets) {
        LinearLayout layout = (LinearLayout)
                findViewById(R.id.settings_container);
        FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) layout.getLayoutParams();
        lp.bottomMargin = insets.bottom;
        layout.setLayoutParams(lp);
    }
}
