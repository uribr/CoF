package cofproject.tau.android.cof;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Utility
{
    static final int MAX_PRESET_NAME_LENGTH = 21;
    static final int SIGMA_SEEKBAR_LENGTH = 100;
    static final double MAX_SIGMA = 100;
    static final double ZERO_SIGMA = 0.001;
    static final int MAX_ITERATIONS = 10;
    static final int ONE = 1;
    static final String RELATIVE_WINDOW_SIZE = "relative window size";
    static final int ZERO = 0;
    static final int MAX_QUANTIZATION_LEVEL = 255;
    static final int MIN_QUANTIZATION_LEVEL = 2;
    static final int DEFAULT_WINDOW_SIZE = 16;
    static final int DEFAULT_NUMBER_OF_ITERATIONS = 1;
    static final byte DEFAULT_QUNTIZATION_LEVEL = 32;
    static final double DEFAULT_SIGMA = 1;
    static final String UNSAVED_PRESET_NAME = "Unsaved Preset";
    static final int CAMERA_CAPTURE_REQUEST_CODE = 0;
    static final int GALLERY_REQUEST_CODE = 1;
    static final int FILTER_SETTINGS_REQUEST_CODE = 2;
    static final String FROM_ORIGINAL_TO_FILTERING = "from original image to filtering";
    static final String FROM_FILTERING_TO_RESULT = "from filtering to result";
    static final String HAS_PRESET = "has preset";
    static final String WINDOW_SIZE = "window_size";
    static final String SIGMA = "sigma";
    static final String LANDSCAPE = "landscape";
    static final String IS_RELATIVE = "is relative";
    static final String ITERATIONS = "iterations";
    static final String QUANTIZATION = "quantization";
    static final String IMG_SIZE = "image size";
    static final String PRESET_NAME = "preset name";
    static final String EMPTY_STRING = "";
    private static final String TAG = "Utility";

    /**
     * Determines if the name chosen for a preset is at least one
     * character long and isn't the default presets' name.
     *
     * @param str
     * @return
     */
    static boolean isNameValid(String str, boolean canBeDefault)
    {
        boolean atLeastOneChar = false;
        CharacterIterator cI = new StringCharacterIterator(str);
        for (char c = cI.first(); c != CharacterIterator.DONE; c = cI.next())
        {
            if (Character.isAlphabetic(c) || Character.isSpaceChar(c))
            {
                atLeastOneChar = true;
            }
            else if (!Character.isDigit(c))
            {
                return false;
            }
        }
        return atLeastOneChar && (canBeDefault || !str.equals(Preset.DEFAULT_PRESET_NAME)) && !str.equals(UNSAVED_PRESET_NAME) && str.length() <= MAX_PRESET_NAME_LENGTH;
    }

    static boolean isNameValid(String str)
    {
        return isNameValid(str, false);
    }

    static Intent insertPresetToDataInent(Preset preset, Intent intent, int imgSize)
    {
        intent.putExtra(PRESET_NAME, preset.getName());
        intent.putExtra(HAS_PRESET ,new JSONObject(preset.presetToMap()).toString());
//        intent.putExtra(SIGMA, preset.getSigma());
//        if (preset.isRelative())
//        {
//            intent.putExtra(WINDOW_SIZE, preset.getWindowSize(imgSize));
//        }
//        else
//        {
//            intent.putExtra(WINDOW_SIZE, preset.getWindowSize());
//        }
//        intent.putExtra(ITERATIONS, preset.getNumberOfIteration());
//        intent.putExtra(QUANTIZATION, preset.getQuantization());
        return intent;
    }

    static Preset extractPresetFromDataIntent(Intent intent)
    {
        String jsonString = intent.getStringExtra(HAS_PRESET);
        JSONObject jsonObject;
        Map<String, String> map = new HashMap<>();
        try
        {
            if (jsonString == null) { return null; }

            jsonObject = new JSONObject(jsonString);

            Iterator<String> keysItr = jsonObject.keys();
            while(keysItr.hasNext())
            {
                String key = keysItr.next();
                String value = (String) jsonObject.get(key);
                map.put(key, value);
            }
        }

        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return new Preset(intent.getStringExtra(PRESET_NAME), map);
//                Preset(intent.getStringExtra(PRESET_NAME),
//                intent.getDoubleExtra(SIGMA, DEFAULT_SIGMA), intent.getIntExtra(ITERATIONS,
//                DEFAULT_NUMBER_OF_ITERATIONS), intent.getIntExtra(WINDOW_SIZE, DEFAULT_WINDOW_SIZE),
//                intent.getIntExtra(IMG_SIZE, 0), intent.getBooleanExtra(IS_RELATIVE,
//                false), intent.getIntExtra(QUANTIZATION, DEFAULT_QUNTIZATION_LEVEL));
    }


    static Bitmap getBitmap(Context context, Uri uri)
    {
        InputStream in;
        try
        {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = context.getContentResolver().openInputStream(uri);
            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            if (in != null)
            {
                in.close();
            }
            int n = (int) Math.floor(0.5 * Math.log(options.outWidth *
                    options.outHeight / IMAGE_MAX_SIZE) / Math.log(2.0));
            int scale = (int) Math.pow(2, n);

            Log.d(TAG, "scale = " + scale + ", orig-width: " + options.outWidth + ", orig-height: " + options.outHeight);
            Bitmap bitmap;
            in = context.getContentResolver().openInputStream(uri);
            if (scale > 1)
            {
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
            } else
            {
                bitmap = BitmapFactory.decodeStream(in);
            }
            if (in != null)
            {
                in.close();
            }
            Log.d(TAG, "bitmap size - width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());
            return bitmap;
        } catch (IOException e)
        {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }


    public static Bitmap getResizedBitmap(Bitmap image, int maxSize)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1)
        {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else
        {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    /**
     * A static method for verifying if there is an activity that accepts the intent provided
     * within the context of ctx
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

    @NonNull
    static Double mapSeekbarToSigma(int progress)
    {
        Log.d(TAG, "mapSeekbarToSigma: entering");
        return ((double) (progress)) * (1 / MAX_SIGMA);
    }
    static Integer mapSigmaToProgress(double sigma)
    {
        Log.d(TAG, "mapSigmaToProgress: entering");
        return ((int) (sigma * MAX_SIGMA));
    }
}
