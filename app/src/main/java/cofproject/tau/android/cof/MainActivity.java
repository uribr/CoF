package cofproject.tau.android.cof;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
    // Private members and methods
    private SharedPreferences sharedPreferences;
    private static final String TAG = "MainActivity";

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

    private Intent genBasicImageProcIntent(boolean capture)
    {
        String tmpStr;

        Intent intent = new Intent(this, PhotoFiltering.class);
        intent.putExtra(getString(R.string.Capture), capture);
        tmpStr = getString(R.string.ScribbleTutorial);
        intent.putExtra(tmpStr, sharedPreferences.getBoolean(tmpStr, true));
        tmpStr = getString(R.string.ParametersTutorial);
        intent.putExtra(tmpStr, sharedPreferences.getBoolean(tmpStr, true));
        return intent;
    }

    /**
     *
     */
    private void startCameraActivity()
    {
        //throw new UnsupportedOperationException("Internal camera feature is not implemented");
        startActivityForResult(genBasicImageProcIntent(true), FILTERING_RETURN_CODE);
    }

    // Protected members and methods
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else { setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); }
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_activity_label);

        // Create configuration files if none exist
        sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);

    }


    // Public members and methods
    public static final int APP_PERMISSIONS_REQUEST_CAMERA = 0;
    public static final int APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE = 1;
    public static final int FILTERING_RETURN_CODE = 2; // TODO - is this needed?

    /**
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        if (requestCode == APP_PERMISSIONS_REQUEST_CAMERA)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Permission was granted, activating camera
                startCameraActivity();
            }
            // Permission denied, do nothing.
        }
        else if (requestCode == MainActivity.APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Permission was granted, open gallery
                startGallery();
            }
            // Permission denied, do nothing
        }
    }

    /**
     *
     * @param ctx
     * @param intent
     * @return
     */
    public static boolean isIntentAvailable(Context ctx, Intent intent)
    {
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * The method checks whether the application has permission to
     * access the camera and if we don't an asynchronous permission
     * request is made.
     */
    public boolean requestCameraPermission()
    {
        boolean cameraPermissionCheck = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED);
        if(!cameraPermissionCheck)
        {
            // Request permission to access the camera, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, APP_PERMISSIONS_REQUEST_CAMERA);
            return false;
        }
        return true;
    }


    /**
     *
     * @param view
     */
    public void onNewPhotoClick(View view)
    {
        Toast.makeText(getApplicationContext(), "Sorry, feature is unavailable at the moment.", Toast.LENGTH_SHORT).show();
        ImageButton imageButton = findViewById(R.id.img_btn_capture_photo);
        imageButton.setOnClickListener(null);
        //TODO if(requestCameraPermission()) { startCameraActivity(); }
    }

    /**
     *
     * @param view
     */
    public void onGalleryButtonClick(View view)
    {
        if(requestExternalStoragePermission()) { startGallery(); }
    }

    public void startGallery()
    {
        startActivityForResult(genBasicImageProcIntent(false), FILTERING_RETURN_CODE);
    }

    public boolean requestExternalStoragePermission()
    {
        boolean readExternalStoragePermissionCheck = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED);
        if(!readExternalStoragePermissionCheck)
        {
            // Request permission to read and write to external storage, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MainActivity.APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }



}
