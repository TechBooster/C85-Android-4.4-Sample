package com.example.audioeffectsample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class LevelMeterView extends View {
    private int mLevel;
    private int mMaxLevel;
    private int mStrokeWidth;

    private Paint mLevelBackground = new Paint();
    private Paint mLevelForeground = new Paint();
    private float[] pts_background;

    public LevelMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
    }

    public LevelMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LevelMeterView(Context context) {
        this(context, null, 0);
    }

    private void init() {
        int width = getWidth();
        int height = getHeight();

        mStrokeWidth = Math.round(width / 120f);
        mLevelBackground.setColor(Color.argb(255, 102, 51, 0));
        mLevelBackground.setStrokeWidth(mStrokeWidth);

        pts_background = new float[60 * 4];
        for (int i = 0; i < 60; i++) {
            pts_background[i * 4] = mStrokeWidth * 2 * i + 5;
            pts_background[i * 4 + 1] = height;
            pts_background[i * 4 + 2] = mStrokeWidth * 2 * i + 5;
            pts_background[i * 4 + 3] = 0;
        }

        mLevelForeground = new Paint();
        mLevelForeground.setStrokeWidth(mStrokeWidth);
        mLevelForeground.setColor(Color.argb(255, 51, 255, 102));
    }

    public void setPeakRMS(int level) {
        mLevel = level;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pts_background == null) {
            init();
        }
        int width = getWidth();
        int height = getHeight();

        canvas.drawLines(pts_background, mLevelBackground);
        //int drawlevel = Math.round(((mLevel * -1 / 9600f) * 60f));
        int drawlevel = Math.round(((mLevel+9600f)/9600f)*60f);
        for (int i = 0; i < drawlevel; i++) {
            float x1 = mStrokeWidth * 2 * i + 5;
            float y1 = height;
            float x2 = mStrokeWidth * 2 * i + 5;
            float y2 = 0;
            float percent = i / 60f;
            setPercentageColor(percent, mLevelForeground);
            canvas.drawLine(x1, y1, x2, y2, mLevelForeground);
        }
    }

    private void setPercentageColor(float percent, Paint paint) {
        int color1 = Color.GREEN;
        int color2 = Color.RED;
        int red = (int) (Color.red(color2) * percent + Color.red(color1)
                * (1 - percent));
        int green = (int) (Color.green(color2) * percent + Color.green(color1)
                * (1 - percent));
        int blue = (int) (Color.blue(color2) * percent + Color.blue(color1)
                * (1 - percent));
        paint.setColor(Color.rgb(red, green, blue));
    }

}
