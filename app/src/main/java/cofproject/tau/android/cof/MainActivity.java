package cofproject.tau.android.cof;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static cofproject.tau.android.cof.Utilities.*;

@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity {

    // Protected members and methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_activity_label);

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //tryShowTutorial();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tryShowTutorial();
            }
        }, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    private void tryShowTutorial() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String tutorialKey = getString(R.string.main_activity_tutorial_key);
        Boolean firstTime = preferences.getBoolean(tutorialKey,true);
        if (firstTime) {
            List<ShowcaseViewParams> params = new ArrayList<>();
            params.add(new ShowcaseViewParams(R.id.capture_photo_layout, R.string.capture_photo_btn_tutorial));
            params.add(new ShowcaseViewParams(R.id.browse_gallery_layout, R.string.browse_gallery_btn_tutorial));
            params.add(new ShowcaseViewParams(R.id.help_button, R.string.help_button_tutorial));
            showTutorial(this, R.string.main_activity_tutorial_title, params.size(), params);
            preferences.edit().putBoolean(tutorialKey, false).apply();
        }
    }

    public void startInfoActivity(View view) {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }


    // public members and methods
    private static final int APP_PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE = 1;

    /**
     * Callback method for permission requests. Expects an {@link #APP_PERMISSIONS_REQUEST_CAMERA}
     * or {@link #APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE} as requestCode and calls
     * the {@link #startFilteringTask(boolean)}.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        // Camera request
        if (requestCode == APP_PERMISSIONS_REQUEST_CAMERA) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, activating camera
                startFilteringTask(true);
            }
            // Permission denied, do nothing.
        }
        // External storage request
        else if (requestCode == MainActivity.APP_PERMISSIONS_REQUEST_READ_AND_WRITE_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, open gallery
                startFilteringTask(false);
            }
            // Permission denied, do nothing
        }
    }


    /**
     * The method checks whether the the current context has permission to
     * access the camera. If it does not an asynchronous permission request is issued.
     *
     * @return whether the current context has permission to access the camera.
     */
    private boolean requestCameraPermission() {
        // Check if we have permission.
        boolean cameraPermissionCheck = (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED);

        boolean readExternalStoragePermissionCheck = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED);

        if (!cameraPermissionCheck) {
            String[] requests;
            if (!readExternalStoragePermissionCheck) // ask for both permissions
            {
                requests = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

            } else // ask only for camera - already have read/write permission
            {
                requests = new String[]{Manifest.permission.CAMERA};
            }
            // Request permission to access the camera, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this, requests, APP_PERMISSIONS_REQUEST_CAMERA);
            return false;
        }

        // We already have permission.
        return true;
    }

    /**
     * The method checks whether the the current context has permission to
     * access the external storage. If it does not an asynchronous permission request is issued.
     *
     * @return whether the current context has permission to access the external storage.
     */
    private boolean requestExternalStoragePermission() {
        // Check if we have permission.
        boolean readExternalStoragePermissionCheck = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED);

        if (!readExternalStoragePermissionCheck) {
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
     * Attempts to call {@link #startFilteringTask(boolean)} if the current context has permission
     * to access the camera, if not an asynchronous request for such a permission is made and method
     * terminates. If the permission is already given, the method calls the
     * startFilteringTaskViaCamera to initiate the filtering task.
     * <p>
     * See {@link #requestCameraPermission()} and
     * {@link #onRequestPermissionsResult(int, String[], int[])}for more information.
     *
     * @param view - the calling view.
     */
    public void onCameraButtonClick(View view) {
        Log.i(TAG, "onCameraButtonClick: click event");
        if (requestCameraPermission()) {
            startFilteringTask(true);
        }
    }

    /**
     * Attempts to call {@link #startFilteringTask(boolean)} if the current context has
     * permission to access the camera, if not an asynchronous request for such a permission is made
     * and method terminates. If the permission is already given, the method calls the
     * startFilteringTaskViaCamera to initiate the filtering task.
     * <p>
     * See {@link #requestExternalStoragePermission()} and
     * {@link #onRequestPermissionsResult(int, String[], int[])}for more information.
     *
     * @param view - the calling view.
     */
    public void onGalleryButtonClick(View view) {
        Log.i(TAG, "onGalleryButtonClick: click event");

        // Check for permission.
        if (requestExternalStoragePermission()) {
            startFilteringTask(false);
        }
    }


    // Private members and methods
    //private SharedPreferences mSharedPreferences;
    private static final String TAG = "MainActivity";


    private Intent genBasicImageProcIntent(boolean fromCamera) {
        Intent intent = new Intent(this, FilteringActivity.class);
        intent.putExtra(getString(R.string.Capture), fromCamera);
        return intent;
    }


    /**
     * Starts the filtering task. If fromCamera == true, we capture the image with the camera.
     * Otherwise, we load an image from gallery.
     */
    private void startFilteringTask(boolean fromCamera) {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
        startActivity(genBasicImageProcIntent(fromCamera));
    }

    public void onClickHelpButton(MenuItem item) {
        String tutorialKey = getString(R.string.main_activity_tutorial_key);
        getPreferences(MODE_PRIVATE).edit().putBoolean(tutorialKey, true).apply();
        tryShowTutorial();
    }

//    public void onClickHelpButton(MenuItem item) {
//        Toast.makeText(this, "pressed in " + TAG, Toast.LENGTH_SHORT).show();
//    }
}
