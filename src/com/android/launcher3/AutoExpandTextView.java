/*
 * Copyright (C) 2014 Grantland Chew
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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A single-line TextView that resizes it's letter spacing to fit the width of the view
 *
 * @author Grantland Chew <grantlandchew@gmail.com>
 * @author Linus Lee <llee@cyngn.com>
 */
public class AutoExpandTextView extends TextView {
    // How precise we want to be when reaching the target textWidth size
    private static final float PRECISION = 0.01f;

    // Attributes
    private float mPrecision;
    private TextPaint mPaint;
    private float[] mPositions;

    public static class HighlightedText {
        public String mText;
        public boolean mHighlight;

        public HighlightedText(String text, boolean highlight) {
            mText = text;
            mHighlight = highlight;
        }
    }

    public AutoExpandTextView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public AutoExpandTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public AutoExpandTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        float precision = PRECISION;

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.AutofitTextView,
                    defStyle,
                    0);
            precision = ta.getFloat(R.styleable.AutofitTextView_precision, precision);
        }

        mPaint = new TextPaint();
        setPrecision(precision);
    }

    /**
     * @return the amount of precision used to calculate the correct text size to fit within it's
     * bounds.
     */
    public float getPrecision() {
        return mPrecision;
    }

    /**
     * Set the amount of precision used to calculate the correct text size to fit within it's
     * bounds. Lower precision is more precise and takes more time.
     *
     * @param precision The amount of precision.
     */
    public void setPrecision(float precision) {
        if (precision != mPrecision) {
            mPrecision = precision;
            refitText();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLines(int lines) {
        super.setLines(1);
        refitText();
    }

    /**
     * Only allow max lines of 1
     */
    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(1);
        refitText();
    }

    /**
     * Re size the font so the specified text fits in the text box assuming the text box is the
     * specified width.
     */
    private void refitText() {
        CharSequence text = getText();

        if (TextUtils.isEmpty(text)) {
            return;
        }

        TransformationMethod method = getTransformationMethod();
        if (method != null) {
            text = method.getTransformation(text, this);
        }
        int targetWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (targetWidth > 0) {
            float high = 100;
            float low = 0;

            mPaint.set(getPaint());
            mPaint.setTextSize(getTextSize());
            float letterSpacing = getLetterSpacing(text, mPaint, targetWidth, low, high,
                    mPrecision);
            mPaint.setLetterSpacing(letterSpacing);
            calculateSections(text);

            super.setLetterSpacing(letterSpacing);
        }
    }

    public float getPositionOfSection(int position) {
        if (mPositions == null || position >= mPositions.length) {
            return 0;
        }
        return mPositions[position];
    }

    /**
     * This calculates the different horizontal positions of each character
     */
    private void calculateSections(CharSequence text) {
        mPositions = new float[text.length()];
        for (int i = 0; i < text.length(); i++) {
            if (i == 0) {
                mPositions[0] = mPaint.measureText(text, 0, 1) / 2;
            } else {
                // try to be lazy and just add the width of the newly added char
                mPositions[i] = mPaint.measureText(text, i, i + 1) + mPositions[i - 1];
            }
        }
    }

    /**
     * Sets the list of sections in the text view.  This will take the first character of each
     * and space it out in the text view using letter spacing
     */
    public void setSections(ArrayList<HighlightedText> sections) {
        mPositions = null;
        if (sections == null || sections.size() == 0) {
            setText("");
            return;
        }

        Resources r = getContext().getResources();
        int highlightColor = r.getColor(R.color.app_scrubber_highlight_color);
        int grayColor = r.getColor(R.color.app_scrubber_gray_color);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (HighlightedText highlightText : sections) {
            SpannableString spannable = new SpannableString(highlightText.mText.substring(0, 1));
            spannable.setSpan(
                    new ForegroundColorSpan(highlightText.mHighlight ? highlightColor : grayColor),
                    0, spannable.length(), 0);
            builder.append(spannable);
        }

        setText(builder);
    }

    private static float getLetterSpacing(CharSequence text, TextPaint paint, float targetWidth,
                                          float low, float high, float precision) {
        float mid = (low + high) / 2.0f;
        paint.setLetterSpacing(mid);

        float measuredWidth = paint.measureText(text, 0, text.length());

        if (high - low < precision) {
            if (measuredWidth < targetWidth) {
                return mid;
            } else {
                return low;
            }
        } else if (measuredWidth > targetWidth) {
            return getLetterSpacing(text, paint, targetWidth, low, mid, precision);
        } else if (measuredWidth < targetWidth) {
            return getLetterSpacing(text, paint, targetWidth, mid, high, precision);
        } else {
            return mid;
        }
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start,
                                 final int lengthBefore, final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        refitText();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            refitText();
        }
    }
}