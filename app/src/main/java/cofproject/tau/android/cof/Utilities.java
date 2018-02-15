package cofproject.tau.android.cof;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

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

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

/**
 * This class holds variety of utilities used in the application.
 */
class Utilities
{
    private static final int MAX_PRESET_NAME_LENGTH = 21;
    static final String DEFAULT_PRESET_NAME = "Default";
    static final int SIGMA_SEEKBAR_LENGTH = 100;
    static final float MAX_SIGMA = 100;
    static final float ZERO_SIGMA = ((float) 0.001);
    static final int MAX_ITERATIONS = 10;
    static final int MAX_QUANTIZATION_LEVEL = 256;
    static final int MIN_QUANTIZATION_LEVEL = 2;
    static final int DEFAULT_NUMBER_OF_ITERATIONS = 1;
    static final byte DEFAULT_QUNTIZATION_LEVEL = 32;
    static final int DEFAULT_WINDOW_SIZE = 15;
    static final double DEFAULT_ALPHA = 0.5; // for FB-CoF
    static final float DEFAULT_SIGMA = 2 * (float) Math.sqrt(DEFAULT_WINDOW_SIZE) + 1;
    static final String UNSAVED_PRESET_NAME = "Unsaved Preset";
    static final int FILTER_SETTINGS_REQUEST_CODE = 2;
    static final String PRESET_NAME = "preset name";

    // general
    static final String QUANTIZATION = "quantization";
    //CoF
    static final String STAT_WINDOW_SIZE = "stat_window_size";
    static final String RELATIVE_STAT_WINDOW_SIZE = "relative_stat_window_size";
    static final String STAT_SIGMA = "stat_sigma";
    static final String FILT_WINDOW_SIZE = "filt_window_size";
    static final String RELATIVE_FILT_WINDOW_SIZE = "relative_filt_window_size";
    static final String FILT_SIGMA = "filt_sigma";
    static final String ITERATIONS = "iterations";
    //FB-CoF
    static final String FILT_WINDOW_SIZE_FB = "filt_window_size_fb";
    static final String RELATIVE_FILT_WINDOW_SIZE_FB = "relative_filt_window_size_fb";
    static final String ITERATIONS_FB = "iterations_fb";
    //Scribble Cosmetics
    static final String SCRIBBLE_WIDTH = "scribble_width";
    static final String SCRIBBLE_COLOR = "scribble_color";

    static final String LANDSCAPE = "landscape";
    static final String IS_RELATIVE = "is relative";
    static final String IMG_SIZE = "image size";
    static final String SCRIBBLE_COLOR_KEY = "SCRIBBLE_COLOR_KEY";
    static SharedPreferences currentPresetFile;
    static SharedPreferences defaultPresetFile;

    private static final String TAG = "Utilities";
    static final int SCRIBBLE_THRESHOLD_MAX_VAL = 255;
    static final int SCRIBBLE_THRESHOLD_INIT_VAL = SCRIBBLE_THRESHOLD_MAX_VAL / 2;
    static final Size SCRIBBLE_DILATION_WINDOW_SIZE = new Size(7, 7);
    static final int SCRIBBLE_DILATION_ITERATIONS_DEFAULT = 3;
    static final Scalar ZERO_SCALAR = new Scalar(0);


    /**
     * Determines if the name chosen for a preset is at least one
     * character long and isn't the default presets' name.
     *
     * @param str The given preset name
     * @return true iff the given preset name is valid
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
        return atLeastOneChar && (canBeDefault || !str.equals(DEFAULT_PRESET_NAME)) && str.length() <= MAX_PRESET_NAME_LENGTH;
    }

    static boolean isNameValid(String str) {
        return isNameValid(str, false);
    }

    static Map<String, String> getHardcodedDefaultParameters()
    {
        Map<String, String> map = new HashMap<>();
        map.put(PRESET_NAME, DEFAULT_PRESET_NAME);
        map.put(STAT_WINDOW_SIZE, String.valueOf(DEFAULT_WINDOW_SIZE));
        map.put(RELATIVE_STAT_WINDOW_SIZE, "0");
        map.put(STAT_SIGMA, String.valueOf(DEFAULT_SIGMA));
        map.put(FILT_WINDOW_SIZE, String.valueOf(DEFAULT_WINDOW_SIZE));
        map.put(RELATIVE_FILT_WINDOW_SIZE, "0");
        map.put(FILT_SIGMA, String.valueOf(DEFAULT_SIGMA));
        map.put(ITERATIONS, String.valueOf(DEFAULT_NUMBER_OF_ITERATIONS));
        map.put(FILT_WINDOW_SIZE_FB, String.valueOf(DEFAULT_WINDOW_SIZE));
        map.put(RELATIVE_FILT_WINDOW_SIZE_FB, "0");
        map.put(ITERATIONS_FB, String.valueOf(DEFAULT_NUMBER_OF_ITERATIONS));
        map.put(QUANTIZATION, String.valueOf(DEFAULT_QUNTIZATION_LEVEL));
        map.put(IS_RELATIVE, String.valueOf(false));
        return map;
    }


    @SuppressLint("ApplySharedPref")
    static void updatePreset(Preset preset, SharedPreferences prefs, int imgSize)
    {
        SharedPreferences.Editor editor = prefs.edit();
        // Put the string representation of the JSON object holding the mapping of
        // the preset parameters.
        editor.putString(PRESET_NAME, preset.getName());
        editor.putString(preset.getName(), new JSONObject(preset.presetToMap()).toString());
        editor.commit();
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putString(PRESET_NAME, preset.getName());
//        editor.putFloat(STAT_SIGMA, preset.getStatSigma().floatValue());
//        editor.putInt(STAT_WINDOW_SIZE, preset.getStatWindowSize(imgSize));
//        editor.putInt(ITERATIONS, preset.getNumberOfIteration());
//        editor.putInt(QUANTIZATION, preset.getQuantization());
//        editor.putBoolean(IS_RELATIVE, preset.isRelative());
//        editor.commit();
    }

    static void updateCurrentPreset(Preset curPreset, int imgSize)
    {
        updatePreset(curPreset, currentPresetFile, imgSize);
    }

    @NonNull
    private static Preset loadPreset(SharedPreferences prefs)
    {
        String presetName = prefs.getString(PRESET_NAME, DEFAULT_PRESET_NAME);
        Map<String, String> map = convertJSONString2Map(prefs.getString(presetName, new JSONObject().toString()));
        if (map == null || map.isEmpty())
        {
            throw new NullPointerException("Preference file is empty or uninitialized");
        }
        return new Preset(presetName, map);

//        return new Preset(prefs.getString(PRESET_NAME, DEFAULT_PRESET_NAME),
//                prefs.getBoolean(IS_RELATIVE, false),
//
//                prefs.getFloat(STAT_SIGMA, DEFAULT_SIGMA),
//                prefs.getInt(ITERATIONS, DEFAULT_NUMBER_OF_ITERATIONS),
//                prefs.getInt(STAT_WINDOW_SIZE, DEFAULT_WINDOW_SIZE),
//                prefs.getInt(QUANTIZATION, DEFAULT_QUNTIZATION_LEVEL));
    }

    @NonNull
    static Preset loadCurrentPreset()
    {
        return loadPreset(currentPresetFile);
    }


    @NonNull
    static Preset loadDefaultPreset()
    {
        return loadPreset(defaultPresetFile);
    }


    @Nullable
    static Map<String, String> convertJSONString2Map(String JSONString) {
        JSONObject jsonObject;
        Map<String, String> map = new HashMap<>();
        try {
            jsonObject = new JSONObject(JSONString);

            Iterator<String> keysItr = jsonObject.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                String value = (String) jsonObject.get(key);
                map.put(key, value);
            }
            return map;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }



    /**
     * Creates a compressed version of the chosen image, in order to save memory space.
     * @param context The current context (activity)
     * @param uri The Uri object holding the image data
     * @return A compresed version of the loaded image
     */
    @Nullable
    static Bitmap getBitmap(Context context, Uri uri) {
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

    /**
     * Generates a basic alert dialog with a "WARNING" title and with a message according to the given message ID.
     * @param context The context (activity)
     * @param msgID The message ID (in R.strings)
     * @return an AlertDialog.Builder object with the given message. It is possible to add positive and negative buttons
     * to it.
     */
    static AlertDialog.Builder generateBasicAlertDialog(Context context, int msgID)
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(R.string.alert_dialog_warning);
        alertDialog.setMessage(msgID);
        return alertDialog;
    }


    /**
     * Releases all the matrices lists in the given list
     * @param matLsts List of lists of matrices
     */
    @SafeVarargs
    static void releaseMats(List<Mat>... matLsts)
    {
        for (List<Mat> lst : matLsts)
        {
            releaseMats(lst);
        }
    }

    /**
     * Releases all the matrices in the given list
     * @param lst A list of matrices
     */
    private static void releaseMats(List<Mat> lst)
    {
        for (Mat m : lst)
        {
            if (m != null)
            {
                m.release();
            }
        }
    }

    /**
     * Releases all the matrices in the given list
     * @param lst A list of matrices
     */
    static void releaseMats(Mat... lst)
    {
        for (Mat m : lst)
        {
            if (m != null)
            {
                m.release();
            }
        }
    }

    /**
     * A class holding the ShowcaseView parameters (view ID and text ID)
     */
    static class ShowcaseViewParams
    {
        int viewId;
        int textId;

        ShowcaseViewParams(int viewId, int textId)
        {
            this.viewId = viewId;
            this.textId = textId;
        }
    }


    /**
     * Runs a MaterialShowcaseViwe tutorial sequence in the current activity
     * @param activity The current activity
     * @param titleId Tutorial title ID
     * @param numOfViews Number of tutrial messages
     * @param showcaseViewParams The different ShowcaseViews parameters objects
     */
    static void showTutorial(Activity activity, int titleId, int numOfViews, List<ShowcaseViewParams> showcaseViewParams)
    {
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(100); // half second between each showcase view
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(activity);
        sequence.setConfig(config);
        View view;
        String title = activity.getString(titleId);
        String content;
        String dismissText = "Next";
        ShowcaseViewParams currParams;

        for(int i = 0; i < numOfViews; i++)
        {
            currParams = showcaseViewParams.get(i);
            view = activity.findViewById(currParams.viewId);
            if (view == null)
            {
                view = new View(activity);
            }
            content = activity.getString(currParams.textId);
            if (i == 1)
            { // remove the title in the second showcase
                title = "";
            }
            if (i == numOfViews - 1)
            {
                dismissText = "Got It!";
            }
            MaterialShowcaseView.Builder msv = new MaterialShowcaseView.Builder(activity)
                                        .setTarget(view)
                                        .setDismissText(dismissText)
                                        .setTitleText(title)
                                        .setContentText(content)
                                        .setDismissOnTouch(true);

            if (currParams.viewId == R.id.post_filtering_buttons_layout)
            {
                msv.withRectangleShape();
            }
            sequence.addSequenceItem(msv.build());
        }
        //sequence.singleUse(sequenceKey);
        sequence.start();
    }
}
