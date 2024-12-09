package com.pressure_sensor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SemicircleGaugeView extends View {
    private Paint paint;
    private float currentPressure = 0;
    private float maxPressure = 10000; // Max pressure

    public SemicircleGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20);
        paint.setColor(Color.BLUE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // Draw the semicircle
        RectF rect = new RectF(0, 0, width, height * 2);
        canvas.drawArc(rect, 180, 180, false, paint);

        // Calculate the angle for the current pressure
        float angle = 180 * (currentPressure / maxPressure);
        paint.setColor(Color.RED);
        canvas.drawArc(rect, 180, angle, false, paint);

        // Draw the needle
        double radians = Math.toRadians(180 - angle);
        float needleLength = width / 2;
        float needleX = (float) (width / 2 + needleLength * Math.cos(radians));
        float needleY = (float) (height - needleLength * Math.sin(radians));
        paint.setColor(Color.BLACK);
        canvas.drawLine(width / 2, height, needleX, needleY, paint);
    }

    public void setPressure(float pressure) {
        currentPressure = pressure;
        invalidate(); // Redraw the view
    }
}
