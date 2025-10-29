package com.example.lifesync;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom View to display progress as a circular arc.
 */
public class CircularProgressView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;
    private float progress = 0f; // Progress in percentage (0-100)
    private float strokeWidth = 20f;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Background circle paint
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setColor(Color.parseColor("#E2E8F0")); // Light gray

        // Progress circle paint
        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#EDACAC"));

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Use half stroke as padding so arc fits inside view bounds
        float padding = strokeWidth / 2f;

        // Set rectangle bounds
        rectF.set(padding, padding, width - padding, height - padding);

        // Draw background circle
        canvas.drawArc(rectF, 0, 360, false, backgroundPaint);

        // Draw progress arc (start at top = -90 degrees)
        float sweepAngle = (progress / 100f) * 360f;
        canvas.drawArc(rectF, -90, sweepAngle, false, progressPaint);
    }

    /**
     * Sets the progress value (0-100) and redraws the view.
     * @param progress The progress percentage.
     */
    public void setProgress(float progress) {
        this.progress = Math.min(100, Math.max(0, progress)); // Clamp between 0-100
        invalidate(); // Redraw the view
    }

    /**
     * Gets the current progress value.
     * @return The current progress percentage.
     */
    public float getProgress() {
        return progress;
    }

    // Optional: update stroke width at runtime
    public void setStrokeWidth(float px) {
        this.strokeWidth = px;
        backgroundPaint.setStrokeWidth(px);
        progressPaint.setStrokeWidth(px);
        invalidate();
    }
}
