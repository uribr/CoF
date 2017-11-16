package cofproject.tau.android.cof;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;



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
    private static final double SIGMA_LIMIT = 10;
    private static final int ITER_LIMIT = 10;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;

    private int counter;
    private int imgHeight, imgWidth;
    private boolean filteringDone;
    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;
    private ImageView newPhotoView;
    private File tempImageFile;
    private Fragment mPreFilterButtonFragment;
    private Fragment mPostFilterButtonFragment;
    private EditText mSigmaET;
    private EditText mIterET;
    private EditText mHeighET;
    private EditText mWidthET;

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

    private boolean verifyValues()
    {
        boolean bool_sigma = mSigmaET.getError() == null && !mSigmaET.getText().toString().isEmpty();
        boolean bool_iter = mIterET.getError() == null && !mIterET.getText().toString().isEmpty();
        boolean bool_height = mHeighET.getError() == null && !mHeighET.getText().toString().isEmpty();
        boolean bool_width = mWidthET.getError() == null && !mWidthET.getText().toString().isEmpty();

        return bool_height && bool_width && bool_iter && bool_sigma;
    }

    private void addOnTextChangeListeners()
    {
        String negMsg = "must be non-negative.";
        String posMsg = "must be positive.";
        String aboveLimMsg = "must be less then ";
        String natMsg = "must be a natural number";

        mSigmaET.addTextChangedListener(new DoubleParameterWatcher(SIGMA_LIMIT, negMsg, aboveLimMsg + Double.toString(SIGMA_LIMIT), " ", "\u03C3 ", mSigmaET));
        mIterET.addTextChangedListener(new IntegerParameterWatcher(ITER_LIMIT, posMsg, aboveLimMsg + Integer.toString(ITER_LIMIT), natMsg,"The number of iterations ", mIterET));

        Integer height = originalBitmap.getHeight();
        mHeighET.addTextChangedListener(new IntegerParameterWatcher(height, negMsg, aboveLimMsg + height.toString(), natMsg, "The height ", mHeighET));

        Integer width = originalBitmap.getWidth();
        mWidthET.addTextChangedListener(new IntegerParameterWatcher(width, negMsg, aboveLimMsg + width.toString(), natMsg, "The width ", mWidthET));
    }


    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_filtering);

        mSigmaET = (EditText)findViewById(R.id.spatial_sigma_input);
        mHeighET = (EditText)findViewById(R.id.window_height_input);
        mWidthET = (EditText)findViewById(R.id.window_width_input);
        mIterET = (EditText)findViewById(R.id.number_of_iterations);

        // Create filtering related buttons fragment and
        mPreFilterButtonFragment = new FilteringButtonsFragment();
        mPreFilterButtonFragment.setArguments(getIntent().getExtras());

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
        newPhotoView = (ImageView) findViewById(R.id.new_photo_view);
        Bundle extras = getIntent().getExtras();
        if (extras == null) { return; }

        // Get counter from intent. The counter is used for naming files
        counter = extras.getInt("counter");

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
        // super.onActivityResult(requestCode, resultCode, data);
        InputStream stream = null;
        if ((requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_CAPTURE_REQUEST_CODE) && resultCode == Activity.RESULT_OK)
        {
            try
            {
                if (originalBitmap != null) { originalBitmap.recycle(); }
                if(data.getData() != null)
                {
                    stream = getContentResolver().openInputStream(data.getData());
                    originalBitmap = BitmapFactory.decodeStream(stream);
                    imgHeight = originalBitmap.getHeight();
                    imgWidth = originalBitmap.getWidth();
                    newPhotoView.setImageBitmap(originalBitmap);

                    // Add listeners to the EditText widgets to
                    // detect changes in the text.
                    addOnTextChangeListeners();
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

    /**
     *
     * @param view
     */
    public void onScribbleOn(View view)
    {

    }

    /**
     *
     * @param view
     */
    public void onApplyFilterClick(View view)
    {
        // Based on code from: https://stackoverflow.com/questions/43513919/android-alert-dialog-with-one-two-and-three-buttons/43513920#43513920
        // Verify that all of the parameters are within the proper ranges.
        if (!verifyValues())
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
            double sigma = Double.parseDouble(mSigmaET.getText().toString());
            int height = Integer.parseInt(mHeighET.getText().toString());
            int width = Integer.parseInt(mWidthET.getText().toString());
            int iter = Integer.parseInt(mIterET.getText().toString());
            CoFilter coFilter = new CoFilter(sigma, height, width);
            filteredBitmap = coFilter.Apply(originalBitmap, iter);

            newPhotoView.setImageBitmap(filteredBitmap);

            // Create the post filtering fragment of buttons if it is the first time
            if (mPostFilterButtonFragment == null)
            {
                mPostFilterButtonFragment = new ResultButtonsFragment();
                mPostFilterButtonFragment.setArguments(getIntent().getExtras());
            }

            // Removing the pre-filtering fragment of buttons and adding the post-filtering fragment of buttons
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.filtering_activity_button_container, mPostFilterButtonFragment);

            // Store the replaced fragment in the back stack for
            // fast retrieval in case the user navigates back to it.
            // transaction.addToBackStack(null);
            transaction.commit();
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
        saveImage(filteredBitmap, "FLTRD_IMG_" + Integer.toString(++counter));
        setResult(Activity.RESULT_OK, new Intent().putExtra("filteringDone", filteringDone));
        finish();
    }


    /**
     *
     * @param view
     */
    public void onShareClick(View view)
    {
        //TODO - implement this
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_SEND);
//
//        saveImage(filteredBitmap, "TMP_SHARABLE_IMG");
//
//
//        intent.setType("image/*");
//        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse());
//        startActivity(Intent.createChooser(intent, "Share"));


    }

    /**
     *
     * @param view
     */
    public void onDiscardClick(View view)
    {
        filteringDone = false;

        // Bring back the original image.
        newPhotoView.setImageBitmap(originalBitmap);

        // Swap between the fragments again.
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.filtering_activity_button_container ,mPreFilterButtonFragment);
        transaction.commit();
    }




}
