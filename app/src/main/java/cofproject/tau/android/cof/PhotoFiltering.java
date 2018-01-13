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
import android.content.SharedPreferences;
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
import android.view.LayoutInflater;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */
//TODO - Replace reconstructing preset with modifying for better performance.
public class PhotoFiltering extends AppCompatActivity implements ParametersFragment.OnCompleteListener
{
    private static final String TAG = "PhotoFiltering";
    private static final String FROM_ORIGINAL_TO_FILTERING = "from original image to filtering";
    private static final String FROM_FILTERING_TO_RESULT = "from filtering to result";
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;
    // TODO - Are the four following constants reasonable?
    private static final int DEFAULT_WINDOW_SIZE = 16;
    private static final int DEFAULT_NUMBER_OF_ITERATIONS = 1;
    private static final double DEFAULT_SIGMA = 1;

    private int mImgHeight, mImgWidth;
    private boolean mfilteringDone;
    private boolean isLandscape;
    private boolean mSavedOnce;
    private Bitmap mOriginalBitmap;
    private Bitmap mFilteredBitmap;
    private PreFilteringButtonsFragment mPreFilterButtonFragment;
    private ImageViewFragment mOriginalImageViewFragment;
    private ImageViewFragment mFilteredImageViewFragment;
    private InFilterButtonsFragment mInFilterButtonFragment;
    private ParametersFragment mFilteringParametersFragment;
    private PostFilteringButtonsFragment mPostFilterButtonFragment;
    private SharedPreferences mPresetPref;
    private Preset mPreset;
    private Uri mURI;
    private File mFile;
    private boolean mIsFiltered = false;
    private boolean mIsShared = false;

    private Mat mImToProcess;
    private Mat mFilteredImage;
    private Mat mMaskToCollect;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }


    private List<String> getPresetNames()
    {
        Vector<String> list = new Vector<>();
        Map<String, ?> map = mPresetPref.getAll();
        list.add(getString(R.string.DefaultPresetName));
        for (Map.Entry<String, ?> entry : map.entrySet())
        {
            if(entry.getKey().equals(getString(R.string.DefaultPresetName))) { continue; }
            list.add(entry.getKey());
        }
        return list;
    }

    private void createPreset(String name,boolean relative, boolean newDefault, boolean createDefault)
    {
        mPreset = new Preset(newDefault || createDefault ? getString(R.string.DefaultPresetName) : name,
                createDefault ? DEFAULT_SIGMA : mFilteringParametersFragment.getSigma(),
                createDefault ? DEFAULT_NUMBER_OF_ITERATIONS : mFilteringParametersFragment.getIter(),
                createDefault ? DEFAULT_WINDOW_SIZE : mFilteringParametersFragment.getHeight(), mOriginalBitmap.getHeight(),
                relative);
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

    private void openPresetConfigFile()
    {
        mPresetPref = this.getSharedPreferences(getString(R.string.PresetsConfigFileName), Context.MODE_PRIVATE);
    }


    private void loadDefaultPreset()
    {
       String name = getString(R.string.DefaultPresetName);
       String params = mPresetPref.getString(name, "");
       if(params.isEmpty())
       {
            // No default preset found, generating an hardcoded default preset
            createPreset(name,false, true, true);
            if(mPreset.isValid())
            {
                mPreset.store(mPresetPref);
                Toast.makeText(getApplicationContext(), "Default preset created and loaded.", Toast.LENGTH_SHORT).show();
                if ((Math.min(mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight()) < mPreset.getWindowSize()))
                {
                    Toast.makeText(getApplicationContext(), "Default window size is too large for the selected image.\nPlease choose valid parameters.", Toast.LENGTH_SHORT).show();
                }
            }
       }
       else { mPreset = new Preset(name, params); }
    }

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isLandscape = true;
            mFilteringParametersFragment = new ParametersFragment();
        }
        else { setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); }
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
        if (extras == null) { return; }






        Intent intent = new Intent();

        if (extras.getBoolean(getString(R.string.Capture)))
        {
            mFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "tmp_img_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            mURI = Uri.fromFile(mFile);
            intent.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
        }
        else
        {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_an_app)), GALLERY_REQUEST_CODE);
        }

    }


    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_CAPTURE_REQUEST_CODE) && resultCode == Activity.RESULT_OK)
        {
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

                    if(isLandscape)
                    {
                        onChangeParameters(new View(this));
                    }

                    // Open the configuration file that contains the presets
                    openPresetConfigFile();

                    // Load the default preset
                    loadDefaultPreset();
                }
            }
            catch (Exception e) { e.printStackTrace(); }
        }

        else { finish(); }
    }


    public void onComplete(Spinner spinner)
    {
        // Set the dimensions of the image
        mFilteringParametersFragment.setDimensionsLimit(Math.min(mImgHeight, mImgWidth));
        mFilteringParametersFragment.applyLimiters();
        mFilteringParametersFragment.applyPreset(mPreset);
        mFilteringParametersFragment.setPresetList(getPresetNames());
        List<String> array = new ArrayList<>(getPresetNames());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, array);
        spinner.setAdapter(adapter);

    }

    public Preset getPreset(String name)
    {
        String params = mPresetPref.getString(name, "");
        mPreset = new Preset(name, params);
        return mPreset;
    }

    public Preset getCurrentPreset()
    {
        return mPreset;
    }

    public boolean getFilteringDone() { return this.mfilteringDone; }


    public void onChangeParameters(View view)
    {
        // Add listeners to the EditText widgets to
        // detect changes in the text.
        if (mFilteringParametersFragment == null)
        {
            mFilteringParametersFragment = new ParametersFragment();

        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (isLandscape) { transaction.replace(R.id.parameter_view_container, mFilteringParametersFragment); }
        else { transaction.replace(R.id.main_view_container, mFilteringParametersFragment); }

        if (!isLandscape)
        {
            if (mInFilterButtonFragment == null) { mInFilterButtonFragment = new InFilterButtonsFragment(); }
            transaction.replace(R.id.filtering_activity_button_container,mInFilterButtonFragment);
            transaction.addToBackStack(FROM_ORIGINAL_TO_FILTERING);
            transaction.commit();
        }

    }

    /**
     *
     * @param view
     */
    public void onScribbleOn(View view)
    {
        Switch sw = view.findViewById(R.id.scribble_switch);
        if(!sw.isChecked())
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
            { @Override public void onClick(DialogInterface dialog, int which)
            {
                Switch scribble = (Switch)findViewById(R.id.scribble_switch);
                scribble.setChecked(true);
            }});
            builder.setCancelable(false);
            builder.create().show();
        }
        else
        {
            mOriginalImageViewFragment.turnScribbleOn();
        }
    }

    private void startFiltering() {
        try
        {
            //todo - remove this!!!!!!
            try {
                mImToProcess.release();
                mImToProcess = Utils.loadResource(this, R.drawable.olive, Imgcodecs.IMREAD_COLOR); // loading as BGR!!!
                Imgproc.cvtColor(mImToProcess, mImToProcess, Imgproc.COLOR_BGR2RGB);
            } catch (IOException e) {
                e.printStackTrace();
            }

            final ProgressDialog ringProgressDialog = ProgressDialog.show(this, "Applying Co-Occurrence Filter", "Please wait...", true);
            ringProgressDialog.setCancelable(false);
            ringProgressDialog.setCanceledOnTouchOutside(false);
            Thread filterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String TAG = "launcherDialogTag";
                    try {

                        // prepare a new Mat object for the filtered image
                        if (mFilteredImage != null) {
                            mFilteredImage.release();
                        }
                        mFilteredImage = new Mat(mImToProcess.size(), mImToProcess.type());

                        // aplly the filter!
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
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }


                    ringProgressDialog.dismiss();

                    // Create the post filtering fragment of buttons if it is the first time
                    if (mPostFilterButtonFragment == null) {
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
    public void onSavePresetClick(View view)
    {
        // Based on code from: https://www.mkyong.com/android/android-prompt-user-input-dialog-example/
        // setup the alert builder
        LayoutInflater li = LayoutInflater.from(this);
        final View promptsView = li.inflate(R.layout.prompt, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(promptsView);

        // Add a listener to the user input to check if the name is valid
        // and display an error to the user if it isn't valid.
        final EditText userInput = (EditText) promptsView.findViewById(R.id.savePresetPromptUserInput);
        userInput.addTextChangedListener(new StringNameWatcher(userInput));
        final CheckBox relativeCheckBox = promptsView.findViewById(R.id.relativePresetCheckBox);
        final CheckBox setAsDefaultCheckBox = promptsView.findViewById(R.id.SetAsDefaultCheckBox);
        // add a button
        builder.setPositiveButton(getString(R.string.okMsg), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

                String name = userInput.getText().toString();
                // Create the preset object
                createPreset(name, relativeCheckBox.isChecked(), setAsDefaultCheckBox.isChecked(), false);

                // if the input is valid (e.g no error is being displayed)
                // we attempt to store the preset in the configuration file
                // and announce the success or failure of the saving.
                if(userInput.getError() == null && mPreset.store(mPresetPref))
                {
                    mFilteringParametersFragment.onStored(name);
                    Toast.makeText(getApplicationContext(), "Preset Saved", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Invalid name, preset saving failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog,int id) { dialog.cancel(); }
        });
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     *
     * @param view
     */
    public void onHelpClick(View view)
    {
        //TODO - show a tutorial of the application that should be used when the user first reache a new screen. Note that the tutorial is screen-dependent
    }


    /**
     *
     * @param view
     */
    public void onSaveResultClick(View view)
    {
        Log.i(TAG, "onSaveResultClick:  onClick event");
        if(!mSavedOnce) { internalSaveOperation(); }
    }


    /**
     *
     * @param view
     */
    public void onShareClick(View view)
    {
        // CREDIT: https://goo.gl/wQvQeh
        Log.i(TAG, "onShareClick: onClick event");

        // save bitmap to cache directory
        try {

            File cachePath = new File(this.getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.jpg"); // overwrites this image every time
            mFilteredBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

        } catch (IOException e) {
            Log.e(TAG, "onShareClick: IOExceotion - " + e.getMessage(), e );
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
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: started");
        if (mImToProcess != null) {
            mImToProcess.release();
        }
        if (mFilteredImage != null) {
            mFilteredImage.release();
        }
        if (mMaskToCollect != null) {
            mMaskToCollect.release();
        }

        super.onDestroy();

    }
}
