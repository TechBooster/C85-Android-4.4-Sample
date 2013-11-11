package com.example.audioeffectsample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class LevelMeterLabelView extends View {
    private int mLevel;
    private int mStrokeWidth;

    private Paint mLabelPaint = null;

    public LevelMeterLabelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
    }

    public LevelMeterLabelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LevelMeterLabelView(Context context) {
        this(context, null, 0);
    }

    private void init() {
        int width = getWidth();
        int height = getHeight();

        mStrokeWidth = Math.round(width / 120f);

        mLabelPaint = new Paint();
        mLabelPaint.setColor(Color.argb(255, 255, 255, 255));
        mLabelPaint.setTextSize(18);

    }

    public void setPeakRMS(int level) {
        mLevel = level;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLabelPaint == null) {
            init();
        }
        int width = getWidth();
        int height = getHeight();
        for (int i = 0; i < 60; i++) {
            if (i % 5 == 0) {
                float x = mStrokeWidth * 2 * i;
                float y = height;
                int level = Math.round((9600f - 9600f / 60f * i) / 100f);
                String text = Integer.toString(level);
                canvas.drawText(text, x, y, mLabelPaint);
            }
        }

    }
    
}
