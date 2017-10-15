package cofproject.tau.android.cof;

import android.Manifest;
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

    private void startCameraActivity()
    {
        Intent intent = new Intent(this, PhotoFiltering.class);
        intent.putExtra("capture", true);
        startActivity(intent);
    }

    // Protected members and methods
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Intent intent = new Intent();
//        intent.setType("image/*");
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        if(!isIntentAvailable(getBaseContext(), intent))
//        {
//
//        }
    }


    // Public members and methods
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
            // TODO - add cases to handle storage managment (not sure if we really need that permissions, will leave it be for now.
        }
    }

    public static boolean isIntentAvailable(Context ctx, Intent intent)
    {
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public void onNewPhotoClick(View view)
    {
        boolean permissionCheck = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED);
        if(!permissionCheck)
        {
            // Request permission to access the camera, this is done ASYNCHRONOUSLY!
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, APP_PERMISSIONS_REQUEST_CAMERA);
        }
        else
        {
            startCameraActivity();
        }
    }


    public void startGallery(View view)
    {
        Intent intent = new Intent(this, PhotoFiltering.class);
        intent.putExtra("capture", false);
        startActivity(intent);
    }

}
