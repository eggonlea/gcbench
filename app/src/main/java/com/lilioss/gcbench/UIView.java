package com.lilioss.gcbench;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;


/**
 * TODO: document your custom view class.
 */
public class UIView extends View {
    private StatDraw statDraw;

    public UIView(Context context) {
        super(context);
        init(null, 0);
    }

    public UIView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Set up a default TextPaint object
        statDraw = new StatDraw("UIView", Color.RED, Color.MAGENTA, Color.GRAY);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        statDraw.resize(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        statDraw.draw(canvas);
        this.invalidate();
    }
}
