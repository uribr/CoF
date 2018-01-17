package cofproject.tau.android.cof;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by Uri on 31/10/2017.
 */

public class ImageViewFragment extends Fragment implements View.OnTouchListener
{
    private static final String TAG = "ImageViewFragment";
    private ImageView mImageView;
    private DottedView mOverlayView;
    private Bitmap mBitmap;
    private boolean mScribbleState;
    private View mView;
    //    private Pair<Integer, Integer> mInitialPoint;
//    private List<Pair<Integer, Integer>> mTouchHistroy;
    private Paint mPaint;

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
        mScribbleState = false;
    }

    @Override
    public void onResume()
    {
        super.onResume();
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
        mImageView.setImageBitmap(Utility.getResizedBitmap(mBitmap, 500));

        // tighten the ImageView around the image when possible
        mImageView.post(new Runnable()
        {
            @Override
            public void run()
            {
                final int actualHeight, actualWidth;
                final int fragmentHeight = mView.getHeight();
                final int fragmentWidth = mView.getWidth();
                final int imageViewHeight = mImageView.getHeight();
                final int imageViewWidth = mImageView.getWidth();
                final int bitmapHeight = mBitmap.getHeight();
                final int bitmapWidth = mBitmap.getWidth();
                final float density = getContext().getResources().getDisplayMetrics().density;
                if (imageViewHeight * bitmapWidth <= imageViewWidth * bitmapHeight)
                {
                    actualWidth = bitmapWidth * imageViewHeight / bitmapHeight;
                    actualHeight = imageViewHeight;
                }
                else
                {
                    actualHeight = bitmapHeight * imageViewWidth / bitmapWidth;
                    actualWidth = imageViewWidth;
                }

                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(actualWidth, actualHeight);
                int horizontalMargin = ((int)(density * (fragmentWidth - actualWidth) / 2));
                int verticalMargin = ((int)(density * (fragmentHeight - actualHeight) / 2));
                params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
                mImageView.setLayoutParams(params);
                mImageView.setForegroundGravity(Gravity.CENTER);


            }
        });

    }

    // Credit: https://stackoverflow.com/questions/47837857/efficiently-drawing-over-an-imageview-that-resides-inside-of-a-fragment-in-respo

    /**
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mView = inflater.inflate(R.layout.image_view_fragment, container, false);
//        mTouchHistroy = new ArrayList<>();
        mOverlayView = mView.findViewById(R.id.dottedView);
        // Set listener to react to touch events in the image plane
        mOverlayView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent event)
            {
                // If the scribble switch is turned on and the event was that of
                // movement (to that end down and up events are considered movements as well
                int eventAction = event.getAction();
                boolean consumed = false;
                if (mScribbleState)
                {
                    if (eventAction == MotionEvent.ACTION_DOWN)
                    {
                        consumed = true;
                        Pair<Integer, Integer> p = new Pair<>((int) event.getX(), (int) event.getY());
                        Log.d(TAG, "onTouch: start point =  " + p.toString());
                        mOverlayView.addScribblePointsCoords(new Pair<>((int) event.getX(), (int) event.getY()), true, false);

                    } else if (eventAction == MotionEvent.ACTION_MOVE)
                    {
                        // Add the point of touch movement to the list of scribble points
                        consumed = true;
                        Pair<Integer, Integer> p = new Pair<>((int) event.getX(), (int) event.getY());
                        Log.d(TAG, "onTouch: " + p.toString());
                        mOverlayView.addScribblePointsCoords(new Pair<>((int) event.getX(), (int) event.getY()), false, false);
                    } else if (eventAction == MotionEvent.ACTION_UP)
                    {
                        consumed = true;
                        Pair<Integer, Integer> p = new Pair<>((int) event.getX(), (int) event.getY());
                        Log.d(TAG, "onTouch: end point = " + p.toString());
                        mOverlayView.addScribblePointsCoords(new Pair<>((int) event.getX(), (int) event.getY()), false, true);
                        // Invalidate the plane above the image (the DottedView) forcing the view to call its' OnDraw method.
                        mOverlayView.invalidate();
                    }
                }
                return consumed;
            }
        });

//        view.setOnTouchListener(new View.OnTouchListener()
//        {
//            @Override
//            public boolean onTouch(View v, MotionEvent event)
//            {
//                if(event.getAction() == MotionEvent.ACTION_DOWN)
//                {
//                    int x = (int) event.getX();
//                    int y = (int) event.getY();
//                    mScribblePointsCoords.add(new Pair<>(x, y));
//                    Bitmap tempBitmap = mBitmap.copy(mBitmap.getConfig(), true);
//                    Canvas canvas = new Canvas(tempBitmap);f
//                    canvas.drawCircle(x, y, R.dimen.default_scribble_point_radius, mPaint);
//                    mImageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
//                    mBitmap = tempBitmap;
//                    return true;
//                }
//                return false;
//            }
//        });
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mImageView = mView.findViewById(R.id.new_photo_view);
        mImageView.setImageBitmap(mBitmap);
        return mView;
    }

    public void turnScribbleOn()
    {
        mScribbleState = true;
    }

    /**
     * Remove all the scribbled coordinates and drawings
     */
    public void clearScribble()
    {
        mOverlayView.clearScribble();
        mScribbleState = false;
    }

    /**
     * @return list of Pairs<Integer, Integer> representing the coordinates of the
     * scribbled points in the DottedView view that hangs above the image
     */
    public List<Pair<Integer, Integer>> getScribbleCoordinates()
    {
        return mOverlayView.getScribbleCoordinatesList();
    }

    public ImageView getImageView()
    {
        return mImageView;
    }
}
