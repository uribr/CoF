package cofproject.tau.android.cof;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static cofproject.tau.android.cof.Preset.DEFAULT_PRESET_NAME;

class Utility
{
    static final int MAX_PRESET_NAME_LENGTH = 21;
    static final int SIGMA_SEEKBAR_LENGTH = 100;
    static final float MAX_SIGMA = 100;
    static final float ZERO_SIGMA = ((float) 0.001);
    static final int MAX_ITERATIONS = 10;

    static final String RELATIVE_WINDOW_SIZE = "relative window size";
    static final int MAX_QUANTIZATION_LEVEL = 255;
    static final int MIN_QUANTIZATION_LEVEL = 2;
    static final int DEFAULT_NUMBER_OF_ITERATIONS = 1;
    static final byte DEFAULT_QUNTIZATION_LEVEL = 32;
    static final int DEFAULT_WINDOW_SIZE = 15;
    static final float DEFAULT_SIGMA = 2 * (float)Math.sqrt(DEFAULT_WINDOW_SIZE) + 1;
    static final String UNSAVED_PRESET_NAME = "Unsaved Preset";
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
    static SharedPreferences currentPresetFile;
    static SharedPreferences defaultPresetFile;
    static final String TAG = "Utility";
    static final int SCRIBBLE_THRESHOLD_MAX_VAL = 255;
    static final int SCRIBBLE_THRESHOLD_INIT_VAL = SCRIBBLE_THRESHOLD_MAX_VAL / 2;
    static final Size SCRIBBLE_DILATION_WINDOW_SIZE = new Size(7,7);
    static final int SCRIBBLE_DILATION_ITERATIONS_DEFAULT = 3;
    static final Scalar ZERO_SCALAR = new Scalar(0);




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
        return atLeastOneChar && (canBeDefault || !str.equals(Preset.DEFAULT_PRESET_NAME)) && str.length() <= MAX_PRESET_NAME_LENGTH;
    }

    static boolean isNameValid(String str)
    {
        return isNameValid(str, false);
    }


    @SuppressLint("ApplySharedPref")
    static void updatePreset(Preset preset, SharedPreferences prefs, int imgSize)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PRESET_NAME, preset.getName());
        editor.putFloat(SIGMA, preset.getSigma().floatValue());
        editor.putInt(WINDOW_SIZE, preset.getWindowSize(imgSize));
        editor.putInt(ITERATIONS, preset.getNumberOfIteration());
        editor.putInt(QUANTIZATION, preset.getQuantization());
        editor.putBoolean(IS_RELATIVE, preset.isRelative());
        editor.commit();
    }

    static void updateCurrentPreset(Preset curPreset, int imgSize)
    {
        updatePreset(curPreset, currentPresetFile, imgSize);
    }


    static Preset loadPreset(SharedPreferences prefs)
    {
        return new Preset(prefs.getString(PRESET_NAME, DEFAULT_PRESET_NAME),
                prefs.getFloat(SIGMA, DEFAULT_SIGMA),
                prefs.getInt(ITERATIONS, DEFAULT_NUMBER_OF_ITERATIONS),
                prefs.getInt(WINDOW_SIZE, DEFAULT_WINDOW_SIZE),
                prefs.getBoolean(IS_RELATIVE, false),
                prefs.getInt(QUANTIZATION, DEFAULT_QUNTIZATION_LEVEL));
    }

    static Preset loadCurrentPreset()
    {
        return loadPreset(currentPresetFile);
    }


    static Preset loadDefaultPreset(int imgSize)
    {
        return loadPreset(defaultPresetFile);
//
//        if(map != null && map.isEmpty())
//        {
//            return new Preset(DEFAULT_PRESET_NAME, map);
//        }
//
//        String jsonString = defaultPresetFile.getString(new JSONObject().toString());
//        JSONObject jsonObject = new JSONObject(jsonString);
//
//        Iterator<String> keysItr = jsonObject.keys();
//        while(keysItr.hasNext())
//        {
//            String key = keysItr.next();
//            String value = (String) jsonObject.get(key);
//            map.put(key, value);
//        }
//
//        if (map.isEmpty())
//        {
//            // No default preset found, generating an hardcoded default preset
//            mPreset = Preset.createPreset(mImgSize);
//            storePreset(mPreset);
//            if (mImgSize < mPreset.getWindowSize())
//            {
//                mPreset = Preset.createPreset(mImgSize);
//                Toast.makeText(getApplicationContext(), "Default window size is too large for the selected image.\n Factory default preset is being loaded instead.", Toast.LENGTH_LONG).show();
//            }
//            else
//            {
//                Toast.makeText(getApplicationContext(), "Default preset created.", Toast.LENGTH_SHORT).show();
//            }
//        } else
//        {
//            Log.d(TAG, "loadPreset: loading the default preset");
//            mPreset = new Preset(DEFAULT_PRESET_NAME, map);
//        }
    }


//    static Intent insertPresetToDataInent(Preset preset, Intent intent, int imgSize)
//    {
//        intent.putExtra(PRESET_NAME, preset.getName());
//        intent.putExtra(HAS_PRESET ,new JSONObject(preset.presetToMap()).toString());
////        intent.putExtra(SIGMA, preset.getSigma());
////        if (preset.isRelative())
////        {
////            intent.putExtra(WINDOW_SIZE, preset.getWindowSize(imgSize));
////        }
////        else
////        {
////            intent.putExtra(WINDOW_SIZE, preset.getWindowSize());
////        }
////        intent.putExtra(ITERATIONS, preset.getNumberOfIteration());
////        intent.putExtra(QUANTIZATION, preset.getQuantization());
//        return intent;
//    }


    static Map<String, String>  convertJSONString2Map(String JSONString)
    {
        JSONObject jsonObject;
        Map<String, String> map = new HashMap<>();
        try
        {
            jsonObject = new JSONObject(JSONString);

            Iterator<String> keysItr = jsonObject.keys();
            while(keysItr.hasNext())
            {
                String key = keysItr.next();
                String value = (String) jsonObject.get(key);
                map.put(key, value);
            }
            return map;
        }

        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return null;
    }

//    static Preset extractPresetFromDataIntent(Intent intent)
//    {
//        String jsonString = intent.getStringExtra(HAS_PRESET);
//        JSONObject jsonObject;
//        Map<String, String> map = new HashMap<>();
//        try
//        {
//            if (jsonString == null) { return null; }
//
//            jsonObject = new JSONObject(jsonString);
//
//            Iterator<String> keysItr = jsonObject.keys();
//            while(keysItr.hasNext())
//            {
//                String key = keysItr.next();
//                String value = (String) jsonObject.get(key);
//                map.put(key, value);
//            }
//        }
//
//        catch (JSONException e)
//        {
//            e.printStackTrace();
//        }
//        return new Preset(intent.getStringExtra(PRESET_NAME), map);
////                Preset(intent.getStringExtra(PRESET_NAME),
////                intent.getDoubleExtra(SIGMA, DEFAULT_SIGMA), intent.getIntExtra(ITERATIONS,
////                DEFAULT_NUMBER_OF_ITERATIONS), intent.getIntExtra(WINDOW_SIZE, DEFAULT_WINDOW_SIZE),
////                intent.getIntExtra(IMG_SIZE, 0), intent.getBooleanExtra(IS_RELATIVE,
////                false), intent.getIntExtra(QUANTIZATION, DEFAULT_QUNTIZATION_LEVEL));
//    }


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
        //Log.d(TAG, "mapSeekbarToSigma: entering");
        return ((double) (progress)) * (1 / MAX_SIGMA);
    }
    static Integer mapSigmaToProgress(double sigma)
    {
        //Log.d(TAG, "mapSigmaToProgress: entering");
        return ((int) (sigma * MAX_SIGMA));
    }

    static AlertDialog.Builder generateBasicAlertDialog(Context context, String title, String msg) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        return alertDialog;
    }

    static AlertDialog.Builder generateBasicAlertDialog(Context context, String title, int msgID) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(title);
        alertDialog.setMessage(msgID);
        return alertDialog;
    }


    @SafeVarargs
    static void releaseMats(List<Mat>... matLsts) {
        for (List<Mat> lst : matLsts) {
            releaseMats(lst);
        }
    }

    private static void releaseMats(List<Mat> lst){
        for (Mat m : lst) {
            if (m != null) {
                m.release();
            }
        }
    }

    static void releaseMats(Mat... lst){
        for (Mat m : lst) {
            if (m != null) {
                m.release();
            }
        }
    }
}
