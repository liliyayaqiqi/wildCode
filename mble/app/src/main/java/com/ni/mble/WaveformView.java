package com.ni.mble;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {

    private double[] samples;
    private double min;
    private double max;


    private Paint paint = new Paint();

    private int width;
    private int height;

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        samples = null;

        paint.setStrokeWidth(1f);
        paint.setAntiAlias(true);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    /**
     * update and redraw Visualizer view
     */
    public void updateVisualizer(double[] samples, double min, double max) {
        this.samples = samples;
        this.min = min;
        this.max = max;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (samples == null || width == 0) {
            return;
        }
        double totalBarsCount = width / dp(1);
        if (totalBarsCount <= 0.1) {
            return;
        }
        int samplesCount = samples.length;
        double samplesPerBar = samplesCount / totalBarsCount;
        double barCounter = 0;
        int nextBarNum = 0;

        int y = height;
        int barNum = 0;
        int lastBarNum;
        int drawBarCount;

        for (int a = 0; a < samplesCount; a++) {
            if (a != nextBarNum) {
                continue;
            }
            drawBarCount = 0;
            lastBarNum = nextBarNum;
            while (lastBarNum == nextBarNum) {
                barCounter += samplesPerBar;
                nextBarNum = (int) barCounter;
                drawBarCount++;
            }

            double value = (samples[a] - min) / (max - min);
            for (int b = 0; b < drawBarCount; b++) {
                float x = barNum * dp(1);
                float h = y - (float) (height * value);
                canvas.drawLine(x, y, x, h, paint);
                barNum++;
            }
        }
    }

    public int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(getContext().getResources().getDisplayMetrics().density * value);
    }
}