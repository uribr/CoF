package cofproject.tau.android.cof;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Created by Uri on 31/10/2017.
 */

public class ImageViewFragment extends Fragment implements View.OnTouchListener
{
    private static final String TAG = "ImageViewFragment";
    private MyImageView mImageView;
    private Bitmap mBitmap;
    private boolean mFirstLoading;
    private boolean mScribbleOn;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        v.onTouchEvent(event);
        return true;
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
            Log.d(TAG, "setAndResizeImageView: setting new image");

            mFirstLoading = false;
            // tighten the ImageView around the image when possible
            mImageView.post(new Runnable() {
                @Override
                public void run() {
                    final int actualHeight, actualWidth;
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
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(actualWidth, actualHeight);
                    params.gravity = Gravity.CENTER;
                    mImageView.setLayoutParams(params);
                }
            });

        }


    }

    // Credit: https://stackoverflow.com/questions/47837857/efficiently-drawing-over-an-imageview-that-resides-inside-of-a-fragment-in-respo


    public boolean isScribbleOn() {
        return mScribbleOn;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.image_view_fragment, container, false);
        mScribbleOn = false;
        mImageView = view.findViewById(R.id.image_view);
        mImageView.setImageBitmap(mBitmap);
        return view;
    }

    public void setScribbleOn(boolean state)
    {
        mScribbleOn = state;
        mImageView.setScribbleState(state);
    }

    /**
     * Remove all the scribbled coordinates and drawings
     */
    public void clearScribble(boolean newScribbleState)
    {
        mScribbleOn = newScribbleState;
        mImageView.clearScribble();
        mImageView.setScribbleState(newScribbleState);
    }

    /**
     * @return the Path object with the scribble contours
     */
    public Path getScribblePath() {
        return mImageView.getPath();
    }

//    public void drawPathOnView(Path path) {
//        mImageView.drawPath(path);
//    }
//
//    public ImageView getImageView()
//    {
//        return mImageView;
//    }

    public int getImageViewHeight() {return mImageView.getHeight();}
    public int getImageViewWidth() {return mImageView.getWidth();}
}
