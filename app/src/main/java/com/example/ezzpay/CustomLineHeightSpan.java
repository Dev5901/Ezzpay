package com.example.ezzpay;

import android.graphics.Paint;
import android.text.Spannable;
import android.text.style.LineHeightSpan;

public class CustomLineHeightSpan implements LineHeightSpan {
    private final int extraLineHeight;

    public CustomLineHeightSpan(int extraLineHeight) {
        this.extraLineHeight = extraLineHeight;
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
        fm.ascent -= extraLineHeight;
        fm.descent += extraLineHeight;
    }
}
