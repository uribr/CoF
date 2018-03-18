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
 * The fragment holds the different images throughout the filtering process
 */
public class ImageViewFragment extends Fragment implements View.OnTouchListener
{
    private static final String TAG = "ImageViewFragment";
    private MyImageView mImageView;
    private Bitmap mBitmap;
    private boolean mFirstLoading;


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

    /**
     * Sets a new image to the fragment.
     * @param bmp The new image to be set.
     */
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

        // set image in imageView
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

    /**
     * A getter method for the mScribbleOn field. Indicates whether the image is in scribble mode.
     * @return true iff the image is in scribble mode.
     */
    public boolean isScribbleOn() {
        //return mScribbleOn;
        return mImageView.isScribbleOn();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.image_view_fragment, container, false);
        //mScribbleOn = false;
        mImageView = view.findViewById(R.id.image_view);
        mImageView.setImageBitmap(mBitmap);
        mImageView.setScribbleState(false);
        return view;
    }

    /**
     * A setter for the image view's scribble state
     * @param state The new scribble state of the image.
     */
    public void setScribbleState(boolean state)
    {
        mImageView.setScribbleState(state);
    }


    /**
     * Removes all the scribbles off the image
     */
    public void clearScribble() {
        mImageView.clearScribble();
    }

    /**
     * A getter method for the Path object of the image, containing the scribble data.
     * @return Path object with the scribble data.
     */
    public Path getScribblePath() {
        return mImageView.getPath();
    }

    /**
     * Returns the image-view height.
     * @return The image-view height.
     */
    public int getImageViewHeight() {return mImageView.getHeight();}

    /**
     * Returns the image-view width.
     * @return The image-view width.
     */
    public int getImageViewWidth() {return mImageView.getWidth();}


    public int getPaintColor() {
        return mImageView.getPaintColor();
    }

    public void setPaintColor(int paintColor) {
        mImageView.setPaintColor(paintColor);
    }
}
