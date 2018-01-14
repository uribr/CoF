package cofproject.tau.android.cof;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    // Public members and methods
    public static final int APP_PERMISSIONS_REQUEST_CAMERA = 0;
    public static final int APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE = 1;

    /**
     * Callback method for permission requests. Expects an {@link #APP_PERMISSIONS_REQUEST_CAMERA}
     * or {@link #APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE} as requestCode and calls
     * the {@link #startFilteringTaskViaCamera()} or {@link #startFilteringTaskViaGallery()}
     * respectively.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        // Camera request
        if (requestCode == APP_PERMISSIONS_REQUEST_CAMERA)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Permission was granted, activating camera
                startFilteringTaskViaCamera();
            }
            // Permission denied, do nothing.
        }
        // External storage request
        else if (requestCode == MainActivity.APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Permission was granted, open gallery
                startFilteringTaskViaGallery();
            }
            // Permission denied, do nothing
        }
    }

    /**
     * The method checks whether the the current context has permission to
     * access the camera. If it does not an asynchronous permission request is issued.
     * @return whether the current context has permission to access the camera.
     */
    public boolean requestCameraPermission()
    {
        // Check if we have permission.
        boolean cameraPermissionCheck = (ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.CAMERA) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED);

        if (!cameraPermissionCheck)
        {
            // Request permission to access the camera, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    APP_PERMISSIONS_REQUEST_CAMERA);
            return false;
        }

        // We already have permission.
        return true;
    }

    /**
     * The method checks whether the the current context has permission to
     * access the external storage. If it does not an asynchronous permission request is issued.
     * @return whether the current context has permission to access the external storage.
     */
    public boolean requestExternalStoragePermission()
    {
        // Check if we have permission.
        boolean readExternalStoragePermissionCheck = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED);

        if (!readExternalStoragePermissionCheck)
        {
            // Request permission to read and write to external storage, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MainActivity.APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE);

            return false;
        }

        // We already have permission
        return true;
    }

    /**
     * Attempts to call {@link #startFilteringTaskViaCamera()} if the current context has permission
     * to access the camera, if not an asynchronous request for such a permission is made and method
     * terminates. If the permission is already given, the method calls the
     * startFilteringTaskViaCamera to initiate the filtering task.
     *
     * See {@link #requestCameraPermission()} and
     * {@link #onRequestPermissionsResult(int, String[], int[])}for more information.
     *
     * @param view - the calling view.
     */
    public void onCameraButtonClick(View view)
    {
        Toast.makeText(getApplicationContext(), "Sorry, feature is unavailable at the moment.", Toast.LENGTH_SHORT).show();
        // Check for permission.
        //TODO if(requestCameraPermission())
//         {
//             startFilteringTaskViaCamera();
//         }
    }

    /**
     * Attempts to call {@link #startFilteringTaskViaGallery()} if the current context has
     * permission to access the camera, if not an asynchronous request for such a permission is made
     * and method terminates. If the permission is already given, the method calls the
     * startFilteringTaskViaCamera to initiate the filtering task.
     *
     * See {@link #requestExternalStoragePermission()} and
     * {@link #onRequestPermissionsResult(int, String[], int[])}for more information.
     *
     * @param view - the calling view.
     */
    public void onGalleryButtonClick(View view)
    {
        // Check for permission.
        if (requestExternalStoragePermission())
        {
            startFilteringTaskViaGallery();
        }
    }




    // Private members and methods
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;

    /**
     * Convenience for creating an intent that initiates the filtering task.
     * @param capture - to use camera or not (not using the camera will force use
     *                of the device storage)
     * @return the intent to issue
     */
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
     * Starts the filtering task with the device storage as the image source.
     */
    private void startFilteringTaskViaCamera()
    {
        //throw new UnsupportedOperationException("Internal camera feature is not implemented");
        startActivity(genBasicImageProcIntent(true));
    }

    /**
     * Starts the filtering task with the device storage as the image source.
     */
    public void startFilteringTaskViaGallery()
    {
        startActivity(genBasicImageProcIntent(false));
    }

    // Protected members and methods
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_activity_label);

        // Create configuration files if none exist
        sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);

    }


}
