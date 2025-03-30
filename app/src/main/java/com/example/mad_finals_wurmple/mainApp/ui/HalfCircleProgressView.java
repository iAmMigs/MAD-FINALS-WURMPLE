package com.example.mad_finals_wurmple.mainApp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class HalfCircleProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF arcRect;

    private float progress = 0f; // 0 to 1
    private int progressColor = Color.parseColor("#508D4E");
    private int backgroundColor = Color.LTGRAY;
    private float strokeWidth = 50f;

    public HalfCircleProgressView(Context context) {
        super(context);
        init();
    }

    public HalfCircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HalfCircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setAntiAlias(true);

        arcRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate the rectangle that will contain our arc
        // We use padding to ensure the arc doesn't get cut off
        float padding = strokeWidth / 2f;
        arcRect.set(
                padding,
                padding,
                w - padding,
                2 * h - padding
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background half circle (180 degrees)
        canvas.drawArc(arcRect, 180, 180, false, backgroundPaint);

        // Draw progress arc based on the current progress (0 to 180 degrees)
        float progressAngle = 180 * progress;
        canvas.drawArc(arcRect, 180, progressAngle, false, progressPaint);
    }

    /**
     * Set the progress value (0.0 to 1.0)
     * @param progress value between 0 and 1
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    /**
     * Set the progress color
     * @param color color value
     */
    public void setProgressColor(int color) {
        this.progressColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    /**
     * Set the background color
     * @param color color value
     */
    public void setBackgroundArcColor(int color) {
        this.backgroundColor = color;
        backgroundPaint.setColor(color);
        invalidate();
    }

    /**
     * Set the stroke width for both arcs
     * @param width stroke width
     */
    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        backgroundPaint.setStrokeWidth(width);
        progressPaint.setStrokeWidth(width);
        invalidate();
    }
}