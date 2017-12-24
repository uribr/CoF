package cofproject.tau.android.cof;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Uri on 29/10/2017.
 */

public class CoFilter
{
    private double mSpatialSigma;
    private int mWindowHeight;
    private int mWindowWidth;

    public CoFilter(double ss, int height, int width)
    {
        mSpatialSigma = ss;
        mWindowHeight = height;
        mWindowWidth = width;
        //TODO perform any necessary OpenCV initialization.
    }



    /**
     * Apply the CoF to the image.
     * @param bitmap
     * @return
     */
    public Bitmap Apply(Bitmap bitmap, int iterations, List<Pair<Integer, Integer>> scribbleList)
    {
        //TODO - implement the filter XD
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

    }

    /**
     *
     * @param bitmap
     * @param iterations
     * @return
     */
    public Bitmap Apply(Bitmap bitmap, int iterations)
    {
        return Apply(bitmap, iterations, new ArrayList<Pair<Integer, Integer>>());
    }


}
