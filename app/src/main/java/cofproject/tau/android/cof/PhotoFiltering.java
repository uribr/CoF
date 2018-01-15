package cofproject.tau.android.cof;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cofproject.tau.android.cof.Utility.CAMERA_CAPTURE_REQUEST_CODE;
import static cofproject.tau.android.cof.Utility.FILTER_SETTINGS_REQUEST_CODE;
import static cofproject.tau.android.cof.Utility.FROM_FILTERING_TO_RESULT;
import static cofproject.tau.android.cof.Utility.GALLERY_REQUEST_CODE;
import static cofproject.tau.android.cof.Utility.IMG_SIZE;
import static cofproject.tau.android.cof.Utility.LANDSCAPE;
import static cofproject.tau.android.cof.Utility.extractPresetFromDataIntent;
import static cofproject.tau.android.cof.Utility.insertPresetToDataInent;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */
//TODO - Replace reconstructing preset with modifying for better performance.
public class PhotoFiltering extends AppCompatActivity
{
    private static final String TAG = "PhotoFiltering";

    private int mImgHeight, mImgWidth;
    private boolean mfilteringDone;
    private boolean mIsLandscape;
    private boolean mSavedOnce;
    private Bitmap mOriginalBitmap;
    private Bitmap mFilteredBitmap;
    private PreFilteringButtonsFragment mPreFilterButtonFragment;
    private ImageViewFragment mOriginalImageViewFragment;
    private ImageViewFragment mFilteredImageViewFragment;

    private PostFilteringButtonsFragment mPostFilterButtonFragment;
    private Preset mPreset;

    private Uri mURI;
    private File mFile;
    private boolean mIsFiltered = false;
    private boolean mIsShared = false;

    private Mat mImToProcess;
    private Mat mFilteredImage;

    private Mat mMaskToCollect;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private static void addImageToGallery(final String filePath, final Context context)
    {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }


    private void internalSaveOperation()
    {
        String root = Environment.getExternalStorageDirectory().toString() + "/CoF";
        File myDir = new File(root);

        //noinspection ResultOfMethodCallIgnored
        myDir.mkdirs();

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss_ddMMyyyy");
        String currentDateandTime = sdf.format(new Date());
        String fileName = "CoF_" + currentDateandTime + ".jpg";
        File file = new File(myDir, fileName);
        if (file.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        try
        {
            FileOutputStream out = new FileOutputStream(file);
            mFilteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            addImageToGallery(file.getAbsolutePath(), this);
            Toast.makeText(getApplicationContext(), "Image saved: " + fileName, Toast.LENGTH_LONG).show();
            mSavedOnce = true;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, String.format("onCreate: No.%d", countCreates));
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mIsLandscape = true;
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_photo_filtering);

        // Create filtering related buttons fragment and
        mPreFilterButtonFragment = new PreFilteringButtonsFragment();
//        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener()
//        {
//            @Override
//            public void onBackStackChanged()
//            {
//                FragmentManager fm = getFragmentManager();
//                for (int i = 0; i < fm.getBackStackEntryCount(); i++)
//                {
//                    if(fm.getBackStackEntryAt(i).equals(mPreFilterButtonFragment))
//                }
//
//
//            }
//        });
        getFragmentManager().beginTransaction().add(R.id.filtering_activity_button_container, mPreFilterButtonFragment).commit();

        // Create a new folder for images in the applications directory

        // Create and send a camera/gallery intent
        Bundle extras = getIntent().getExtras();
        if (extras == null)
        {
            return;
        }

        Intent intent = new Intent();

        if (extras.getBoolean(getString(R.string.Capture)))
        {
            mFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "tmp_img_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            mURI = Uri.fromFile(mFile);
            intent.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
        } else
        {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_an_app)), GALLERY_REQUEST_CODE);
        }

    }


    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK)
        {
            if ((requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_CAPTURE_REQUEST_CODE))
            {
                try
                {
                    if (mOriginalBitmap != null)
                    {
                        mOriginalBitmap.recycle();
                    }
                    mURI = data.getData();
                    if (mURI != null)
                    {
                        // store image
                        mOriginalBitmap = Utility.getBitmap(this, mURI);
                        //                    stream = getContentResolver().openInputStream(data.getData());
                        //                    mOriginalBitmap = BitmapFactory.decodeStream(stream);
                        if (mOriginalBitmap != null)
                        {
                            mImgHeight = mOriginalBitmap.getHeight();
                            mImgWidth = mOriginalBitmap.getWidth();
                        } else
                        {
                            Log.e(TAG, "onActivityResult: mOriginalBitmap != null", new NullPointerException("mOriginalBitmap != null"));
                        }
            try
            {
                if (mOriginalBitmap != null) { mOriginalBitmap.recycle(); }

                if (requestCode == GALLERY_REQUEST_CODE) {
                    mURI = data.getData();
                }
                if(mURI != null)
                {
                    // store image
                    mOriginalBitmap = Util.getBitmap(this, mURI);

                    if (requestCode == CAMERA_CAPTURE_REQUEST_CODE) {
                        // if our image was taken from the camera - delete it (keep it only in the bitmap)
                        File f = new File(mURI.getPath());
                        if (f.exists()) {
                            f.delete();
                        }
                    }

                    if (mOriginalBitmap != null)
                    {
                        mImgHeight = mOriginalBitmap.getHeight();
                        mImgWidth = mOriginalBitmap.getWidth();
                    }
                    else
                    {
                        Log.e(TAG, "onActivityResult: mOriginalBitmap != null", new NullPointerException("mOriginalBitmap != null"));
                    }

                    // Initialize image view fragment that will hold the image.
                    if(mOriginalImageViewFragment == null) {mOriginalImageViewFragment = new ImageViewFragment();}
                    // Add the image fragment to the container.
                    getFragmentManager().beginTransaction().add(R.id.main_view_container, mOriginalImageViewFragment).commit();
                    mOriginalImageViewFragment.setImage(mOriginalBitmap);


                        if (mIsLandscape)
                        {
                            onFilterSettingsClick(new View(this));
                        }
                    }

else
                    {
                    finish();
            }
            }catch (Exception e) { e.printStackTrace(); }


        }

            else if (requestCode == FILTER_SETTINGS_REQUEST_CODE)
            {
                if (data != null)
                {
                    Preset tmp = extractPresetFromDataIntent(data);
                    if (tmp != null)
                    {
                        mPreset = tmp;
                    }
                }
            }
        }
    }


    public boolean getFilteringDone()
    {
        return this.mfilteringDone;
    }


    public void onFilterSettingsClick(View view)
    {

        Intent intent = new Intent(this, FilterSettings.class);
        int img_size = Math.min(mImgHeight, mImgWidth);
        // If the current preset is not null, put its content in the intent.
//        if (mPreset != null)
//        {
//           insertPresetToDataInent(mPreset, intent, img_size);

//            intent.putExtra(PRESET_NAME, mPreset.getName());
//            intent.putExtra(SIGMA, mPreset.getSigma());
//            if (mPreset.isRelative())
//            {
//                intent.putExtra(WINDOW_SIZE, mPreset.getWindowSize(img_size));
//            } else
//            {
//                intent.putExtra(WINDOW_SIZE, mPreset.getWindowSize());
//            }
//            intent.putExtra(ITERATIONS, mPreset.getNumberOfIteration());
//            intent.putExtra(QUANTIZATION, mPreset.getQuantization());
//            intent.putExtra(IS_RELATIVE,mPreset.isRelative());
//        }
        if (mPreset == null)
        {
            mPreset = Preset.createPreset(img_size);
        }
        insertPresetToDataInent(mPreset, intent, img_size);
        // Notify the activity if we're in landscape mode.
        intent.putExtra(LANDSCAPE, mIsLandscape);
        intent.putExtra(IMG_SIZE, img_size);
        startActivityForResult(intent, FILTER_SETTINGS_REQUEST_CODE);
    }

    /**
     * @param view
     */
    public void onScribbleOn(View view)
    {
        Switch sw = view.findViewById(R.id.scribble_switch);
        if (!sw.isChecked())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning");
            builder.setMessage("This action will delete all scribble points created so far.\nAre you sure you want to continue?\n");
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    mOriginalImageViewFragment.clearScribble();
                }
            });
            builder.setNegativeButton("no", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Switch scribble = findViewById(R.id.scribble_switch);
                    scribble.setChecked(true);
                }
            });
            builder.setCancelable(false);
            builder.create().show();
        } else
        {
            mOriginalImageViewFragment.turnScribbleOn();
        }
    }

    private void startFiltering() {

    /**
     * @param view
     */
    public void onApplyFilterClick(View view)
    {
        // Based on code from: https://stackoverflow.com/questions/43513919/android-alert-dialog-with-one-two-and-three-buttons/43513920#43513920

        // Verify that all of the parameters are within the proper ranges.
//        if (!mFilteringParametersFragment.verifyValues())
//        {
//            // setup the alert builder
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("Error");
//            builder.setMessage("Please enter the filters parameters.\n\nIf you are unsure what to enter press the help button.");
//
//            // add a button
//            builder.setPositiveButton("OK", null);
//
//            // create and show the alert dialog
//            AlertDialog dialog = builder.create();
//            dialog.show();
//            return;
//        }

        Log.i(TAG, "onApplyFilterClick: onClick event!");


        //if (mPreFilterButtonFragment.isScribbleOn())
        if (false) // todo - handle scribble later on
        {
            //mFilteredBitmap = CoF.applyFilter(mOriginalBitmap, mPreset, mOriginalImageViewFragment.getScribbleCoordinates());
        }

        // begin filtering!!!
        if (mImToProcess != null)
        {
            mImToProcess.release();
        }
        mImToProcess = new Mat();

        Utils.bitmapToMat(mOriginalBitmap, mImToProcess);
        try
        {
            //todo - remove this!!!!!!
            try
            {
                mImToProcess.release();
                mImToProcess = Utils.loadResource(this, R.drawable.field, Imgcodecs.IMREAD_COLOR); // loading as BGR!!!
                //
                //Imgproc.cvtColor(mImToProcess, rgb, Imgproc.COLOR_BGR2RGB);
                mImToProcess = Utils.loadResource(this, R.drawable.olive, Imgcodecs.IMREAD_COLOR); // loading as BGR!!!
                Imgproc.cvtColor(mImToProcess, mImToProcess, Imgproc.COLOR_BGR2RGB);
            } catch (IOException e)
            {
                e.printStackTrace();
            }





            final ProgressDialog ringProgressDialog = ProgressDialog.show(this, "Applying Co-Occurrence Filter", "Please wait ...",  true);
            ringProgressDialog.setCancelable(false);
            ringProgressDialog.setCanceledOnTouchOutside(false);
            Thread filterThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    String TAG = "launcherDialogTag";
                    try {

                        // prepare a new Mat object for the filtered image
                        if (mFilteredImage != null) {
                            mFilteredImage.release();
                        }
                        mFilteredImage = new Mat(mImToProcess.size(), mImToProcess.type());

                        // apply the filter!
                        CoF.applyFilter(mImToProcess, mFilteredImage, mPreset);

                        // convert filterd image to uint8 type
                        mFilteredImage.convertTo(mFilteredImage, CvType.CV_8UC(mFilteredImage.channels()));

                        // save the filtered Mat into Bitmap
                        mFilteredBitmap = Bitmap.createBitmap(mFilteredImage.cols(), mFilteredImage.rows(), Bitmap.Config.RGB_565);
                        Utils.matToBitmap(mFilteredImage, mFilteredBitmap, true);

                        // Create the filtered image view.
                        if (mFilteredImageViewFragment == null)
                        {
                            mFilteredImageViewFragment = new ImageViewFragment();
                        }
                        mFilteredImageViewFragment.setImage(mFilteredBitmap);
                        mfilteringDone = true;
                        mSavedOnce = false;
                        mIsShared = false;
                        mIsFiltered = true;

                        Log.i(TAG, "Filtering finished");
                    } catch (Exception e)
                    {
                        Log.e(TAG, e.getMessage(), e);
                    }


                    ringProgressDialog.dismiss();

                    // Create the post filtering fragment of buttons if it is the first time
                    if (mPostFilterButtonFragment == null)
                    {
                        mPostFilterButtonFragment = new PostFilteringButtonsFragment();
                    }
                    // Replacing the in-filtering fragment of buttons with the post-filtering fragment of buttons.
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.filtering_activity_button_container, mPostFilterButtonFragment);
                    transaction.replace(R.id.main_view_container, mFilteredImageViewFragment);
                    transaction.addToBackStack(FROM_FILTERING_TO_RESULT);
                    transaction.commit();
                }
            });
            filterThread.start();

            }
        catch (Exception e)
        {
            Log.e(TAG, "startFiltering: ERROR - " + e.getMessage(), e);
        }
    }


    /**
     *
     * @param view
     */
    public void onApplyFilterClick(View view)
    {

        Log.i(TAG, "onApplyFilterClick: onClick event!");
        if (mOriginalBitmap == null)
        {
            Log.e(TAG, "onApplyFilterClick: mOriginalBitmap == null", new NullPointerException("mOriginalBitmap == null"));
        }
        // Update to user changes
        if (mFilteringParametersFragment != null)
        {
            mFilteringParametersFragment.updatePreset(mPreset);
        }



        //if (mPreFilterButtonFragment.isScribbleOn())
        if (false) // todo - handle scribble later on
        {
            //mFilteredBitmap = CoF.applyFilter(mOriginalBitmap, mPreset, mOriginalImageViewFragment.getScribbleCoordinates());
        }

        // begin filtering!!!
        if (mImToProcess != null) {
            mImToProcess.release();
        }
        mImToProcess = new Mat();

        Utils.bitmapToMat(mOriginalBitmap, mImToProcess);

        // the image loaded from the bitmap is RGBa - convert it to RGB
        Imgproc.cvtColor(mImToProcess, mImToProcess, Imgproc.COLOR_RGBA2RGB);

        startFiltering();

    }

    /**
     *
     * @param view
     */


    /**
     * @param view
     */
    public void onHelpClick(View view)
    {
        //TODO - show a tutorial of the application that should be used when the user first reache a new screen. Note that the tutorial is screen-dependent
    }


    /**
     * @param view
     */
    public void onSaveResultClick(View view)
    {
        Log.i(TAG, "onSaveResultClick:  onClick event");
        if (!mSavedOnce)
        {
            internalSaveOperation();
        }
    }


    /**
     * @param view
     */
    public void onShareClick(View view)
    {
        // CREDIT: https://goo.gl/wQvQeh
        Log.i(TAG, "onShareClick: onClick event");

        // save bitmap to cache directory
        try
        {

            File cachePath = new File(this.getCacheDir(), "images");
            if (!cachePath.mkdirs())
            {
                // don't forget to make the directory
                Log.i(TAG, "onShareClick: Failed to create directory.");
            }
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.jpg"); // overwrites this image every time
            mFilteredBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

        } catch (IOException e)
        {
            Log.e(TAG, "onShareClick: IOExceotion - " + e.getMessage(), e );
            e.printStackTrace();
        }

        File imagePath = new File(this.getCacheDir(), "images");
        File newFile = new File(imagePath, "image.jpg");
        Uri contentUri = FileProvider.getUriForFile(this, "cofproject.tau.android.cof.fileprovider", newFile);

        if (contentUri != null)
        {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_an_app)));
            mIsShared = true;
        }
    }


    @SuppressLint("ApplySharedPref")
    public void onDeletePresetClick(View view)
    {
        // Default preset cannot be deleted but it can be overridden.
        if(!mPreset.getName().equals(getString(R.string.DefaultPresetName)))
        {
            SharedPreferences.Editor editor = mPresetPref.edit();
            editor.remove(mPreset.getName());
            editor.commit();
            mFilteringParametersFragment.onRemovedPreset(mPreset.getName());
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Cannot delete default preset", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (!mIsFiltered || mSavedOnce || mIsShared) {
            super.onBackPressed();
            return;
        }
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("WARNING");
        alertDialog.setMessage(R.string.back_pressed_msg);
        // Add the buttons
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mIsFiltered = false;
                PhotoFiltering.super.onBackPressed();

            }
        });

        alertDialog.setNegativeButton(getString(R.string.no),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())
        {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else
        {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    @Override
    protected void onDestroy()
    {
        Log.i(TAG, "onDestroy: started");
        if (mImToProcess != null)
        {
            mImToProcess.release();
        }
        if (mFilteredImage != null)
        {
            mFilteredImage.release();
        }
        if (mMaskToCollect != null)
        {
            mMaskToCollect.release();
        }

        super.onDestroy();

    }


}
