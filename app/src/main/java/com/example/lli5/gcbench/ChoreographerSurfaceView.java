package com.example.lli5.gcbench;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * TODO: document your custom view class.
 */
public class ChoreographerSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private StatDraw statDraw;
    private SurfaceHolder mSurfaceHolder;
    private Choreographer mChoreographer;
    private Choreographer.FrameCallback mVSyncFrameCallback;

    public ChoreographerSurfaceView(Context context) {
        super(context);
        init(null, 0);
    }

    public ChoreographerSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void init(AttributeSet attrs, int defStyle) {
        // Set up a default TextPaint object
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        statDraw.resize(width, height);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        statDraw = new StatDraw("CSView", Color.YELLOW, Color.BLUE, Color.GRAY);
        mSurfaceHolder = holder;
        mChoreographer = Choreographer.getInstance();
        mVSyncFrameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                onVSync(frameTimeNanos);
            }
        };
        mChoreographer.postFrameCallback(mVSyncFrameCallback);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mChoreographer.removeFrameCallback(mVSyncFrameCallback);
        mChoreographer = null;
        mSurfaceHolder = null;
    }

    protected void onVSync(long frameTimeNanos) {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if(canvas == null)
            return;
        canvas.drawColor(Color.DKGRAY);
        statDraw.draw(canvas);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
        mChoreographer.postFrameCallback(mVSyncFrameCallback);
    }
}
