// Credit: https://stackoverflow.com/questions/47837857/efficiently-drawing-over-an-imageview-that-resides-inside-of-a-fragment-in-respo
package cofproject.tau.android.cof;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Uri on 21/12/2017.
 */

public class DottedView extends View
{
    private ArrayList<Pair<Integer, Integer>> mScribblePointsCoords = new ArrayList<>();
    private int radius = 5;
    private Paint mPaint;

    public DottedView(Context context)
    {
        super(context);
        init();
    }

    public DottedView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public DottedView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        for (Pair<Integer, Integer> pair: mScribblePointsCoords)
        {
            canvas.drawCircle(pair.first, pair.second, radius, mPaint);
        }
    }

    public void addScribblePointsCoords(Pair pair)
    {
        mScribblePointsCoords.add(pair);
    }

    public List<Pair<Integer, Integer>> getScribbleCoordinatesList()
    {
        return mScribblePointsCoords;
    }

    public void clearScribble()
    {
        mScribblePointsCoords.clear();
        this.invalidate();
    }

    private void init()
    {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLUE);
    }
}
