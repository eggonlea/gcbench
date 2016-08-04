package com.example.lli5.gcbench;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.text.TextPaint;

/**
 * Created by lli5 on 8/27/14.
 */
public class StatDraw {
    private int mFrames = 0;
    private int mDrops = 0;
    private long mTime = 0;
    private long mDrawTime = 0;
    private long mMaxDelta = 0;
    private long mMaxDraw = 0;

    private int mX = 0;
    private int mWidth = 0;
    private int mHeight = 0;
    private float mLines[];
    private Paint mCurPaint;
    private Paint mOldPaint;
    private Paint mCordPaint;
    private TextPaint mTextPaint;
    private String mName;

    public StatDraw(String name, int curColor, int nextColor, int cordColor) {
        mName = name;
        mCurPaint = new Paint();
        mCurPaint.setColor(curColor);
        mOldPaint = new Paint();
        mOldPaint.setColor(nextColor);
        mCordPaint = new Paint();
        mCordPaint.setColor(cordColor);
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setColor(curColor);
        mTextPaint.setTextSize(48);
        mTime = System.currentTimeMillis();
    }

    public void resize(int width, int height) {
        mX = 0;
        mWidth = width;
        mHeight = height;
        mLines = new float[width * 4];
    }

    public void draw(Canvas canvas) {
        if(mWidth <= 0 || mHeight <= 0) {
            return;
        }

        long curTime = SystemClock.uptimeMillis();
        long delta = curTime - mTime;
        if(delta > 33) {
            mDrops++;
        }
        if(delta > mMaxDelta) {
            mMaxDelta = delta;
        }
        mTime = curTime;
        mFrames ++;

        mLines[mX * 4] = mX;
        mLines[mX * 4 + 1] = mHeight - delta;
        mLines[mX * 4 + 2] = mX;
        mLines[mX * 4 + 3] = mHeight;
        mX ++;
        if(mX >= mWidth) {
            mX = 0;
            int color = mCurPaint.getColor();
            mCurPaint.setColor(mOldPaint.getColor());
            mOldPaint.setColor(color);
            mMaxDelta = mMaxDraw = 0;
        }

        int d = 48;
        int x = 0;
        int y = 1;
        try {
            canvas.drawText("View: " + mName, x, (y++) * d, mTextPaint);
            canvas.drawText("Frames: " + mFrames, x, (y++) * d, mTextPaint);
            canvas.drawText("Drops: " + mDrops, x, (y++) * d, mTextPaint);
            canvas.drawText("Delta: " + delta + " / " + mMaxDelta, x, (y++) * d, mTextPaint);
            canvas.drawText("Draw: " + mDrawTime + " / " + mMaxDraw, x, (y++) * d, mTextPaint);
            canvas.drawLine(x, mHeight - 16.67f, mWidth, mHeight - 16.67f, mCordPaint);
            canvas.drawLine(x, mHeight - 33.33f, mWidth, mHeight - 33.33f, mCordPaint);
            canvas.drawLine(x, mHeight, mWidth, mHeight, mCordPaint);
            canvas.drawLines(mLines, 0, mX * 4, mCurPaint);
            canvas.drawLines(mLines, mX * 4, (mWidth - mX) * 4, mOldPaint);
        } catch (OutOfMemoryError e) {
            // do nothing
        }

        mDrawTime = SystemClock.uptimeMillis() - curTime;
        if(mDrawTime > mMaxDraw) {
            mMaxDraw = mDrawTime;
        }
    }
}
