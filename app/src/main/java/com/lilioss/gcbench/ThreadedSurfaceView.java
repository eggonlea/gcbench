package com.lilioss.gcbench;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * TODO: document your custom view class.
 */
public class ThreadedSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private DrawThread drawThread;

    public class DrawThread extends Thread {
        public boolean mRunning;
        public StatDraw mDraw;
        public SurfaceHolder mHolder;

        public DrawThread(SurfaceHolder holder) {
            mHolder = holder;
            mDraw = new StatDraw("TSView", Color.CYAN, Color.GREEN, Color.GRAY);
        }

        @Override
        public void run() {
            super.run();
            mRunning = true;
            while(mRunning) {
                Canvas canvas = mHolder.lockCanvas();
                if(canvas == null)
                    break;
                canvas.drawColor(Color.BLACK);
                mDraw.draw(canvas);
                mHolder.unlockCanvasAndPost(canvas);
                try {
                    sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ThreadedSurfaceView(Context context) {
        super(context);
        init(null, 0);
    }

    public ThreadedSurfaceView(Context context, AttributeSet attrs) {
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
        drawThread.mDraw.resize(width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawThread = new DrawThread(holder);
        drawThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawThread.mRunning = false;
        boolean retry = true;
        while(retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}
