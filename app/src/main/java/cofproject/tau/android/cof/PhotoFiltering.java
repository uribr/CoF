package cofproject.tau.android.cof;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */

public class PhotoFiltering extends AppCompatActivity {
    private static final String TAG = "PhotoFiltering";
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;

    private int counter;
    private boolean filteringDone;
    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;
    private ImageView newPhotoView;
    private File tempImageFile;
    private Fragment mPreFilterButtonFragment;
    private Fragment mPostFilterButtonFragment;

    public String mCurrentPhotoPath;

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
        if (file.exists()) { file.delete(); }

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
        StringBuilder errorMessageBuilder;
        String sigma = ((EditText)findViewById(R.id.spatial_sigma_input)).getText().toString();
        String height = ((EditText)findViewById(R.id.window_height_input)).getText().toString();
        String width = ((EditText)findViewById(R.id.window_width_input)).getText().toString();
        //TODO - add pop up with information
        if (!width.isEmpty())
        {
            if(!height.isEmpty())
            {
                if(!sigma.isEmpty())
                {

                }
            }
        }


        return false;
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
        if (extras == null)
        {
            return;
        }

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
        super.onActivityResult(requestCode, resultCode, data);
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
                    newPhotoView.setImageBitmap(originalBitmap);
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

    public boolean getFilteringDone()
    {
        return this.filteringDone;
    }

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
        // Verify that all of the parameters are within the proper ranges.
        verifyValues();

        // Create a CoF object with the specified parameters and apply it.
        double sigma = Double.parseDouble(((EditText)findViewById(R.id.spatial_sigma_input)).getText().toString());
        int height = Integer.parseInt(((EditText)findViewById(R.id.window_height_input)).getText().toString());
        int width = Integer.parseInt(((EditText)findViewById(R.id.window_width_input)).getText().toString());
        CoFilter coFilter = new CoFilter(sigma, height, width);
        filteredBitmap = coFilter.Apply(originalBitmap);

        newPhotoView.setImageBitmap(filteredBitmap);

        // Create the post filtering fragment of buttons if it is the first time
        if(mPostFilterButtonFragment == null)
        {
            mPostFilterButtonFragment = new ResultButtonsFragment();
            mPostFilterButtonFragment.setArguments(getIntent().getExtras());
        }

        // Removing the pre-filtering fragment of buttons and adding the post-filtering fragment of buttons
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.filtering_activity_button_container ,mPostFilterButtonFragment);

        // Store the replaced fragment in the back stack for
        // fast retrieval in case the user navigates back to it.
        // transaction.addToBackStack(null);
        transaction.commit();
        filteringDone = true;

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
    }

    /**
     *
     * @param view
     */
    public void onShareClick(View view)
    {
        // TODO - implement share.
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
