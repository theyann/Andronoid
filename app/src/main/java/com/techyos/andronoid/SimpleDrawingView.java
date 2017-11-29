package com.techyos.andronoid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philippesimons on 25/11/2017.
 */

public class SimpleDrawingView extends View {

    // setup initial color
    private final int paintColor = Color.BLACK;
    // defines paint and canvas
    private Paint drawPaint;
    // stores next circle
    private Path path = new Path();

    private float mPreviousX = 0;
    private float mPreviousY = 0;
    private final List<PointF> mPoints = new ArrayList<>();

    public SimpleDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setupPaint();
    }

    private void setupPaint() {
        // Setup paint with color and stroke styles
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, drawPaint);
    }

    public List<PointF> getPoints() {
        return mPoints;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float pointX = event.getX();
        float pointY = event.getY();
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPoints.clear();
                mPreviousX = pointX;
                mPreviousY = pointY;

                path.reset();
                path.moveTo(pointX, pointY);
                return true;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(pointX, pointY);
                mPoints.add(new PointF(pointX - mPreviousX, pointY - mPreviousY));
                mPreviousX = pointX;
                mPreviousY = pointY;
                break;

            default:
                return false;
        }

        // Force a view to draw again
        postInvalidate();
        return true;
    }
}