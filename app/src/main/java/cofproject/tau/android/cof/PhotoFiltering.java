package cofproject.tau.android.cof;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */

public class PhotoFiltering extends AppCompatActivity {
    private static final String TAG = "PhotoFiltering";
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;

    private Bitmap bitmap;
    private ImageView newPhotoView;
    private File myFilesDir;


    private boolean verifyValues()
    {
        String sigma = ((EditText)findViewById(R.id.spatial_sigma_input)).getText().toString();
        String height = ((EditText)findViewById(R.id.window_height_input)).getText().toString();
        String width = ((EditText)findViewById(R.id.window_width_input)).getText().toString();
        //TODO - add pop up with information
        if (width.isEmpty())
        {
            return false;
        }
        if(height.isEmpty())
        {
            return false;
        }
        if(sigma.isEmpty())
        {
            return false;
        }
        return true;
    }

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_filtering);

        // Create a new folder for images in the applications directory
        myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/cofproject.tau.android.cof/files");
        myFilesDir.mkdirs();

        newPhotoView = (ImageView) findViewById(R.id.new_photo_view);
        Bundle extras = getIntent().getExtras();
        if (extras == null)
        {
            return;
        }
        Intent intent = new Intent();
        if (extras.getBoolean("capture"))
        {
            intent.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(myFilesDir.toString()+"/temp.jpg")));
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
        } else
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
                if (bitmap != null)
                {
                    bitmap.recycle();
                }
                stream = getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);
                newPhotoView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
            } finally
            {
                if (stream != null)
                {
                    try
                    {
                        stream.close();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        else if(resultCode == 0)
        {
            finish();
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
    public void onApplyFilterClick(View view)
    {
        //TODO - do some magic here with

        double sigma = Double.parseDouble(((EditText)findViewById(R.id.spatial_sigma_input)).getText().toString());
        int height = Integer.parseInt(((EditText)findViewById(R.id.window_height_input)).getText().toString());
        int width = Integer.parseInt(((EditText)findViewById(R.id.window_width_input)).getText().toString());
        CoFilter coFilter = new CoFilter(sigma, height, width);
        coFilter.Apply(findViewById(R.id.new_photo_view));
        Intent intent = new Intent(this, FilteringResults.class);
        //TODO - pass filtered image (I have no idea yet how. intent.putExtra();?
        startActivity(intent);
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
    public void onHelpClick(View view)
    {

    }


}
