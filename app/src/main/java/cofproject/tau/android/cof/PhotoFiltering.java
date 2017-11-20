package cofproject.tau.android.cof;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Switch;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */

public class PhotoFiltering extends AppCompatActivity {
    private static final String TAG = "PhotoFiltering";
    private static final String FROM_ORIGINAL_TO_FILTERING = "from original image to filtering";
    private static final String FROM_FILTERING_TO_RESULT = "from filtering to result";
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;
    private int imgHeight, imgWidth;
    private boolean filteringDone;
    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;
    private PreFilteringButtonsFragment mPreFilterButtonFragment;
    private ImageViewFragment mOriginalImageViewFragment;
    private ImageViewFragment mFilteredImageViewFragment;
    private InFilterButtonsFragment mInFilterButtonFragment;
    private ParametersFragment mFilteringParametersFragment;
    private PostFilteringButtonsFragment mPostFilterButtonFragment;


    public String mCurrentPhotoPath;

    //TODO - need to test this method.
    /**
     *
     * @param finalBitmap
     * @param image_name
     */
    private void saveImage(Bitmap finalBitmap, String image_name)
    {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname =  image_name+ ".jpg";
        File file = new File(myDir, fname);
       // if (file.exists()) { file.delete(); }

        try
        {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }



    private void waitForFragmentToResume(Fragment frag)
    {
        // TODO - make this into a multi-threaded thingy
        while(!frag.isResumed())
        {
            try
            {
                wait(200);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }


    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_filtering);

        // Create filtering related buttons fragment and
        mPreFilterButtonFragment = new PreFilteringButtonsFragment();

        //TODO - add proper back support for fragments on the back stack
        // Add proper back behaviour for fragments
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

        if (extras.getBoolean("capture"))
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
        //TODO - add setting/loading default parameters
        // super.onActivityResult(requestCode, resultCode, data);
        InputStream stream = null;
        if ((requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_CAPTURE_REQUEST_CODE) && resultCode == Activity.RESULT_OK)
        {
            try
            {
                if (originalBitmap != null) { originalBitmap.recycle(); }
                if(data.getData() != null)
                {
                    // Store image
                    stream = getContentResolver().openInputStream(data.getData());
                    originalBitmap = BitmapFactory.decodeStream(stream);
                    imgHeight = originalBitmap.getHeight();
                    imgWidth = originalBitmap.getWidth();

                    // Initialize image view fragment that will hold the image.
                    if(mOriginalImageViewFragment == null) {mOriginalImageViewFragment = new ImageViewFragment();}
                    // Add the image fragment to the container.
                    getFragmentManager().beginTransaction().add(R.id.main_view_container, mOriginalImageViewFragment).commit();
                    mOriginalImageViewFragment.setImage(originalBitmap.copy(originalBitmap.getConfig(), false));
                    // mOriginalImageViewFragment.setImage(data.getData());
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


    public boolean getFilteringDone() { return this.filteringDone; }

    public void onChangeParameters(View view)
    {
        //TODO - store parameters
        // Add listeners to the EditText widgets to
        // detect changes in the text.
        if(mFilteringParametersFragment == null)
        {
            mFilteringParametersFragment = new ParametersFragment();
            mFilteringParametersFragment.setDimensions(imgHeight, imgWidth);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_view_container, mFilteringParametersFragment);

        if(mInFilterButtonFragment == null) { mInFilterButtonFragment = new InFilterButtonsFragment(); }
        transaction.replace(R.id.filtering_activity_button_container,mInFilterButtonFragment);
        transaction.addToBackStack(FROM_ORIGINAL_TO_FILTERING);
        transaction.commit();

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
        if (!mFilteringParametersFragment.verifyValues())
        {
            // setup the alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error");
            builder.setMessage("Please enter the filters parameters.\n\nIf you are unsure what to enter press the help button.");

            // add a button
            builder.setPositiveButton("OK", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

        if (originalBitmap != null)
        {
            // Create a CoF object with the specified parameters and apply it.
            double sigma = mFilteringParametersFragment.getSigma();
            int height = mFilteringParametersFragment.getHeight();
            int width = mFilteringParametersFragment.getWidth();
            int iter = mFilteringParametersFragment.getIter();
            CoFilter coFilter = new CoFilter(sigma, height, width);
            filteredBitmap = coFilter.Apply(originalBitmap, iter);


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

            mFilteredImageViewFragment.setImage(filteredBitmap);
            filteringDone = true;
        }
    }

    /**
     *
     * @param view
     */
    public void onSavePresetClick(View view)
    {
        //TODO - write new preset to configuration file if it exits, if it doesn't then create it!
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
     *
     * @param view
     */
    public void onSaveResultClick(View view)
    {
        //todo
    }


    /**
     *
     * @param view
     */
    public void onShareClick(View view)
    {
        //TODO - implement this
    }





}
