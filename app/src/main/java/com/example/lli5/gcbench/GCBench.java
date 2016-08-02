package com.example.lli5.gcbench;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.LinkedList;


public class GCBench extends Activity implements SeekBar.OnSeekBarChangeListener {
    private final String TAG = "GCBench";
    private final int MB = 1024 * 1024;
    private long mCapMB;
    private long mHeapRatio;
    private SeekBar mCapBar;
    private SeekBar mRatioBar;
    private TextView mMemInfoRT;
    private TextView mMemInfoAM;
    private TextView mExplitGC;
    private TextView mStringAlloc;
    private TextView mStringConcat;
    private TextView mBitmapSmall;
    private TextView mBitmapLarge;
    private TextView mNativeMemory;
    private TextView mLogs;
    private int mLogn = 0;
    private Handler mHandler = new Handler();

    private long nFreeRT = -1;
    private long nAvailAM = -1;
    private int nStringAlloc = 0;
    private int nStringConcat = 0;
    private int nBitMapSmall = 0;
    private int nBitmapLarge = 0;
    private int nNativeMemory = 0;

    private final int gcInterval = 100;
    private LinkedList<String> aStringAlloc = new LinkedList<String>();
    private LinkedList<String> aStringConcat = new LinkedList<String>();
    private LinkedList<Bitmap> aBitmapSmall = new LinkedList<Bitmap>();
    private LinkedList<Bitmap> aBitmapLarge = new LinkedList<Bitmap>();
    private LinkedList<Long> aNativeMemory = new LinkedList<Long>();

    private Thread explitGC;
    private Thread stringAlloc;
    private Thread stringConcat;
    private Thread bitmapSmall;
    private Thread bitmapLarge;
    private Thread nativeMemory;

    static {
        System.loadLibrary("jni-lib");
    }

    private native long nativeMalloc(int size);
    private native void nativeFree(long ptr);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gcbench);
        ((RadioButton)findViewById(R.id.maxMemory)).setChecked(true);
        mMemInfoRT = (TextView)findViewById(R.id.memoryInfoRT);
        mMemInfoAM = (TextView)findViewById(R.id.memoryInfoAM);
        mExplitGC = (TextView)findViewById(R.id.explitGCInfo);
        mExplitGC.setText(gcInterval + "ms");
        mStringAlloc = (TextView)findViewById(R.id.stringAllocInfo);
        mStringConcat = (TextView)findViewById(R.id.stringConcatInfo);
        mBitmapSmall = (TextView)findViewById(R.id.bitmapSmallInfo);
        mBitmapLarge = (TextView)findViewById(R.id.bitmapLargeInfo);
        mNativeMemory = (TextView)findViewById(R.id.nativeMemoryInfo);
        mLogs = (TextView)findViewById(R.id.logs);
        mLogs.setMovementMethod(ScrollingMovementMethod.getInstance());

        mCapBar = ((SeekBar)findViewById(R.id.capMemoryMB));
        mCapBar.setMax((int) Runtime.getRuntime().maxMemory() / MB);
        mCapBar.setProgress(0);
        mCapBar.setOnSeekBarChangeListener(this);
        mRatioBar = ((SeekBar)findViewById(R.id.heapRatio));
        mRatioBar.setMax(100);
        mRatioBar.setProgress(75);
        mRatioBar.setOnSeekBarChangeListener(this);
        mCapMB = 0;
        mHeapRatio = 75;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateInfo();
                mHandler.postDelayed(this, 1000);
            }
        }, 1000);

        log("onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy");

        stopThread(explitGC);
        stopThread(stringAlloc);
        stopThread(stringConcat);
        stopThread(bitmapSmall);
        stopThread(bitmapLarge);
        stopThread(nativeMemory);
        mHandler.removeCallbacksAndMessages(null);
        log("Finishing...");
        finish();
    }

    private void updateInfo()
    {
        try {
            long free = Runtime.getRuntime().freeMemory() / MB;
            if(nFreeRT != free) {
                nFreeRT = free;
                long total = Runtime.getRuntime().totalMemory();
                long max = Runtime.getRuntime().maxMemory();
                String s = "RT: Free/Total/Max/Cap(%) = "
                        + free + "/"
                        + total / MB + "/"
                        + max / MB + "/"
                        + mCapMB + "MB ("
                        + mHeapRatio + "%)";
                mMemInfoRT.setText(s);
                log(s);
            }

            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long avail = mi.availMem / MB;
            if(nAvailAM != avail) {
                nAvailAM = avail;
                String s = "AM: Avail/Thres/Total (Low) = "
                        + avail + "/"
                        + mi.threshold / MB + "/"
                        + mi.totalMem / MB + " ("
                        + mi.lowMemory + ")";
                mMemInfoAM.setText(s);
                log(s);
            }

            int alloc = aStringAlloc.size();
            if(nStringAlloc != alloc) {
                nStringAlloc = alloc;
                String s = (alloc / 1000) + "K: " + (alloc * 96 / MB) + "MB";
                mStringAlloc.setText(s);
                log("StringAlloc: " + s);
            }

            int concat = aStringConcat.size();
            if(nStringConcat != concat) {
                nStringConcat = concat;
                String s = (concat / 1000) + "K: " + (concat * 128 / MB) + "MB";
                mStringConcat.setText(s);
                log("StringConcat: " + s);
            }

            int small = aBitmapSmall.size();
            if(nBitMapSmall != small) {
                nBitMapSmall = small;
                String s = (small / 1000) + "K: " + (small * (16 * 16 * 2 + 12) / MB) + "MB";
                mBitmapSmall.setText(s);
                log("BitmapSmall: " + s);
            }

            int large = aBitmapLarge.size();
            if(nBitmapLarge != large) {
                nBitmapLarge = large;
                String s = large + ": " + (large * (1024 * 1024 * 4 + 12) / MB) + "MB";
                mBitmapLarge.setText(s);
                log("BitmapLarge: " + s);
            }

            int malloc = aNativeMemory.size();
            if(nNativeMemory != malloc) {
                nNativeMemory = malloc;
                String s = aNativeMemory.size() + "MB";
                mNativeMemory.setText(s);
                log("NativeMemory: " + s);
            }
        } catch (OutOfMemoryError e) {
            log(e.toString());
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        switch (seekBar.getId()) {
            case R.id.capMemoryMB:
                mCapMB = progress;
                ((RadioButton)findViewById(R.id.capMemory)).setChecked(true);
                break;
            case R.id.heapRatio:
                mHeapRatio = progress;
                break;
        }
        updateInfo();
    }

    public void onStartTrackingTouch(SeekBar seekBar) { /* do nothing */ }
    public void onStopTrackingTouch(SeekBar seekBar) { /* do nothing */ }

    private void stopThread(Thread thread) {
        if (thread != null) {
            log("stopThread: " + thread.getName());
            boolean retry = true;
            while(retry) {
                thread.interrupt();
                try {
                    thread.join();
                    retry = false;
                } catch (Exception e) {
                    // do nothing
                }
            }
            log("stopThread: " + thread.getName() + " done.");
        }
    }

    public void onMemoryCapping(View view) {
        if (((RadioButton)view).isChecked()) {
            switch(view.getId()) {
                case R.id.maxMemory:
                    mCapMB = 0;
                    updateInfo();
                    break;
                case R.id.capMemory:
                    mCapMB = mCapBar.getProgress();
                    updateInfo();
                    break;
            }
        }
    }

    private void checkMemoryCapping()
    {
        if (mCapMB > 0) {
            long size = 0;
            size += aStringAlloc.size() * 96;
            size += aStringConcat.size() * 128;
            size += aBitmapSmall.size() * (16 * 16 * 2 + 12);
            size += aBitmapLarge.size() * (1024 * 1024 * 4 + 12);
            if (size > mCapMB * MB)
                throw new OutOfMemoryError();
        }
    }

    private void trimList(LinkedList List)
    {
        long n = List.size() * (100 - mHeapRatio) / 100;
        while (n-- > 0)
            List.remove();
    }

    public void onExplitGC(View view) {
        if (((CheckBox)view).isChecked()) {
            explitGC = new Thread(new Runnable() {
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        System.gc();
                        SystemClock.sleep(gcInterval);
                    }
                }
            }, getString(R.string.explicit_gc));
            explitGC.start();
            log("startThread " + explitGC.getName());
        } else {
            stopThread(explitGC);
        }
    }

    public void onStringAlloc(View view) {
        if (((CheckBox)view).isChecked()) {
            stringAlloc = new Thread(new Runnable() {
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        SystemClock.sleep(1);
                        try {
                            checkMemoryCapping();
                            aStringAlloc.add(new String("1234567890"));
                        } catch (OutOfMemoryError e) {
                            trimList(aStringAlloc);
                            System.gc();
                        }
                    }
                    aStringAlloc.clear();
                }
            }, getString(R.string.string_alloc));
            stringAlloc.start();
            log("startThread " + stringAlloc.getName());
        } else {
            stopThread(stringAlloc);
        }
    }

    public void onStringConcat(View view) {
        if (((CheckBox)view).isChecked()) {
            stringConcat = new Thread(new Runnable() {
                public void run() {
                    int i = 100;
                    while(!Thread.currentThread().isInterrupted()) {
                        SystemClock.sleep(1);
                        try {
                            checkMemoryCapping();
                            aStringConcat.add("abcdefg" + i + ".");
                            i = i < 999 ? i + 1: 100;
                        } catch (OutOfMemoryError e) {
                            trimList(aStringConcat);
                            System.gc();
                        }
                    }
                    aStringConcat.clear();
                }
            }, getString(R.string.string_concat));
            stringConcat.start();
            log("startThread " + stringConcat.getName());
        } else {
            stopThread(stringConcat);
        }
    }

    public void onSmallBitmap(View view) {
        if (((CheckBox)view).isChecked()) {
            bitmapSmall = new Thread(new Runnable() {
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        SystemClock.sleep(1);
                        try {
                            checkMemoryCapping();
                            aBitmapSmall.add(Bitmap.createBitmap(16, 16, Bitmap.Config.RGB_565));
                        } catch (OutOfMemoryError e) {
                            trimList(aBitmapSmall);
                            System.gc();
                        }
                    }
                    aBitmapSmall.clear();
                }
            }, getString(R.string.small_bitmap));
            bitmapSmall.start();
            log("startThread " + bitmapSmall.getName());
        } else {
            stopThread(bitmapSmall);
        }
    }

    public void onLargeBitmap(View view) {
        if (((CheckBox)view).isChecked()) {
            bitmapLarge = new Thread(new Runnable() {
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        SystemClock.sleep(1000);
                        try {
                            checkMemoryCapping();
                            aBitmapLarge.add(Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888));
                        } catch (OutOfMemoryError e) {
                            //trimList(aBitmapLarge);
                            aBitmapLarge.clear();
                            System.gc();
                        }
                    }
                    aBitmapLarge.clear();
                }
            }, getString(R.string.large_bitmap));
            bitmapLarge.start();
            log("startThread " + bitmapLarge.getName());
        } else {
            stopThread(bitmapLarge);
        }
    }

    public void onNativeMemory(View view) {
        if (((CheckBox)view).isChecked()) {
            nativeMemory = new Thread(new Runnable() {
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        SystemClock.sleep(10);
                        checkMemoryCapping();
                        long ptr = nativeMalloc(MB);
                        if (ptr > 0) {
                            aNativeMemory.add(ptr);
                        } else {
                            Log.w(TAG, "Failed to allocate native memory");
                            while(!aNativeMemory.isEmpty()) {
                                nativeFree(aNativeMemory.removeFirst());
                            }
                            System.gc();
                        }
                    }
                    while(!aNativeMemory.isEmpty()) {
                        nativeFree(aNativeMemory.removeFirst());
                    }
                }
            }, getString(R.string.native_memory));
            nativeMemory.start();
            log("startThread " + nativeMemory.getName());
        } else {
            stopThread(nativeMemory);
        }
    }
    
    private void log(String s) {
        String msg = mLogn + ": " + s + "\n";
        mLogn ++;
        Log.i(TAG, msg);
        mLogs.append(msg);
        scroll(mLogs);
    }

    private void scroll(TextView tv) {
        int lines = tv.getLineCount();
        if(lines > 0) {
            int y = Math.max(0, tv.getLayout().getLineTop(lines) - tv.getHeight());
            tv.scrollTo(0, y);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        String reason;
        switch(level) {
            case TRIM_MEMORY_BACKGROUND:
                reason = "BACKGROUND";
                break;
            case TRIM_MEMORY_COMPLETE:
                reason = "COMPLETE";
                break;
            case TRIM_MEMORY_MODERATE:
                reason = "MODERATE";
                break;
            case TRIM_MEMORY_RUNNING_CRITICAL:
                reason = "RUNNING CRITICAL";
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                reason = "RUNNING LOW";
                break;
            case TRIM_MEMORY_RUNNING_MODERATE:
                reason = "RUNNING MODERATE";
                break;
            case TRIM_MEMORY_UI_HIDDEN:
                reason = "UI HIDDEN";
                break;
            default:
                reason = "[INVALID]";
                break;
        }

        log("onTrimMemory(" + level + "): " + reason);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        log("onLowMemory()");
    }
}