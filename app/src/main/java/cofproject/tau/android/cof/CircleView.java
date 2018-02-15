package cofproject.tau.android.cof;

// CREDIT: https://stackoverflow.com/questions/25961263/draw-a-circle-onto-a-view-android
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CircleView extends View {
    private static final int DEFAULT_CIRCLE_COLOR = Color.BLUE;

    private int mColor;
    private Paint mPaint;

    public CircleView(Context context) {
        super(context);
        init(context, null);
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        //invalidate();
    }

    private void init(Context context, AttributeSet attrs) {
        mPaint = new Paint();
        mColor = DEFAULT_CIRCLE_COLOR;
        mPaint.setAntiAlias(true);
    }

    public void setColor(int color) {
        this.mColor = color;
        invalidate();
    }

    public int getColor() {
        return mColor;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();

        int usableWidth = w - (pl + pr);
        int usableHeight = h - (pt + pb);

        int radius = Math.min(usableWidth, usableHeight) / 6;
        int cx = pl + (usableWidth / 2);
        int cy = pt + (usableHeight / 2);

        mPaint.setColor(mColor);
        canvas.drawCircle(cx, cy, radius, mPaint);
    }


}