package cofproject.tau.android.cof;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Credit: http://www.vogella.com/tutorials/AndroidCamera/article.html
 */

public class PhotoFiltering extends AppCompatActivity {

    private static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    private static final int GALLERY_REQUEST_CODE = 1;

    private Bitmap bitmap;
    private ImageView newPhotoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_filtering);
        newPhotoView = (ImageView) findViewById(R.id.new_photo_view);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }
        Intent intent = new Intent();
        if (extras.getBoolean("capture")) {
            intent.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
        } else {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        InputStream stream = null;
        if ((requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_CAPTURE_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
            try {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                stream = getContentResolver().openInputStream(data.getData());
                bitmap = BitmapFactory.decodeStream(stream);
                newPhotoView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
