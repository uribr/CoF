package cofproject.tau.android.cof;


import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by Uri on 31/10/2017.
 */

public class ImageViewFragment extends Fragment implements View.OnTouchListener
{
    private static final String TAG = "ImageViewFragment";
    private ImageView mImageView;
    private DottedView mOverlayView;
    private Bitmap mBitmap;
    private View mView;
    private boolean mFirstLoading;

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        v.onTouchEvent(event);
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //mScribbleState = false;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mFirstLoading = true;
        if (mBitmap != null)
        {
            setAndResizeImageView();
        }
    }

    public void setImage(Bitmap bmp)
    {
        mBitmap = bmp.copy(bmp.getConfig(), false);
        if (this.isResumed())
        {
            setAndResizeImageView();
        }
    }

    private void setAndResizeImageView()
    {
        // set image in imageview
        mImageView.setImageBitmap(mBitmap);

        // resize image only in the initial loading
        if (mFirstLoading) {

            mFirstLoading = false;
            // tighten the ImageView around the image when possible
            mImageView.post(new Runnable() {
                @Override
                public void run() {
                    final int actualHeight, actualWidth;
//                final int fragmentHeight = mView.getHeight();
//                final int fragmentWidth = mView.getWidth();
                    final int imageViewHeight = mImageView.getHeight();
                    final int imageViewWidth = mImageView.getWidth();
                    final int bitmapHeight = mBitmap.getHeight();
                    final int bitmapWidth = mBitmap.getWidth();
                    //final float density = getContext().getResources().getDisplayMetrics().density;
                    if (imageViewHeight * bitmapWidth <= imageViewWidth * bitmapHeight) {
                        actualWidth = bitmapWidth * imageViewHeight / bitmapHeight;
                        actualHeight = imageViewHeight;
                    } else {
                        actualHeight = bitmapHeight * imageViewWidth / bitmapWidth;
                        actualWidth = imageViewWidth;
                    }

                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(actualWidth, actualHeight);
//                int horizontalMargin = ((int)(density * (fragmentWidth - actualWidth) / 2));
//                int verticalMargin = ((int)(density * (fragmentHeight - actualHeight) / 2));
//                params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
                    mImageView.setLayoutParams(params);
                    mImageView.setForegroundGravity(Gravity.CENTER);


                }
            });

        }


    }

    // Credit: https://stackoverflow.com/questions/47837857/efficiently-drawing-over-an-imageview-that-resides-inside-of-a-fragment-in-respo

    /**
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mView = inflater.inflate(R.layout.image_view_fragment, container, false);
        mOverlayView = mView.findViewById(R.id.dottedView);
        mImageView = mView.findViewById(R.id.new_photo_view);
        mImageView.setImageBitmap(mBitmap);
        return mView;
    }

    public void turnScribbleOn()
    {
        //mScribbleState = true;
        mOverlayView.setScribbleState(true);
    }

    /**
     * Remove all the scribbled coordinates and drawings
     */
    public void clearScribble()
    {
        mOverlayView.clearScribble();
        mOverlayView.setScribbleState(false);
        //mScribbleState = false;
    }

    /**
     * @return the Path object with the scribble contours
     */
    public Path getScribblePath() {
        return mOverlayView.getPath();
    }

    public void drawPathOnView(Path path) {
        mOverlayView.drawPath(path);
    }

    public ImageView getImageView()
    {
        return mImageView;
    }

    public int getImageViewHeight() {return mImageView.getHeight();}
    public int getImageViewWidth() {return mImageView.getWidth();}
}
