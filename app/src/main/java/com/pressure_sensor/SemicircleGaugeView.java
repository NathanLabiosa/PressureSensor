package com.pressure_sensor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SemicircleGaugeView extends View {
    private float currentPressure = 0f;
    private float maxPressure = 10000f; // e.g., 10,000 as the max

    // Cutoffs for each zone
    private float greenCutoff = 600f;
    private float yellowCutoff = 6000f;

    public SemicircleGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private Paint arcPaint;
    private Paint needlePaint;

    private void init() {
        // For the arcs
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(100f);  // Thick stroke for arcs


        // For the needle
        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeWidth(10f);  // Thin stroke for needle
        needlePaint.setColor(Color.BLACK);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Get the full available width from the parent's measure spec.
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Set the height to be half the width (for a perfect semicircle).
        int height = width / 2;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // Use viewHeight as the diameter of the circle so that the semicircle fits vertically.
        float radius = viewHeight; // diameter = viewHeight * 2, but we'll adjust the rectangle so it fits within the view.
        // Center the circle horizontally.
        float left = (viewWidth - 2 * radius) / 2f + 50;
        float top = arcPaint.getStrokeWidth() / 2f;
        float right = left + 2 * radius - 100;
        float bottom = top + 2 * radius;
        RectF rect = new RectF(left, top, right, bottom);

        // Calculate angles for each zone, out of 180 total degrees.
        float greenAngle = 180f * (greenCutoff / maxPressure);
        float yellowAngle = 180f * (yellowCutoff / maxPressure);

        // 1) Draw the GREEN zone (0..600)
        arcPaint.setColor(Color.GREEN);
        canvas.drawArc(rect, 180, greenAngle, false, arcPaint);

        // 2) Draw the YELLOW zone (600..6000)
        arcPaint.setColor(Color.YELLOW);
        canvas.drawArc(rect, 180 + greenAngle, yellowAngle - greenAngle, false, arcPaint);

        // 3) Draw the RED zone (6000..maxPressure)
        arcPaint.setColor(Color.RED);
        canvas.drawArc(rect, 180 + yellowAngle, 180 - yellowAngle, false, arcPaint);

        // Draw the needle according to the current pressure.
        float clampedPressure = Math.max(0, Math.min(currentPressure, maxPressure));
        float currentAngle = 180f * (clampedPressure / maxPressure);
        double radians = Math.toRadians(180 - currentAngle);
        float centerX = viewWidth / 2f;
        float centerY = viewHeight; // bottom of the view is the center of our semicircle
        float needleLength = radius; // extend the needle to the circle's edge

        float needleX = (float) (centerX + needleLength * Math.cos(radians));
        float needleY = (float) (centerY - needleLength * Math.sin(radians));

        needlePaint.setColor(Color.BLACK);
        canvas.drawLine(centerX, centerY, needleX, needleY, needlePaint);
    }


    /**
     * Updates the gauge with a new pressure reading and redraws.
     */
    public void setPressure(float pressure) {
        this.currentPressure = pressure;
        invalidate(); // Trigger a redraw
    }

    /**
     * Optionally allow adjusting the max pressure or zone cutoffs if needed.
     */
    public void setMaxPressure(float maxPressure) {
        this.maxPressure = maxPressure;
        invalidate();
    }

    public void setZoneCutoffs(float greenCutoff, float yellowCutoff) {
        this.greenCutoff = greenCutoff;
        this.yellowCutoff = yellowCutoff;
        invalidate();
    }
}
