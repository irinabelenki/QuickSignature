package com.irinabelenki.quicksignature;

/**
 * Created by Irina on 12/11/2016.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

public class TouchImageView extends ImageView {
    private Matrix matrix;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private int mode = NONE;

    private PointF last = new PointF();
    private PointF start = new PointF();
    private float minScale = 1f;
    private float maxScale = 3f;
    private float[] m;
    private int viewWidth, viewHeight;
    private static final int CLICK = 3;
    private float saveScale = 1f;
    protected float origWidth, origHeight;
    private int oldMeasuredWidth, oldMeasuredHeight;
    private ScaleGestureDetector mScaleDetector;
    private Context context;

    public static String TAG = "TouchImageView";

    public TouchImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    private void sharedConstructing(Context context) {
        super.setClickable(true);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        matrix = new Matrix();
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                Log.d(TAG, "action: " + event.toString());

                PointF curr = new PointF(event.getX(), event.getY());
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        last.set(curr);
                        start.set(last);
                        mode = DRAG;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            float fixTransX = getFixDragTrans(deltaX, viewWidth, origWidth * saveScale);
                            float fixTransY = getFixDragTrans(deltaY, viewHeight, origHeight * saveScale);
                            matrix.postTranslate(fixTransX, fixTransY);
                            fixTrans();
                            last.set(curr.x, curr.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        int xDiff = (int) Math.abs(curr.x - start.x);
                        int yDiff = (int) Math.abs(curr.y - start.y);
                        if (xDiff < CLICK && yDiff < CLICK)
                            performClick();
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;

                }
                setImageMatrix(matrix);
                invalidate();
                return true; // indicate event was handled
            }
        });
    }

    public void setMaxZoom(float x) {
        maxScale = x;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            Log.d(TAG, "scaleFactor: " + scaleFactor);
            Log.d(TAG, "saveScale: " + minScale + " minScale: " + saveScale + " maxScale: " + maxScale);
            float origScale = saveScale;
            saveScale *= scaleFactor;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                scaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                scaleFactor = minScale / origScale;
            }
            Log.d(TAG, "scaleFactor: " + scaleFactor);

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight)
                matrix.postScale(scaleFactor, scaleFactor, viewWidth / 2, viewHeight / 2);
            else
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            fixTrans();
            return true;
        }
    }

    void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];
        float fixTransX = getFixTrans(transX, viewWidth, origWidth * saveScale);
        float fixTransY = getFixTrans(transY, viewHeight, origHeight * saveScale);
        if (fixTransX != 0 || fixTransY != 0) {
            matrix.postTranslate(fixTransX, fixTransY);
        }
    }

    float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;
        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }

    float getFixDragTrans(float delta, float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return 0;
        }
        return delta;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        Log.d(TAG, "onMeasure, mode: " + mode);

        //
        // Rescales image on rotation
        //
        if (oldMeasuredWidth == viewWidth && oldMeasuredHeight == viewHeight
                || viewWidth == 0 || viewHeight == 0)
            return;
        oldMeasuredHeight = viewHeight;
        oldMeasuredWidth = viewWidth;
        if (saveScale == 1) {
            //Fit to screen.
            float scale;
            Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0)
                return;
            int bmWidth = drawable.getIntrinsicWidth();
            int bmHeight = drawable.getIntrinsicHeight();
            Log.d(TAG, "bmWidth: " + bmWidth + " bmHeight : " + bmHeight);
            float scaleX = (float) viewWidth / (float) bmWidth;
            float scaleY = (float) viewHeight / (float) bmHeight;
            scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);
            // Center the image
            float redundantYSpace = (float) viewHeight - (scale * (float) bmHeight);
            float redundantXSpace = (float) viewWidth - (scale * (float) bmWidth);
            redundantYSpace /= (float) 2;
            redundantXSpace /= (float) 2;
            matrix.postTranslate(redundantXSpace, redundantYSpace);
            origWidth = viewWidth - 2 * redundantXSpace;
            origHeight = viewHeight - 2 * redundantYSpace;
            setImageMatrix(matrix);
        }
        fixTrans();
    }

    private int bitmapLeft = -1;
    private int bitmapTop = -1;
    private int bitmapRight = -1;
    private int bitmapBottom = -1;
    Bitmap overlayBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888); // this creates a MUTABLE bitmap
    private int SHIFT = 50;
    Paint bitmapPaint = new Paint();

    public void setBitmapCoordinates(int left, int top, int right, int bottom) {
        this.bitmapLeft = left;
        this.bitmapTop = top;
        this.bitmapRight = right;
        this.bitmapBottom = bottom;
    }

    public void bitmapUp() {
        if (inLimits(bitmapLeft, bitmapTop - SHIFT, bitmapRight, bitmapBottom - SHIFT)) {
            bitmapTop -= SHIFT;
            bitmapBottom -= SHIFT;
        }
    }

    public void bitmapDown() {
        if (inLimits(bitmapLeft, bitmapTop + SHIFT, bitmapRight, bitmapBottom + SHIFT)) {
            bitmapTop += SHIFT;
            bitmapBottom += SHIFT;
        }
    }

    public void bitmapLeft() {
        if (inLimits(bitmapLeft - SHIFT, bitmapTop, bitmapRight - SHIFT, bitmapBottom)) {
            bitmapLeft -= SHIFT;
            bitmapRight -= SHIFT;
        }
    }

    public void bitmapRight() {
        if (inLimits(bitmapLeft + SHIFT, bitmapTop, bitmapRight + SHIFT, bitmapBottom)) {
            bitmapLeft += SHIFT;
            bitmapRight += SHIFT;
        }
    }

    private boolean bitmapInit() {
        return bitmapLeft > -1 && bitmapTop > -1 && bitmapRight > -1 && bitmapBottom > -1;
    }

    private boolean inLimits(int left, int top, int right, int bottom) {
        //int[] location = new int[2];
        //getLocationOnScreen(location);
        //int x = location[0];
        //int y = location[1];
        if (left >= 0 && left <= getWidth() &&
                top >= 0 && top <= getHeight() &&
                right >= 0 && right <= getWidth() &&
                bottom >= 0 && bottom <= getHeight() &&
                right - left <= getWidth() &&
                bottom - top <= getHeight()
                ) {
            return true;
        }
        return false;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmapInit()) {
            canvas.drawBitmap(overlayBitmap, 0, 0, null);
            bitmapPaint.setColor(Color.RED);
            canvas.drawRect(new RectF(bitmapLeft, bitmapTop, bitmapRight, bitmapBottom), bitmapPaint);
        }
    }
}

