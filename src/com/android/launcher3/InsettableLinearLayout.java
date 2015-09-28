package com.android.launcher3;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class InsettableLinearLayout extends LinearLayout implements
    ViewGroup.OnHierarchyChangeListener, Insettable {

    protected Rect mInsets = new Rect();

    public InsettableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnHierarchyChangeListener(this);
    }

    public void setLinearLayoutChildInsets(View child, Rect newInsets, Rect oldInsets) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        if (child instanceof Insettable) {
            ((Insettable) child).setInsets(newInsets);
        } else if (!lp.ignoreInsets) {
            if (!lp.ignoreTopInsets) {
                lp.topMargin += (newInsets.top - oldInsets.top);
            }
            lp.leftMargin += (newInsets.left - oldInsets.left);
            lp.rightMargin += (newInsets.right - oldInsets.right);
            if (!lp.ignoreBottomInsets) {
                lp.bottomMargin += (newInsets.bottom - oldInsets.bottom);
            }
        }
        child.setLayoutParams(lp);
    }

    @Override
    public void setInsets(Rect insets) {
        final int n = getChildCount();
        for (int i = 0; i < n; i++) {
            final View child = getChildAt(i);
            setLinearLayoutChildInsets(child, insets, mInsets);
        }
        mInsets.set(insets);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new InsettableLinearLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof InsettableLinearLayout.LayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends LinearLayout.LayoutParams {
        boolean ignoreInsets = false;
        boolean ignoreTopInsets = false;
        boolean ignoreBottomInsets = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs,
                    R.styleable.InsettableLinearLayout_Layout);
            ignoreInsets = a.getBoolean(
                    R.styleable.InsettableLinearLayout_Layout_layout_ignoreInsets, false);
            ignoreTopInsets = a.getBoolean(
                    R.styleable.InsettableLinearLayout_Layout_layout_ignoreTopInsets, false);
            ignoreBottomInsets = a.getBoolean(
                    R.styleable.InsettableLinearLayout_Layout_layout_ignoreBottomInsets, false);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams lp) {
            super(lp);
        }
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        setLinearLayoutChildInsets(child, mInsets, new Rect());
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
    }

}
