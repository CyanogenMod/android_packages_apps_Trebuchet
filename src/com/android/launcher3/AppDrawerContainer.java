package com.android.launcher3;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class AppDrawerContainer extends InsettableFrameLayout {
    public AppDrawerContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setInsets(Rect insets) {
        // List view
        View view = findViewById(R.id.app_drawer_recyclerview);
        FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) view.getLayoutParams();
        int paddingBottom = view.getPaddingBottom() + insets.bottom - mInsets.bottom;
        int paddingTop = view.getPaddingTop() + insets.top - mInsets.top;
        view.setLayoutParams(lp);
        view.setPadding(view.getPaddingLeft(), paddingTop, view.getPaddingRight(), paddingBottom);

        // Scrubber
        view = findViewById(R.id.app_drawer_scrubber_container);
        LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) view.getLayoutParams();
        llp.bottomMargin += insets.bottom - mInsets.bottom;
        view.setLayoutParams(llp);
    }
}
