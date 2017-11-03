package cofproject.tau.android.cof;

import android.graphics.Bitmap;
import android.view.View;

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
    public Bitmap Apply(Bitmap bitmap)
    {
        //TODO - implement the filter XD
        return bitmap;
    }


}
