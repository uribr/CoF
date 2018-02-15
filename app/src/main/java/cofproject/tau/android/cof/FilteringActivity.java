package cofproject.tau.android.cof;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.common.base.Stopwatch;
import com.google.common.primitives.Bytes;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static cofproject.tau.android.cof.Utilities.FILTER_SETTINGS_REQUEST_CODE;
import static cofproject.tau.android.cof.Utilities.IMG_SIZE;
import static cofproject.tau.android.cof.Utilities.SCRIBBLE_COLOR_KEY;
import static cofproject.tau.android.cof.Utilities.currentPresetFile;
import static cofproject.tau.android.cof.Utilities.defaultPresetFile;
import static cofproject.tau.android.cof.Utilities.loadCurrentPreset;
import static cofproject.tau.android.cof.Utilities.loadDefaultPreset;
import static cofproject.tau.android.cof.Utilities.updateCurrentPreset;

/**
 * In this activity all the filtering process occurs - the image is loaded and then the user is able
 * to choose parameters, scribble on the image, and filter it using either CoF or FB-CoF.
 *
 * @see CoF
 */
public class FilteringActivity extends AppCompatActivity implements ButtonsFragment.ButtonsFragmentListener {
    private static final String TAG = "FilteringActivity";
    private static final String FROM_ORIGINAL_TO_FILTERING = "from original image to filtering";
    private static final String FROM_FILTERING_TO_RESULT = "from filtering to result";
    private static final String FROM_SCRIBBLE_TO_THRESHOLD = "from scribble to threshold";
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;
    private static final float PATH_RESOLUTION = (float) 0.01;

    private int mImgHeight, mImgWidth;
    //private boolean mIsLandscape;
    private boolean mSavedOnce;
    private Bitmap mOriginalBitmap;
    private Bitmap mFilteredBitmap;
    private ImageViewFragment mImageViewFragment;
    //    private SharedPreferences mCurrentPreset;
//    private SharedPreferences mDefaultPreset;
    private Preset mPreset;
    private Uri mURI;
    private boolean mIsFiltered;
    private boolean mIsShared;
    private SwitchCompat mScribbleSwitch;

    private FilteringMode mFilteringMode;

    private int mScribbleThreshold;
    private Mat mImToFilter;
    private Mat mFilteredImage;
    private Mat mScribbleImage;
    private Mat mForegroundMask;
    private Mat mBackgroundMask;
    private Mat mImToCollect;
    private Mat mFilteredScribble;

    /**
     * Clears the scribbles off the image. After the button is pressed, the user may draw scribbles
     * again - no need to turn the switch on
     *
     * @param view The "clear scribble" button
     */
    public void onClearScribbleClick(View view) {
        Log.i(TAG, "onClearScribbleClick: cleared scribble");
        mImageViewFragment.clearScribble();
        mImageViewFragment.setScribbleState(true);
    }


    private enum FilteringMode {
        REGULAR_FILTERING,
        SCRIBBLE_INITIALIZATION,
        FOREGROUND_BACKGROUND
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE)
//        {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            mIsLandscape = true;
//        } else
//        {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
        setContentView(R.layout.activity_filtering);
        mFilteringMode = FilteringMode.REGULAR_FILTERING;


        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(TAG, "onCreate: extras == null", new NullPointerException("extras == null"));
            finish();
            return;
        }

        // Create and send a camera/gallery intent
        Intent intent = new Intent();
        if (extras.getBoolean(getString(R.string.Capture))) {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "tmp_img_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            mURI = Uri.fromFile(file);
            intent.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
        } else {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_an_app)), GALLERY_REQUEST_CODE);
        }

        // Open preference files.
        currentPresetFile = getSharedPreferences(getString(R.string.CurrentPresetSharedPreferenceFileName), Context.MODE_PRIVATE);
        defaultPresetFile = getSharedPreferences(getString(R.string.DefaultPresetSharedPreferenceFileName), Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_filtering);
        initFragments();
    }

    @Override
    public void configSeekBar(SeekBar seekBar) {
        if (seekBar == null) {
            return;
        }
        seekBar.setMax(Utilities.SCRIBBLE_THRESHOLD_MAX_VAL);
        seekBar.setProgress(mScribbleThreshold);
        onChangeScribbleThreshold(mScribbleThreshold);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                onChangeScribbleThreshold(i);
                mScribbleThreshold = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void configSwitch(SwitchCompat switchCompat) {
        if (switchCompat == null) {
            return;
        }
        switchCompat.setChecked(mFilteringMode != FilteringMode.REGULAR_FILTERING);
    }

    private static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void internalSaveOperation() {
        String root = Environment.getExternalStorageDirectory().toString() + "/CoF";
        File myDir = new File(root);

        //noinspection ResultOfMethodCallIgnored
        myDir.mkdirs();

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss_ddMMyyyy");
        String currentDateandTime = sdf.format(new Date());
        String fileName = "CoF_" + currentDateandTime + ".jpg";
        File file = new File(myDir, fileName);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            mFilteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            addImageToGallery(file.getAbsolutePath(), this);
            Toast.makeText(getApplicationContext(), "Image saved: " + fileName, Toast.LENGTH_SHORT).show();
            mSavedOnce = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void testSave(Mat imToSave, String fileName) {
//        String root = Environment.getExternalStorageDirectory().toString() + "/Scribble Stuff";
//        File myDir = new File(root);
//
//        //noinspection ResultOfMethodCallIgnored
//        myDir.mkdirs();
//
//        File file = new File(myDir, fileName);
//        if (file.exists()) {
//            //noinspection ResultOfMethodCallIgnored
//            file.delete();
//        }
//        try {
//            FileOutputStream out = new FileOutputStream(file);
//            Bitmap bmp = Bitmap.createBitmap(imToSave.cols(), imToSave.rows(), Bitmap.Config.RGB_565);
//            Utils.matToBitmap(imToSave, bmp, true);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
//            out.flush();
//            out.close();
//            addImageToGallery(file.getAbsolutePath(), this);
//            Toast.makeText(getApplicationContext(), "Image saved: " + fileName, Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            finish();
            return;
        }
        switch (requestCode) {
            case GALLERY_REQUEST_CODE:
                mURI = data.getData();
            case CAMERA_CAPTURE_REQUEST_CODE:
                try {
                    if (mOriginalBitmap != null) {
                        mOriginalBitmap.recycle();
                    }
                    if (mURI != null) {
                        // storePreset image
                        mOriginalBitmap = Utilities.getBitmap(this, mURI);

                        // if our image was taken from the camera - delete it (keep it only in the bitmap)
                        File f = new File(mURI.getPath());
                        if (f.exists() && requestCode == CAMERA_CAPTURE_REQUEST_CODE) {
                            f.delete();
                        }

                        if (mOriginalBitmap != null) {
                            mImgHeight = mOriginalBitmap.getHeight();
                            mImgWidth = mOriginalBitmap.getWidth();
                        } else {
                            Log.e(TAG, "onActivityResult: mOriginalBitmap != null", new NullPointerException("mOriginalBitmap != null"));
                        }
                        initFragments();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mPreset = loadDefaultPreset(Math.min(mImgHeight, mImgWidth));
                    // Set the default preset as the current preset
                    updateCurrentPreset(mPreset, Math.min(mImgHeight, mImgWidth));
                }
                break;
            case FILTER_SETTINGS_REQUEST_CODE:
                if (mImageViewFragment.isScribbleOn()) {
                    mScribbleSwitch.setChecked(true);
                }
                if (data != null) {
                    mPreset = loadCurrentPreset();
                    mImageViewFragment.setPaintColor(data.getIntExtra(SCRIBBLE_COLOR_KEY, Color.BLUE));
                }
                break;
        }
    }


    /**
     * Starts the activity of the filter settings
     *
     * @param view The filter settings button.
     */
    public void onFilterSettingsClick(View view) {
        Intent intent = new Intent(this, FilterSettingsActivity.class);

        // Notify the activity if we're in landscape mode.
        //intent.putExtra(LANDSCAPE, mIsLandscape);
        intent.putExtra(IMG_SIZE, Math.min(mImgHeight, mImgWidth));
        intent.putExtra(SCRIBBLE_COLOR_KEY, mImageViewFragment.getPaintColor());
        startActivityForResult(intent, FILTER_SETTINGS_REQUEST_CODE);
    }


    /**
     * Toggles the scribble switch.
     *
     * @param view The scribble switch.
     */
    public void onScribbleSwitch(View view) {

        mScribbleSwitch = view.findViewById(R.id.scribble_switch);
        if (mScribbleSwitch.isChecked()) {
            mImageViewFragment.setScribbleState(true);
            mFilteringMode = FilteringMode.SCRIBBLE_INITIALIZATION;
        } else {
            Path p = mImageViewFragment.getScribblePath();
            if (p != null && !p.isEmpty()) {
                showClearScribbleDialog(false);
            } else {
                mImageViewFragment.setScribbleState(false);
                mFilteringMode = FilteringMode.REGULAR_FILTERING;
            }
        }
    }


    private void showClearScribbleDialog(final boolean backPressed) {
        int msgId = (backPressed && !mIsFiltered) ? R.string.scribble_back_msg : R.string.scribble_switch_off_msg;
        AlertDialog.Builder alertDialog = Utilities.generateBasicAlertDialog(this, msgId);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                mFilteringMode = FilteringMode.REGULAR_FILTERING;
                if (backPressed) {
                    FilteringActivity.super.onBackPressed();
                    if (!mIsFiltered) {
                        return;
                    }
                    mImageViewFragment.setImage(mOriginalBitmap);
                    mIsFiltered = false;
                } else {
                    mImageViewFragment.clearScribble();
                }
                mImageViewFragment.setScribbleState(false);

            }
        });
        alertDialog.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mScribbleSwitch.setChecked(true);
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.create().show();

    }

    /**
     * Cancels the scribble mask thresholding process, restoring the original image.
     *
     * @param view The cancel button.
     */
    public void onCancelClick(View view) {
        this.onBackPressed();
    }

    /**
     * Starts the performance of FB-CoF after thresholding the foreground mask.
     *
     * @param view The continue button.
     */
    public void onContinueScribbleClick(View view) {

        mForegroundMask = new Mat();
        mBackgroundMask = new Mat();
        Scalar threshold = new Scalar(mScribbleThreshold);
        // threshold the filtered image (CoFed scribbled mask) to create the foreground mask
        Core.compare(mFilteredScribble, threshold, mForegroundMask, Core.CMP_GE);
        // compare foreground mask to 0 to get the background mask
        Core.compare(mFilteredScribble, threshold, mBackgroundMask, Core.CMP_LT);

        // erode the background mask (to reduce the number of FG pixels in it)
        if (mScribbleThreshold == 0) {
            mScribbleThreshold++;
        }
        int seWinSize = (int) Math.round(255.0 / mScribbleThreshold);
        Mat SE = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(seWinSize, seWinSize));
        Imgproc.erode(mBackgroundMask, mBackgroundMask, SE);
        SE.release();

        //todo - remove this (for the paper only)
//        testSave(mForegroundMask, "fg_mask.jpg");
//        testSave(mBackgroundMask, "bg_mask.jpg");
        //todo - until here

        // convert masks to 32-bit float, 1/0 matrix
        mForegroundMask.convertTo(mForegroundMask, CvType.CV_32FC1, 1.0 / 255.0);
        mBackgroundMask.convertTo(mBackgroundMask, CvType.CV_32FC1, 1.0 / 255.0);

        mFilteredImage.release();
        mFilteredImage = new Mat(mImToFilter.size(), mImToFilter.type());

        mFilteringMode = FilteringMode.FOREGROUND_BACKGROUND;
        new FilteringAsyncTask().execute();
    }


    @SuppressLint("StaticFieldLeak")
    private class FilteringAsyncTask extends AsyncTask<Void, String, Void> {

        private final String TAG = "FilteringAsyncTask";

        private ProgressDialog progressDialog;
        private Stopwatch stopwatch;
        private int iterCnt;
        private int nBins;
        private int winSize;
        private double sigma;

        //todo - to be wired
        private int winSizeFilt;
        private double sigmaFilt;
        private int winSizeFB;
        private int iterCntFB;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            int titleId = R.string.applying_cof_title;
            switch (mFilteringMode) {
                case REGULAR_FILTERING:
                    titleId = R.string.applying_cof_title;
                    break;
                case SCRIBBLE_INITIALIZATION:
                    titleId = R.string.scribble_init_title;
                    break;
                case FOREGROUND_BACKGROUND:
                    titleId = R.string.applying_fb_cof_title;
                    break;
            }
            // disable the screen fro going to sleep
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressDialog = new ProgressDialog(FilteringActivity.this);
            progressDialog.setTitle(titleId);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        private void regularFiltering() {
            if (mImToFilter.rows() != mFilteredImage.rows() || mImToFilter.cols() != mFilteredImage.cols()) {
                Log.d(TAG, "regularFiltering: imToProcess.size() != filteredImage.size()", new IllegalArgumentException("imToProcess.size() != filteredImage.size()"));
            }

            publishProgress(getString(R.string.filtering_phase_qunatization));
            stopwatch.reset();
            stopwatch.start();
            CoF.quantize(mImToFilter, mImToCollect, nBins);
            stopwatch.stop();
            Log.d(TAG, "regularFiltering: quantize time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

            Mat pab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
            Mat pmi = new Mat(pab.size(), pab.type());

            publishProgress(getString(R.string.filtering_phase_collect_Pab));
            stopwatch.reset();
            stopwatch.start();
            // collecting with the default all-ones mask
            CoF.collectPab(mImToCollect, pab, nBins, winSize, sigma);
            stopwatch.stop();
            Log.d(TAG, "regularFiltering: collectPab time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
            CoF.pabToPmi(pab, pmi);

            // clone image to filter according to the filtering mode
            Mat imToFilterCopy = (mFilteringMode == FilteringMode.SCRIBBLE_INITIALIZATION) ? mScribbleImage.clone() : mImToFilter.clone();
            System.gc();
            stopwatch.reset();
            stopwatch.start();
            for (int i = 0; i < iterCnt; i++) {
                Log.d(TAG, "regularFiltering: cofilter iteration No. " + (i + 1) + "/" + iterCnt);
                publishProgress(getString(R.string.filtering_phase_iter_count) + (i + 1) + "/" + iterCnt);
                CoF.coFilter(imToFilterCopy, mImToCollect, mFilteredImage, pmi, winSize, sigma);
                mFilteredImage.copyTo(imToFilterCopy);
                System.gc();
            }
            stopwatch.stop();
            Log.d(TAG, "regularFiltering: coFilter time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
            Utilities.releaseMats(pab, pmi, imToFilterCopy);

            // convert filterd image to uint8 type
            mFilteredImage.convertTo(mFilteredImage, CvType.CV_8UC(mFilteredImage.channels()));
            // save the filtered Mat into Bitmap
            mFilteredBitmap = Bitmap.createBitmap(mFilteredImage.cols(), mFilteredImage.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(mFilteredImage, mFilteredBitmap, true);
        }

        private void fgBgFiltering() {
            Mat fgPab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
            Mat bgPab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);

            publishProgress(getString(R.string.filtering_phase_collect_fg_Pab));
            stopwatch.reset();
            stopwatch.start();
            CoF.collectPab(mImToCollect, mForegroundMask, fgPab, nBins, winSize, sigma);
            stopwatch.stop();
            Log.d(TAG, "fgBgFiltering: collectPab (foreground) time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

            publishProgress(getString(R.string.filtering_phase_collect_bg_Pab));
            stopwatch.reset();
            stopwatch.start();
            CoF.collectPab(mImToCollect, mBackgroundMask, bgPab, nBins, winSize, sigma);
            stopwatch.stop();
            Log.d(TAG, "fgBgFiltering: collectPab (background) time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
            Core.add(bgPab, new Scalar(Math.pow(10, -10)), bgPab);

            Mat imToFilterCopy = mImToFilter.clone();

            stopwatch.reset();
            stopwatch.start();
            for (int i = 0; i < 3; i++) { //todo - change hard coded values!
                Log.d(TAG, "fgBgFiltering: FBCofilter iteration No. " + (i + 1) + "/" + 3);
                publishProgress(getString(R.string.filtering_phase_iter_count) + (i + 1) + "/" + 3);
                CoF.FBCoFilter(imToFilterCopy, mImToCollect, mFilteredImage, fgPab, bgPab, 15);
                mFilteredImage.copyTo(imToFilterCopy);
                System.gc();
            }
            stopwatch.stop();
            Log.d(TAG, "fgBgFiltering: FBCoFilter time: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
            Utilities.releaseMats(fgPab, bgPab, imToFilterCopy);
        }

        private void initParams() {
            //todo - split parameters extraction according to filtering mode
            // extract parmeters from preset
            iterCnt = mPreset != null ? mPreset.getNumberOfIteration() : Utilities.DEFAULT_NUMBER_OF_ITERATIONS;
            sigma = mPreset != null ? mPreset.getStatSigma() : Utilities.DEFAULT_SIGMA;
            nBins = mPreset != null ? mPreset.getQuantization() : Utilities.DEFAULT_QUNTIZATION_LEVEL;
            winSize = mPreset != null ? mPreset.getStatWindowSize() : Utilities.DEFAULT_WINDOW_SIZE;
            if (winSize % 2 == 0) {
                winSize--;
            }
        }


        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.i(TAG, "doInBackground: Started filtering");
                initParams();

                stopwatch = Stopwatch.createUnstarted(); // stopwatch to measure times
                Log.i(TAG, "doInBackground: filtering mode: " + mFilteringMode.name());
                switch (mFilteringMode) {
                    case SCRIBBLE_INITIALIZATION:
                        // init scribble
                        mScribbleThreshold = Utilities.SCRIBBLE_THRESHOLD_INIT_VAL;
                        publishProgress(getString(R.string.filtering_phase_processing_scribble));
                        generateScribbleImage();
                        // fall through
                    case REGULAR_FILTERING:
                        regularFiltering();
                        break;
                    case FOREGROUND_BACKGROUND:
                        fgBgFiltering();
                        break;
                }
                Log.i(TAG, "doInBackground: Filtering finished");

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mIsFiltered = true;

            int relevantLayoutId;
            String strToBackStack;
            if (mFilteringMode == FilteringMode.SCRIBBLE_INITIALIZATION) {
                Utilities.releaseMats(mFilteredScribble);
                mFilteredScribble = mFilteredImage.clone();
                //todo - remove this (for the paper only)
                //testSave(mFilteredScribble, "filtered_scribble.jpg");
                //todo - until here

                relevantLayoutId = R.layout.scribble_mask_threshold_fragment;
                strToBackStack = FROM_SCRIBBLE_TO_THRESHOLD;
                mImageViewFragment.clearScribble();
            } else {

                // convert filterd image to uint8 type
                mFilteredImage.convertTo(mFilteredImage, CvType.CV_8UC(mFilteredImage.channels()));
                // save the filtered Mat into Bitmap
                mFilteredBitmap = Bitmap.createBitmap(mFilteredImage.cols(), mFilteredImage.rows(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mFilteredImage, mFilteredBitmap, true);
                mImageViewFragment.setImage(mFilteredBitmap);

                mIsShared = false;
                mSavedOnce = false;

                // Create the post filtering fragment
                relevantLayoutId = R.layout.post_filtering_buttons_fragment;
                strToBackStack = FROM_FILTERING_TO_RESULT;

            }
            mImageViewFragment.setScribbleState(false);
            Fragment relevantFragment = ButtonsFragment.newInstance(relevantLayoutId);
            replaceButtonsFragments(relevantFragment, strToBackStack);
            // allow the screen to sleep again
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressDialog.dismiss();
        }

    }


    /**
     * Starts the performance of CoF on the input image
     *
     * @param view The apply filter button
     */
    public void onApplyFilterClick(View view) {

        Log.i(TAG, "onApplyFilterClick: onClick event!");
        mIsFiltered = false;

        if (mOriginalBitmap == null) {
            Log.e(TAG, "onApplyFilterClick: mOriginalBitmap == null", new NullPointerException("mOriginalBitmap == null"));
        }

        Utilities.releaseMats(mImToFilter, mImToCollect, mFilteredImage);

        mImToFilter = new Mat();
        Utils.bitmapToMat(mOriginalBitmap, mImToFilter);
        // the image loaded from the bitmap is RGBa - convert it to RGB
        Imgproc.cvtColor(mImToFilter, mImToFilter, Imgproc.COLOR_RGBA2RGB);
        mFilteredImage = new Mat(mImToFilter.size(), mImToFilter.type());

        // this matrix will hold the quantization mapping.
        // we assume there are no more than 256 bins, so we can use byte-typed matrix
        mImToCollect = new Mat(mImToFilter.size(), CvType.CV_8UC1);

        //FilteringMode mode = mImageViewFragment.isScribbleOn() ? FilteringMode.SCRIBBLE_INITIALIZATION : FilteringMode.REGULAR_FILTERING;
        new FilteringAsyncTask().execute();
    }

    private void generateScribbleImage() {

        Path scribblePath = mImageViewFragment.getScribblePath();
        int height = mImageViewFragment.getImageViewHeight();
        int width = mImageViewFragment.getImageViewWidth();
        Mat scribbleMat = new Mat(new Size(width, height), CvType.CV_8UC1);

        pathToMat(scribblePath, scribbleMat, height, width);

        Utilities.releaseMats(mScribbleImage);

        mScribbleImage = new Mat();
        // resize the binary image to the original image size
        Imgproc.resize(scribbleMat, mScribbleImage, mImToFilter.size());

        //todo - remove this (for the paper only)
//        Mat cpy = mImToFilter.clone();
//        cpy.setTo(new Scalar(255, 0, 0), mScribbleImage);
//        testSave(cpy, "im_with_scribble.jpg");
        //todo - until here

        // perform dilation in order to thicken the white scribble lines
        Mat SE = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, Utilities.SCRIBBLE_DILATION_WINDOW_SIZE);
        Imgproc.dilate(mScribbleImage, mScribbleImage, SE, new Point(-1, -1), Utilities.SCRIBBLE_DILATION_ITERATIONS_DEFAULT);
        Utilities.releaseMats(scribbleMat, SE);
    }

    private void onChangeScribbleThreshold(int threshold) {
        Mat mask = new Mat();
        Mat tmp = new Mat();
        Core.compare(mFilteredScribble, new Scalar(threshold), mask, Core.CMP_GE);
        mImToFilter.copyTo(tmp, mask);

        double alpha = 0.7;
        Core.addWeighted(tmp, alpha, mImToFilter, 1 - alpha, 0, tmp);

        mFilteredBitmap = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(tmp, mFilteredBitmap, true);
        // update the image view.
        mImageViewFragment.setImage(mFilteredBitmap);

        Utilities.releaseMats(mask, tmp);
    }

    /**
     * @param scribblePath the path containing the scribbles drawn by the user
     * @param scribbleMat  a matrix - will hold 255 in each scribble point.
     *                     Should be pre-allocated before calling the function.
     * @param height       array height
     * @param width        array width
     */
    private void pathToMat(Path scribblePath, Mat scribbleMat, int height, int width) {
        byte[][] scribbleArr = new byte[height][width];
        PathMeasure pm = new PathMeasure(scribblePath, false);
        float contourLength, distance;
        int numPoints, x, y;
        float[] position = new float[2];
        do {
            contourLength = pm.getLength();
            numPoints = (int) (contourLength / PATH_RESOLUTION) + 1;
            for (int i = 0; i < numPoints; i++) {
                distance = (i * contourLength) / (numPoints - 1);
                if (pm.getPosTan(distance, position, null)) {
                    x = (int) position[1];
                    y = (int) position[0];
                    if (x < 0 || x >= height || y < 0 || y >= width) {
                        continue;
                    }
                    scribbleArr[x][y] = (byte) 255;
                }
            }
        } while (pm.nextContour());

        // flatten the 2D array
        byte[] flattened = Bytes.concat(scribbleArr);
        // store the array contents in a Mat object
        scribbleMat.put(0, 0, flattened);
    }


    /**
     * Saves the filtered image
     *
     * @param view The save image button
     */
    public void onSaveResultClick(View view) {
        Log.i(TAG, "onSaveResultClick:  onClick event");
        if (!mSavedOnce) {
            // save image
            internalSaveOperation();
        } else {
            Toast.makeText(this, "Image already saved", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Starts a sharing intent in order to share the filtered image
     *
     * @param view The share image button
     */
    public void onShareClick(View view) {
        // CREDIT: https://goo.gl/wQvQeh
        Log.i(TAG, "onShareClick: onClick event");

        // save bitmap to cache directory
        try {

            File cachePath = new File(this.getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.jpg"); // overwrites this image every time
            mFilteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.flush();
            stream.close();

        } catch (IOException e) {
            Log.e(TAG, "onShareClick: IOExceotion - " + e.getMessage(), e);
            e.printStackTrace();
        }

        File imagePath = new File(this.getCacheDir(), "images");
        File newFile = new File(imagePath, "image.jpg");
        Uri contentUri = FileProvider.getUriForFile(this, "cofproject.tau.android.cof.fileprovider", newFile);

        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_an_app)));
            mIsShared = true;
        }
    }


    private void backPressAlert() {
        // here the image is filtered and wasn't saved or shared - warn the user:
        AlertDialog.Builder alertDialog = Utilities.generateBasicAlertDialog(this, R.string.back_pressed_msg);
        // Add the buttons
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                FilteringActivity.super.onBackPressed();
                if (mFilteringMode == FilteringMode.REGULAR_FILTERING) {
                    mIsFiltered = false;
                    mImageViewFragment.setImage(mOriginalBitmap);
                } else if (mFilteringMode == FilteringMode.FOREGROUND_BACKGROUND) {
                    mFilteringMode = FilteringMode.SCRIBBLE_INITIALIZATION;
                }
            }
        });

        alertDialog.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void onBackPressedRegularFiltering() {
        if (!mIsFiltered) {
            super.onBackPressed();
        } else if (mSavedOnce || mIsShared) {
            super.onBackPressed();
            mImageViewFragment.setImage(mOriginalBitmap);
            mIsFiltered = false;
        } else {
            backPressAlert();
        }
    }

    @Override
    public void onBackPressed() {

        switch (mFilteringMode) {
            case REGULAR_FILTERING:
                onBackPressedRegularFiltering();
                break;
            case SCRIBBLE_INITIALIZATION:
                Path p = mImageViewFragment.getScribblePath();
                if (!mIsFiltered && p != null && p.isEmpty()) {
                    super.onBackPressed();
                } else {
                    showClearScribbleDialog(true);
                }
                break;
            case FOREGROUND_BACKGROUND:
                if (mSavedOnce || mIsShared) {
                    mFilteringMode = FilteringMode.REGULAR_FILTERING;
                    super.onBackPressed();
                    super.onBackPressed();
                    mImageViewFragment.setImage(mOriginalBitmap);
                    mImageViewFragment.setScribbleState(false);
                    mIsFiltered = false;
                } else {
                    backPressAlert();
                }
                break;
        }
    }

    private void replaceButtonsFragments(Fragment fragment) {
        replaceButtonsFragments(fragment, null);
    }

    private void replaceButtonsFragments(Fragment fragment, String strToBackstack) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.filtering_activity_button_container, fragment);
        if (strToBackstack != null) {
            transaction.addToBackStack(strToBackstack);
        }
        transaction.commit();
    }

    private void initFragments() {
        mImageViewFragment = new ImageViewFragment();
        Bitmap relevantBitmap = mIsFiltered ? mFilteredBitmap : mOriginalBitmap;
        int layoutId = mIsFiltered ? R.layout.post_filtering_buttons_fragment : R.layout.pre_filtering_buttons_fragment;
        ButtonsFragment buttonsFragment = ButtonsFragment.newInstance(layoutId);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.filtering_activity_button_container, buttonsFragment);
        transaction.add(R.id.main_view_container, mImageViewFragment);
        transaction.commit();
        mImageViewFragment.setImage(relevantBitmap);
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: started");
        Utilities.releaseMats(mImToFilter, mFilteredImage, mScribbleImage, mImToCollect, mForegroundMask, mBackgroundMask, mFilteredScribble);
        // Clear the current preset preference file
        currentPresetFile.edit().clear().apply();
        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Runs the app tutorial for the current activity.
     *
     * @param item The help button
     */
    public void onClickHelpButton(MenuItem item) {
        Log.i(TAG, "onClickHelpButton: pressed");
        int relevantLayoutId = -1;
        String tutorialKey = null;

        switch (mFilteringMode) {
            case SCRIBBLE_INITIALIZATION:
                if (mIsFiltered) {
                    relevantLayoutId = R.layout.scribble_mask_threshold_fragment;
                    tutorialKey = getString(R.string.scribble_mask_threshold_tutorial_key);
                    break;
                }
            case REGULAR_FILTERING:
                if (!mIsFiltered) {
                    relevantLayoutId = R.layout.pre_filtering_buttons_fragment;
                    tutorialKey = getString(R.string.pre_filtering_tutorial_key);
                    break;
                }
            case FOREGROUND_BACKGROUND: //the image is filtered
                relevantLayoutId = R.layout.post_filtering_buttons_fragment;
                tutorialKey = getString(R.string.post_filtering_tutorial_key);
                break;

        }
        // set the tutorialKey to be true - like the first time
        getPreferences(MODE_PRIVATE).edit().putBoolean(tutorialKey, true).apply();
        // re-create the fragment so it will call the tutorial again
        replaceButtonsFragments(ButtonsFragment.newInstance(relevantLayoutId));
    }
}
