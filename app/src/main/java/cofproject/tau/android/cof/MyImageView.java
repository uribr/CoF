package cofproject.tau.android.cof;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A class extending Android's AppCompatImageView, adding drawing abilities.
 */
public class MyImageView extends android.support.v7.widget.AppCompatImageView {
    private static final int TOUCH_TOLERANCE = 4;
    private static final float PAINT_STROKE_WIDTH = 10f;

    private final Paint mPaint;
    private final Path mPath;
    private boolean mScribbleState;
    private float mX, mY;


    /**
     * A constructor for the class, initialing the Paint and Path objects.
     * @param context context
     * @param attrs attrs
     */
    public MyImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(PAINT_STROKE_WIDTH);

        mPath = new Path();
        mScribbleState = false;
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mPath, mPaint);
    }

    /**
     * A getter method for mPath.
     * @return Gets the scribble path.
     */
    public Path getPath() {
        return mPath;
    }

    /**
     * Clears the scribble drawn on the image.
     */
    public void clearScribble() {
        mPath.reset();
        invalidate();
    }

    /**
     * Sets the scribble state of the image
     * @param scribbleState The new scribble state
     */
    public void setScribbleState(boolean scribbleState) {
        mScribbleState = scribbleState;
    }

    public boolean isScribbleOn() {
        return mScribbleState;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScribbleState) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchMove(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touchUp();
                    invalidate();
                    break;
                default: // should not get here
                    return false;
            }
        }
        return mScribbleState;
    }


    private void touchStart(float x, float y) {
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    public int getPaintColor() {
        return mPaint.getColor();
    }

    public void setPaintColor(int paintColor) {
        mPaint.setColor(paintColor);
        invalidate();
    }
}
