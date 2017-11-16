package cofproject.tau.android.cof;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
    // Private members and methods
    private static final int APP_PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int APP_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int FILTERING_RETURN_CODE = 2;

    private Intent genBasicImageProcIntent(boolean capture)
    {
        Intent intent = new Intent(this, PhotoFiltering.class);
        intent.putExtra("capture", capture);
        return intent;
    }

    /**
     *
     */
    private void startCameraActivity()
    {
        startActivityForResult(genBasicImageProcIntent(true), FILTERING_RETURN_CODE);
    }

    // Protected members and methods
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //TODO - read counter from configuration file
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //TODO - write new counter to configuarion file.
    }


    // Public members and methods

    /**
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case APP_PERMISSIONS_REQUEST_CAMERA:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // Permission was granted, activating camera
                    startCameraActivity();
                }
                break;
                // Permission denied, do nothing.
            }
            case APP_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:

            // TODO - add cases to handle storage managment (not sure if we really need that permissions, will leave it be for now.
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
     *
     * @param view
     */
    public void onNewPhotoClick(View view)
    {
        boolean cameraPermissionCheck = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED);
        boolean readExternalStoragePermissionCheck = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED);

        if(!cameraPermissionCheck)
        {
            // Request permission to access the camera, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, APP_PERMISSIONS_REQUEST_CAMERA);
        }
        if(!readExternalStoragePermissionCheck)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, APP_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
        else
        {
            startCameraActivity();
        }
    }

    /**
     *
     * @param view
     */
    public void startGallery(View view)
    {
        startActivityForResult(genBasicImageProcIntent(false), FILTERING_RETURN_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }



}
