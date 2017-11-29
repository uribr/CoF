package cofproject.tau.android.cof;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Date;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */
//TODO - Replace reconstructing preset with modifying for better performance.
public class PhotoFiltering extends AppCompatActivity {
    private static final String TAG = "PhotoFiltering";
    private static final String FROM_ORIGINAL_TO_FILTERING = "from original image to filtering";
    private static final String FROM_FILTERING_TO_RESULT = "from filtering to result";
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;
    // TODO - Are the four following constants reasonable?
    private static final int DEFAULT_WINDOW_WIDTH = 16;
    private static final int DEFAULT_WINDOW_HEIGHT = 16;
    private static final int DEFAULT_NUMBER_OF_ITERATIONS = 1;
    private static final double DEFAULT_SIGMA = 1;

    private int imgHeight, imgWidth;
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

    private static void addImageToGallery(final String filePath, final Context context) {

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private boolean isNameValid(String str)
    {
        boolean atLeastOneChar = false;
        CharacterIterator cI = new StringCharacterIterator(str);
        for (char c = cI.first(); c != CharacterIterator.DONE; c = cI.next())
        {
            if(Character.isAlphabetic(c)) { atLeastOneChar = true; }
            else if(!Character.isDigit(c)) { return false; }
        }
        return atLeastOneChar;
    }



    private void createPreset(String name,boolean relative, boolean newDefault, boolean createDefault)
    {
        mPreset = new Preset(newDefault || createDefault ? getString(R.string.DefaultPresetName) : name,
                createDefault ? DEFAULT_SIGMA : mFilteringParametersFragment.getSigma(),
                createDefault ? DEFAULT_NUMBER_OF_ITERATIONS : mFilteringParametersFragment.getIter(),
                createDefault ? DEFAULT_WINDOW_WIDTH : mFilteringParametersFragment.getWidth(), mOriginalBitmap.getWidth(),
                createDefault ? DEFAULT_WINDOW_HEIGHT : mFilteringParametersFragment.getHeight(), mOriginalBitmap.getHeight(),
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

    private void loadDefaultPreset()
    {
        mPresetPref = this.getSharedPreferences(getString(R.string.PresetsConfigFileName), Context.MODE_PRIVATE);
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
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Default window size is too large for the selected image.\nPlease choose valid parameters.", Toast.LENGTH_SHORT).show();
                return;
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
        //tempImageFile = new File(getFilesDir().getAbsolutePath() + "/Android/data/cofproject.tau.android.cof/temp.jpg");

        // Create and send a camera/gallery intent
        Bundle extras = getIntent().getExtras();
        if (extras == null) { return; }

        // Get counter from intent. The counter is used for naming files

        Intent intent = new Intent();

        if (extras.getBoolean(getString(R.string.Capture)))
        {
            intent.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
        }
        else
        {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
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
        InputStream stream = null;
        if ((requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_CAPTURE_REQUEST_CODE) && resultCode == Activity.RESULT_OK)
        {
            try
            {
                if (mOriginalBitmap != null) { mOriginalBitmap.recycle(); }
                if(data.getData() != null)
                {
                    // store image
                    stream = getContentResolver().openInputStream(data.getData());
                    mOriginalBitmap = BitmapFactory.decodeStream(stream);
                    imgHeight = mOriginalBitmap.getHeight();
                    imgWidth = mOriginalBitmap.getWidth();

                    // Initialize image view fragment that will hold the image.
                    if(mOriginalImageViewFragment == null) {mOriginalImageViewFragment = new ImageViewFragment();}
                    // Add the image fragment to the container.
                    getFragmentManager().beginTransaction().add(R.id.main_view_container, mOriginalImageViewFragment).commit();
                    mOriginalImageViewFragment.setImage(mOriginalBitmap.copy(mOriginalBitmap.getConfig(), false));
                    // mOriginalImageViewFragment.setImage(data.getData());

                    if(isLandscape)
                    {
                        onChangeParameters(new View(this));
                    }



                }
            }
            catch (FileNotFoundException e) { e.printStackTrace(); }

            finally
            {
                if (stream != null)
                {
                    try { stream.close(); }
                    catch (IOException e) { e.printStackTrace(); }
                }
            }
        }

        else { finish(); }
    }


    public boolean getFilteringDone() { return this.mfilteringDone; }


    public void onChangeParameters(View view)
    {
        //TODO - store parameters

        // Add listeners to the EditText widgets to
        // detect changes in the text.
        if(mFilteringParametersFragment == null)
        {
            // Load the default preset
            loadDefaultPreset();
            mFilteringParametersFragment = new ParametersFragment();
            mFilteringParametersFragment.setDimensionsLimit(imgHeight, imgWidth);
            mFilteringParametersFragment.applyPreset(mPreset);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if(isLandscape) { transaction.replace(R.id.parameter_view_container, mFilteringParametersFragment); }
        else { transaction.replace(R.id.main_view_container, mFilteringParametersFragment); }

        if(!isLandscape)
        {
            if(mInFilterButtonFragment == null) { mInFilterButtonFragment = new InFilterButtonsFragment(); }
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
            builder.setTitle("Error");
            builder.setMessage("This action will delete all scribble points created so far.\nAre you sure you want to continue?\n");
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //TODO Remove all scribble
                }
            });
            builder.setNegativeButton("no", new DialogInterface.OnClickListener()
            { @Override public void onClick(DialogInterface dialog, int which)
            {
                Switch scribble = (Switch)findViewById(R.id.scribble_switch);
                scribble.setChecked(true);
            }});

            builder.create().show();

        }
    }

    /**
     *
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

        if (mOriginalBitmap != null)
        {
            // Create a CoF object with the specified parameters and apply it.
            double sigma = mFilteringParametersFragment.getSigma();
            int height = mFilteringParametersFragment.getHeight();
            int width = mFilteringParametersFragment.getWidth();
            int iter = mFilteringParametersFragment.getIter();
            CoFilter coFilter = new CoFilter(sigma, height, width);
            mFilteredBitmap = coFilter.Apply(mOriginalBitmap, iter);


            // Create the post filtering fragment of buttons if it is the first time
            if (mPostFilterButtonFragment == null) {mPostFilterButtonFragment = new PostFilteringButtonsFragment();}
            // Create the filtered image view.
            if(mFilteredImageViewFragment == null) {mFilteredImageViewFragment = new ImageViewFragment(); }

            // Replacing the in-filtering fragment of buttons with the post-filtering fragment of buttons.
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.filtering_activity_button_container, mPostFilterButtonFragment);
            transaction.replace(R.id.main_view_container, mFilteredImageViewFragment);
            transaction.addToBackStack(FROM_FILTERING_TO_RESULT);
            transaction.commit();

            mFilteredImageViewFragment.setImage(mFilteredBitmap);
            mfilteringDone = true;
            mSavedOnce = false;
        }
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
    public void onLoadPresetClick(View view)
    {
        String name = ((Spinner)findViewById(R.id.presetSpinner)).getSelectedItem().toString();
        if(isNameValid(name))
        {
            mPreset = new Preset(name, mPresetPref.getString(name, ""));
            if(mPreset.isValid())
            {
                Toast.makeText(getApplicationContext(), "Preset Loaded", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Invalid preset, modify preset to make it valid before applying it", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Invalid name, preset loading failed.", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     *
     * @param view
     */
    public void onHelpClick(View view)
    {
        //TODO - show a pop-up dialog with explanation about the utilization of the application.
    }

    /**
     * The method checks whether the application has permission to
     * access the external storage and if we don't an asynchronous
     * permission request is made.
     */
    public void requestExternalStoragePermission()
    {
        boolean readExternalStoragePermissionCheck = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED);
        if(!readExternalStoragePermissionCheck)
        {
            // Request permission to read and write to external storage, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MainActivity.APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE);
        }
        // The application has permissions thus we call the internal saving operation
        else if(!mSavedOnce) { internalSaveOperation(); }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        if (requestCode == MainActivity.APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Permission was granted, save image
                internalSaveOperation();
            }
            // Permission denied, do nothing.
        }
    }

    /**
     *
     * @param view
     */
    public void onSaveResultClick(View view)
    {
        Log.i(TAG, "onSaveResultClick:  onClick event");
        requestExternalStoragePermission();
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
            startActivity(Intent.createChooser(shareIntent, "Choose an app"));
        }
    }





}
