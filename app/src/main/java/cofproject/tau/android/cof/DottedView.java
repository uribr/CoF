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
import java.util.Iterator;
import java.util.List;

/**
 * Created by Uri on 21/12/2017.
 */

public class DottedView extends View
{
    private ArrayList<ArrayList<Pair<Integer, Integer>>> mScribblePointsCoords = new ArrayList<>();
    private ArrayList<Pair<Integer, Integer>> mTempGroup;
    private Paint mPaint;
    private int mRadius;

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
        Pair<Integer, Integer> point1 = null;
        Pair<Integer, Integer> point2 = null;
        Iterator<ArrayList<Pair<Integer, Integer>>> groupIter = mScribblePointsCoords.iterator();

        for (ArrayList<Pair<Integer, Integer>> group : mScribblePointsCoords)
        {
            if (group.size() == 1)
            {
                point1 = mScribblePointsCoords.get(0).get(0);
                canvas.drawCircle(point1.first, point1.second, mRadius, mPaint);
                continue;
            }

            point1 = group.get(0);
            for (int index = 0; index < group.size(); index++)
            {
                point2 = group.get(index);
                mPaint.setStrokeWidth(mRadius);
                canvas.drawLine(point1.first, point1.second, point2.first, point2.second, mPaint);
                point1 = point2;
            }
        }
    }

    public void addScribblePointsCoords(Pair<Integer, Integer> pair, boolean first, boolean last)
    {
        if (first && last)
        {
            return;
        }

        if (first)
        {
            mTempGroup = new ArrayList<>();
        }

        if (!mTempGroup.contains(pair))
        {
            mTempGroup.add(pair);
        }

        if (last)
        {
            mScribblePointsCoords.add(mTempGroup);
        }
    }


    public List<Pair<Integer, Integer>> getScribbleCoordinatesList()
    {
        ArrayList<Pair<Integer, Integer>> pairs = new ArrayList<>();
        for (ArrayList<Pair<Integer, Integer>> lst : mScribblePointsCoords)
        {
            pairs.addAll(lst);
        }
        return pairs;
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
        mScribblePointsCoords = new ArrayList<>();
        mTempGroup = new ArrayList<>();
        mRadius = 10;
    }
}
