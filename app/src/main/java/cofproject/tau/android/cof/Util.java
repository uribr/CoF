package cofproject.tau.android.cof;

/**
 * Created by noamg on 04/01/2018.
 */


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class Util {

    private static final String TAG = "Util";

    public static Bitmap getBitmap(Context context, Uri uri) {
        InputStream in;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = context.getContentResolver().openInputStream(uri);
            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            if (in != null) {
                in.close();
            }
            int n = (int) Math.floor(0.5 * Math.log(options.outWidth *
                    options.outHeight / IMAGE_MAX_SIZE) / Math.log(2.0));
            int scale = (int) Math.pow(2, n);

            Log.d(TAG, "scale = " + scale + ", orig-width: " + options.outWidth + ", orig-height: " + options.outHeight);
            Bitmap bitmap;
            in = context.getContentResolver().openInputStream(uri);
            if (scale > 1) {
                // scale to max possible inSampleSize that still yields an image larger than target
                options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inSampleSize = scale;
                bitmap = BitmapFactory.decodeStream(in, null, options);
                // resize to desired dimensions
                int height = bitmap.getHeight();
                int width = bitmap.getWidth();
                Log.d(TAG, "1st scale operation dimensions - width: " +
                        width + ", height: " + height);
                double y = Math.sqrt(IMAGE_MAX_SIZE / (((double) width) / height));
                double x = (y / height) * width;
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                        (int) x,
                        (int) y, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
                System.gc();
            } else {
                bitmap = BitmapFactory.decodeStream(in);
            }
            if (in != null) {
                in.close();
            }
            Log.d(TAG, "bitmap size - width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());
            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }


    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
