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
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cofproject.tau.android.cof.Utility.FILTER_SETTINGS_REQUEST_CODE;
import static cofproject.tau.android.cof.Utility.IMG_SIZE;
import static cofproject.tau.android.cof.Utility.LANDSCAPE;
import static cofproject.tau.android.cof.Utility.currentPresetFile;
import static cofproject.tau.android.cof.Utility.defaultPresetFile;
import static cofproject.tau.android.cof.Utility.loadCurrentPreset;
import static cofproject.tau.android.cof.Utility.loadDefaultPreset;
import static cofproject.tau.android.cof.Utility.updateCurrentPreset;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */
public class PhotoFilteringActivity extends AppCompatActivity implements ScribbleMaskThresholdFragment.OnFinishedCreateView {
    private static final String TAG = "PhotoFilteringActivity";
    private static final String FROM_ORIGINAL_TO_FILTERING = "from original image to filtering";
    private static final String FROM_FILTERING_TO_RESULT = "from filtering to result";
    private static final String FROM_SCRIBBLE_TO_THRESHOLD = "from scribble to threshold";
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;

    private int mImgHeight, mImgWidth;
    private boolean mIsLandscape;
    private boolean mSavedOnce;
    private Bitmap mOriginalBitmap;
    private Bitmap mFilteredBitmap;
    private PreFilteringButtonsFragment mPreFilterButtonFragment;
    private ImageViewFragment mOriginalImageViewFragment;
    private ImageViewFragment mFilteredImageViewFragment;
    private SharedPreferences mCurrentPreset;
    private SharedPreferences mDefaultPreset;
    private Preset mPreset;
    private Uri mURI;
    private boolean mIsFiltered;
    private boolean mIsShared;
    private SwitchCompat mScribbleSwitch;

    private int mScribbleThreshold;
    private Mat mImToFilter;
    private Mat mFilteredImage;
    private Mat mScribbleImage;
    private Mat mForegroundMask;
    private Mat mBackgroundMask;
    private Mat mImToCollect;
    private Mat mFilteredScribble;


    private enum FilteringMode {
        REGULAR_FILTERING,
        SCRIBBLE_INITIALIZATION,
        SCRIBBLE_FOREGROUND_BACKGROUND
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
        setContentView(R.layout.activity_photo_filtering);
        mScribbleThreshold = Utility.SCRIBBLE_THRESHOLD_INIT_VAL;

        // Create filtering related buttons fragment and
        mPreFilterButtonFragment = new PreFilteringButtonsFragment();
        getFragmentManager().beginTransaction().add(R.id.filtering_activity_button_container, mPreFilterButtonFragment).commit();

        // Create a new folder for images in the applications directory
        // Create and send a camera/gallery intent
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

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
        setContentView(R.layout.activity_photo_filtering);
        // todo - handle code dup
        if (mIsFiltered) {
            mFilteredImageViewFragment = new ImageViewFragment();
            PostFilteringButtonsFragment fragment = new PostFilteringButtonsFragment();
            getFragmentManager().beginTransaction().add(R.id.filtering_activity_button_container, fragment).commit();
            getFragmentManager().beginTransaction().add(R.id.main_view_container, mFilteredImageViewFragment).commit();
            mFilteredImageViewFragment.setImage(mFilteredBitmap);
        } else {
            mOriginalImageViewFragment = new ImageViewFragment();
            mPreFilterButtonFragment = new PreFilteringButtonsFragment();
            getFragmentManager().beginTransaction().add(R.id.filtering_activity_button_container, mPreFilterButtonFragment).commit();
            getFragmentManager().beginTransaction().add(R.id.main_view_container, mOriginalImageViewFragment).commit();
            mOriginalImageViewFragment.setImage(mOriginalBitmap);
        }
    }

    @Override
    public void configSeekBar(SeekBar seekBar) {
        seekBar.setMax(Utility.SCRIBBLE_THRESHOLD_MAX_VAL);
        seekBar.setProgress(Utility.SCRIBBLE_THRESHOLD_INIT_VAL);
        onChangeScribbleThreshold(seekBar.getProgress());
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
            Toast.makeText(getApplicationContext(), "Image saved: " + fileName, Toast.LENGTH_LONG).show();
            mSavedOnce = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        do {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_CAPTURE_REQUEST_CODE) {
                    try {
                        if (mOriginalBitmap != null) {
                            mOriginalBitmap.recycle();
                        }
                        if (requestCode == GALLERY_REQUEST_CODE) {
                            mURI = data.getData();
                        }

                        if (mURI != null) {
                            // storePreset image
                            mOriginalBitmap = Utility.getBitmap(this, mURI);
//
//                            List<Mat> mats = new ArrayList<>(4);
//                            int[] colors = new int[]{50,100,150,200};
//                            for(int i = 0; i < 4; i++) {
//                                mats.add(new Mat(200,100, CvType.CV_8UC1, new Scalar(colors[i])));
//                            }
//                            Mat res = new Mat();
//                            Core.hconcat(mats, res);
//                            res.convertTo(res, CvType.CV_32FC1, 1.0/255.0);
//                            Mat noise = new Mat(res.size(), CvType.CV_32FC1);
//                            Core.randn(noise, 0, 0.025);
//                            Core.add(res, noise, res);
//                            res.convertTo(res,CvType.CV_8UC1, 255);
//                            Utility.releaseMats(mats);
//                            mOriginalBitmap = Bitmap.createBitmap(res.cols(), res.rows(), Bitmap.Config.RGB_565);
//                            Utils.matToBitmap(res, mOriginalBitmap);

                            if (requestCode == CAMERA_CAPTURE_REQUEST_CODE) {
                                // if our image was taken from the camera - delete it (keep it only in the bitmap)
                                File f = new File(mURI.getPath());
                                if (f.exists()) {
                                    f.delete();
                                }
                            }

                            if (mOriginalBitmap != null) {
                                mImgHeight = mOriginalBitmap.getHeight();
                                mImgWidth = mOriginalBitmap.getWidth();
                            } else {
                                Log.e(TAG, "onActivityResult: mOriginalBitmap != null", new NullPointerException("mOriginalBitmap != null"));
                            }

                            // Initialize image view fragment that will hold the image.
                            if (mOriginalImageViewFragment == null) {
                                mOriginalImageViewFragment = new ImageViewFragment();
                            }
                            // Add the image fragment to the container.
                            getFragmentManager().beginTransaction().add(R.id.main_view_container, mOriginalImageViewFragment).commit();
                            mOriginalImageViewFragment.setImage(mOriginalBitmap);

                            if (mIsLandscape) {
                                onFilterSettingsClick(new View(this));
                            }
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        mPreset = loadDefaultPreset(Math.min(mImgHeight, mImgWidth));

                        // Set the default preset as the current preset
                        updateCurrentPreset(mPreset, Math.min(mImgHeight, mImgWidth));
                    }
                } else if (requestCode == FILTER_SETTINGS_REQUEST_CODE) {
                    if (data != null) {
                        mPreset = loadCurrentPreset();
                    }
                    return;
                }
            }
        } while (false);
        finish();
    }


    public void onFilterSettingsClick(View view) {
        Intent intent = new Intent(this, FilterSettingsActivity.class);

        // Notify the activity if we're in landscape mode.
        intent.putExtra(LANDSCAPE, mIsLandscape);
        intent.putExtra(IMG_SIZE, Math.min(mImgHeight, mImgWidth));
        startActivityForResult(intent, FILTER_SETTINGS_REQUEST_CODE);
    }


    /**
     * @param view
     */
    public void onScribbleSwitch(View view) {
        if (mScribbleSwitch == null) {
            mScribbleSwitch = view.findViewById(R.id.scribble_switch);
        }
        if (mScribbleSwitch.isChecked()) {
            mOriginalImageViewFragment.turnScribbleOn();
        } else {
            showClearScribbleDialog(false);
        }
    }


    private void showClearScribbleDialog(final boolean onBackPressed) {
        AlertDialog.Builder alertDialog = Utility.generateBasicAlertDialog(this, "WARNING", R.string.scribble_switch_off_msg);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mOriginalImageViewFragment.clearScribble();
                if (onBackPressed) {
                    mIsFiltered = false;
                    PhotoFilteringActivity.super.onBackPressed();

                }

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

    public void onCancelClick(View view) {
        this.onBackPressed();
    }

    public void onContinueScribbleClick(View view) {

        mForegroundMask = new Mat();
        mBackgroundMask = new Mat();
        Scalar threshold = new Scalar(mScribbleThreshold);
        // threshold the filtered image (CoFed scribbled mask) to create the foreground mask
        Core.compare(mFilteredScribble, threshold, mForegroundMask, Core.CMP_GE);
        // compare foreground mask to 0 to get the background mask
        Core.compare(mFilteredScribble, threshold, mBackgroundMask, Core.CMP_LT);

        // convert masks to 32-bit float, 1/0 matrix
        mForegroundMask.convertTo(mForegroundMask, CvType.CV_32FC1, 1.0 / 255.0);
        mBackgroundMask.convertTo(mBackgroundMask, CvType.CV_32FC1, 1.0 / 255.0);

        mFilteredImage.release();
        mFilteredImage = new Mat(mImToFilter.size(), mImToFilter.type());

        new FilteringAsyncTask().execute(FilteringMode.SCRIBBLE_FOREGROUND_BACKGROUND);
    }


    @SuppressLint("StaticFieldLeak")
    private class FilteringAsyncTask extends AsyncTask<FilteringMode, String, Void> {

        private final String TAG = "FilteringAsyncTask";
        ProgressDialog mProgressDialog;
        FilteringMode scribbleMode;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(PhotoFilteringActivity.this);
            mProgressDialog.setTitle("Applying Co-Occurrence Filter");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(FilteringMode... fm) {

            scribbleMode = fm[0];

            try {
                do {
                    Log.i(TAG, "doInBackground: Started applying filter");
                    if (scribbleMode == FilteringMode.SCRIBBLE_INITIALIZATION) {
                        publishProgress("Processing scribble info...");
                        generateScribbleImage();
                    }
                    //todo - split parameters extraction according to filtering mode
                    // extract parmeters from params
                    int iterCnt = mPreset != null ? mPreset.getNumberOfIteration() : Utility.DEFAULT_NUMBER_OF_ITERATIONS;
                    double sigma = mPreset != null ? mPreset.getStatSigma() : Utility.DEFAULT_SIGMA;
                    int nBins = mPreset != null ? mPreset.getQuantization() : Utility.DEFAULT_QUNTIZATION_LEVEL;
                    int winSize = mPreset != null ? mPreset.getStatWindowSize() : Utility.DEFAULT_WINDOW_SIZE;

                    if (winSize % 2 == 0) {
                        winSize--;
                    }

                    if (mImToFilter.rows() != mFilteredImage.rows() || mImToFilter.cols() != mFilteredImage.cols()) {
                        Log.e(TAG, "doInBackground: imToProcess.size() != filteredImage.size()", new IllegalArgumentException("imToProcess.size() != filteredImage.size()"));
                    }


                    Stopwatch sw = Stopwatch.createUnstarted(); // stopwatch to measure times

                    if (scribbleMode == FilteringMode.SCRIBBLE_FOREGROUND_BACKGROUND) {
                        Mat fgPab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
                        Mat bgPab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
                        Mat fgPmi = new Mat(fgPab.size(), fgPab.type());
                        Mat bgPmi = new Mat(bgPab.size(), bgPab.type());

                        publishProgress("Collecting foreground statistics...");
                        sw.reset();
                        sw.start();
                        CoF.collectPab(mImToCollect, mForegroundMask, fgPab, nBins, winSize, sigma);
                        sw.stop();
                        Log.d(TAG, "doInBackground: collectPab (foreground) time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
                        CoF.pabToPmi(fgPab, fgPmi);


                        publishProgress("Collecting background statistics...");
                        sw.reset();
                        sw.start();
                        CoF.collectPab(mImToCollect, mBackgroundMask, bgPab, nBins, winSize, sigma);
                        sw.stop();
                        Log.d(TAG, "doInBackground: collectPab (background) time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
                        CoF.pabToPmi(bgPab, bgPmi);

                        Mat cpy = mImToFilter.clone();

                        publishProgress("Filtering...");
                        sw.reset();
                        sw.start();
                        for (int i = 0; i < 3; i++) {
                            CoF.FBCoFilter(cpy, mImToCollect, mFilteredImage, fgPmi, bgPmi, 15);
                            //CoF.coFilter(cpy, mImToCollect, mFilteredImage, fgPmi, 15, 2 * Math.sqrt(15) + 1);
                            mFilteredImage.copyTo(cpy);
                            System.gc();
                        }

                        sw.stop();
                        Log.d(TAG, "doInBackground: FBCoFilter time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

                        Utility.releaseMats(fgPab, bgPab, fgPmi, bgPmi, cpy);
                        break;

                    }
                    publishProgress("Quantizing...");
                    sw.reset();
                    sw.start();
                    CoF.quantize(mImToFilter, mImToCollect, nBins);
                    sw.stop();
                    Log.d(TAG, "doInBackground: quantize time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

                    Mat pab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
                    Mat pmi = new Mat(pab.size(), pab.type());

                    publishProgress("Collecting Statistics...");
                    sw.reset();
                    sw.start();
                    // collecting with the default all-ones mask
                    CoF.collectPab(mImToCollect, pab, nBins, winSize, sigma);
                    sw.stop();
                    Log.d(TAG, "doInBackground: collectPab time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
                    CoF.pabToPmi(pab, pmi);
                    pab.release();

                    Mat imToProcessCopy = (scribbleMode == FilteringMode.SCRIBBLE_INITIALIZATION) ? mScribbleImage.clone() : mImToFilter.clone();
                    System.gc();
                    sw.reset();
                    sw.start();
                    for (int i = 0; i < iterCnt; i++) {
                        Log.d(TAG, "doInBackground: cofilter iteration No. " + (i + 1) + "/" + iterCnt);
                        publishProgress("Filtering - Iteration No. " + (i + 1) + "/" + iterCnt);
                        CoF.coFilter(imToProcessCopy, mImToCollect, mFilteredImage, pmi, winSize, sigma);
                        mFilteredImage.copyTo(imToProcessCopy);
                        System.gc();
                    }
                    sw.stop();
                    Log.d(TAG, "applyCoF: coFilter time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

                    imToProcessCopy.release();
                    pmi.release();
                } while (false);

                if (scribbleMode != FilteringMode.SCRIBBLE_INITIALIZATION) {
                    // convert filterd image to uint8 type
                    mFilteredImage.convertTo(mFilteredImage, CvType.CV_8UC(mFilteredImage.channels()));
                    // save the filtered Mat into Bitmap
                    mFilteredBitmap = Bitmap.createBitmap(mFilteredImage.cols(), mFilteredImage.rows(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(mFilteredImage, mFilteredBitmap, true);
                }
                Log.i(TAG, "Filtering finished");

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mProgressDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Create the filtered image view.
            if (mFilteredImageViewFragment == null) {
                mFilteredImageViewFragment = new ImageViewFragment();
            }
            Fragment relevantFragment;
            String strToBackStack;

            if (scribbleMode == FilteringMode.SCRIBBLE_INITIALIZATION) {
                if (mFilteredScribble != null) {
                    mFilteredScribble.release();
                }
                mFilteredScribble = mFilteredImage.clone();
                relevantFragment = new ScribbleMaskThresholdFragment();
                strToBackStack = FROM_SCRIBBLE_TO_THRESHOLD;
            } else {
                mFilteredImageViewFragment.setImage(mFilteredBitmap);
                mIsFiltered = true;
                mIsShared = false;
                mSavedOnce = false;

                // Create the post filtering fragment
                relevantFragment = new PostFilteringButtonsFragment();
                strToBackStack = FROM_FILTERING_TO_RESULT;

            }

            // Replacing the in-filtering fragment of buttons with the post-filtering fragment of buttons.
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.filtering_activity_button_container, relevantFragment);
            transaction.replace(R.id.main_view_container, mFilteredImageViewFragment);
            transaction.addToBackStack(strToBackStack);
            transaction.commit();
            mProgressDialog.dismiss();
        }

    }



    /**
     * @param view
     */
    public void onApplyFilterClick(View view) {

        Log.i(TAG, "onApplyFilterClick: onClick event!");
        mIsFiltered = false;

        if (mOriginalBitmap == null) {
            Log.e(TAG, "onApplyFilterClick: mOriginalBitmap == null", new NullPointerException("mOriginalBitmap == null"));
        }

        Utility.releaseMats(mImToFilter, mImToCollect, mFilteredImage);

        mImToFilter = new Mat();
        Utils.bitmapToMat(mOriginalBitmap, mImToFilter);
        // the image loaded from the bitmap is RGBa - convert it to RGB
        Imgproc.cvtColor(mImToFilter, mImToFilter, Imgproc.COLOR_RGBA2RGB);
        mFilteredImage = new Mat(mImToFilter.size(), mImToFilter.type());

        // this matrix will hold the quantization mapping.
        // we assume there are no more than 256 bins, so we can use byte-typed matrix
        mImToCollect = new Mat(mImToFilter.size(), CvType.CV_8UC1);

        FilteringMode mode = mPreFilterButtonFragment.isScribbleOn() ?
                FilteringMode.SCRIBBLE_INITIALIZATION : FilteringMode.REGULAR_FILTERING;
        new FilteringAsyncTask().execute(mode);
    }

    private void generateScribbleImage() {

        Path scribblePath = mOriginalImageViewFragment.getScribblePath();
        int height = mOriginalImageViewFragment.getImageViewHeight();
        int width = mOriginalImageViewFragment.getImageViewWidth();
        Mat scribbleMat = new Mat(new Size(width, height), CvType.CV_8UC1);

        pathToMat(scribblePath, scribbleMat, height, width);

        Utility.releaseMats(mScribbleImage);

        mScribbleImage = new Mat();
        // resize the binary image to the original image size
        Imgproc.resize(scribbleMat, mScribbleImage, mImToFilter.size());

        // perform dilation in order to thicken the white scribble lines
        Mat SE = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, Utility.SCRIBBLE_DILATION_WINDOW_SIZE);
        Imgproc.dilate(mScribbleImage, mScribbleImage, SE, new Point(-1, -1), Utility.SCRIBBLE_DILATION_ITERATIONS_DEFAULT);

        scribbleMat.release();
        SE.release();

    }

    void onChangeScribbleThreshold(int threshold) {
        Mat mask = new Mat();
        Mat tmp = new Mat();
        Core.compare(mFilteredScribble, new Scalar(threshold), mask, Core.CMP_GE);
        mImToFilter.copyTo(tmp, mask);

        double alpha = 0.7;
        Core.addWeighted(tmp, alpha, mImToFilter, 1 - alpha, 0, tmp);

        mFilteredBitmap = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(tmp, mFilteredBitmap, true);
        // Create the filtered image view.
        if (mFilteredImageViewFragment == null) {
            mFilteredImageViewFragment = new ImageViewFragment();
        }
        mFilteredImageViewFragment.setImage(mFilteredBitmap);

        mask.release();
        tmp.release();
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
            numPoints = (int) (contourLength / 0.01) + 1;
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
     * @param view
     */
    public void onHelpClick(View view) {
        //TODO - show a tutorial of the application that should be used when the user first reache a new screen. Note that the tutorial is screen-dependent
    }


    /**
     * @param view
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
     * @param view
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

    @Override
    public void onBackPressed() {
        // if we're in scribble mode and the image is not filtered - warn the user
        if (mScribbleSwitch != null && mScribbleSwitch.isChecked() && !mIsFiltered) {
            showClearScribbleDialog(true);
            return;
        }
        // if the image is not filtered, or already saved or shared - go back gracefully
        if (!mIsFiltered || mSavedOnce || mIsShared) {
            super.onBackPressed();
            return;
        }

        // here the image is filtered and wasn't saved or shared - warn the user:
        AlertDialog.Builder alertDialog = Utility.generateBasicAlertDialog(this, "WARNING", R.string.back_pressed_msg);
        // Add the buttons
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mIsFiltered = false;
                //mOriginalImageViewFragment.setImage(mOriginalBitmap);
                PhotoFilteringActivity.super.onBackPressed();
            }
        });

        alertDialog.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: started");
        Utility.releaseMats(mImToFilter, mFilteredImage, mScribbleImage, mImToCollect, mForegroundMask, mBackgroundMask, mFilteredScribble);
        // Clear the current preset preference file
        currentPresetFile.edit().clear().apply();
        super.onDestroy();

    }
}
