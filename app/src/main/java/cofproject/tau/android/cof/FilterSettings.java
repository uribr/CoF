// AlertDialog related code is Based on code from: https://www.mkyong.com/android/android-prompt-user-input-dialog-example/
package cofproject.tau.android.cof;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import static cofproject.tau.android.cof.Preset.DEFAULT_PRESET_NAME;
import static cofproject.tau.android.cof.Utility.IMG_SIZE;
import static cofproject.tau.android.cof.Utility.ITERATIONS;
import static cofproject.tau.android.cof.Utility.LANDSCAPE;
import static cofproject.tau.android.cof.Utility.MAX_ITERATIONS;
import static cofproject.tau.android.cof.Utility.MAX_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utility.MAX_SIGMA;
import static cofproject.tau.android.cof.Utility.MIN_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utility.QUANTIZATION;
import static cofproject.tau.android.cof.Utility.SIGMA_SEEKBAR_LENGTH;
import static cofproject.tau.android.cof.Utility.UNSAVED_PRESET_NAME;
import static cofproject.tau.android.cof.Utility.WINDOW_SIZE;
import static cofproject.tau.android.cof.Utility.convertJSONString2Map;
import static cofproject.tau.android.cof.Utility.defaultPresetFile;
import static cofproject.tau.android.cof.Utility.isNameValid;
import static cofproject.tau.android.cof.Utility.loadCurrentPreset;
import static cofproject.tau.android.cof.Utility.loadDefaultPreset;
import static cofproject.tau.android.cof.Utility.mapSeekbarToSigma;
import static cofproject.tau.android.cof.Utility.mapSigmaToProgress;
import static cofproject.tau.android.cof.Utility.updatePreset;

public class FilterSettings extends AppCompatActivity implements ParametersFragment.OnFinishedCreateView
{

    private static final String TAG = "FilterSettings";

    private SharedPreferences mPresetPref;
    private Preset mPreset;
    private ParametersFragment mFilteringParametersFragment;
    private boolean mIsADialogOpen;
    private int mImgSize;
    private List<String> mPresets;

    private boolean mIsLandscape;

    private void onRemovedPreset(String name)
    {
        Log.d(TAG, "onRemovedPreset: ");
        mPresets.remove(name);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: entering");
        mIsADialogOpen = false;
        mPresets = new ArrayList<>();
        mFilteringParametersFragment = new ParametersFragment();
        Intent intent = this.getIntent();
        mIsLandscape = intent.getBooleanExtra(LANDSCAPE, false);
        mImgSize = intent.getIntExtra(IMG_SIZE, 0);
        if (mImgSize == 0)
        {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        // Load preset
        mPreset = loadCurrentPreset();

        Log.d(TAG, "onCreate: adding fragments");
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (mIsLandscape)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mFilteringParametersFragment = new ParametersFragment();
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_photo_filtering);
        // Create filtering related buttons fragment and
        transaction.add(R.id.filtering_activity_button_container, new InFilterButtonsFragment());
        transaction.add(R.id.main_view_container, mFilteringParametersFragment);
        transaction.commit();

        Log.d(TAG, "onCreate: loading shared preference file for presests");
        mPresetPref = this.getSharedPreferences(getString(R.string.PresetsConfigFileName), Context.MODE_PRIVATE);
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();

        // If the current preset is null, create it.
        Log.d(TAG, "onPause: setting return value");
        if (mFilteringParametersFragment.getPresetName().equals(UNSAVED_PRESET_NAME) || mPreset == null)
        {
            createPresetFromUserSettings(UNSAVED_PRESET_NAME, false, false);
        }

        // Update current preset
        updateCurrentPreset();
//        Utility.insertPresetToDataInent(mPreset, intent, mImgSize);
        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }

//    @Override
//    protected void onPause()
//    {
//        super.onPause();
//        Intent intent = new Intent();
//
//        // If the current preset is null, create it.
//        Log.d(TAG, "onPause: setting return value");
//        if (mFilteringParametersFragment.getPresetName().equals(UNSAVED_PRESET_NAME) || mPreset == null)
//        {
//            createPresetFromUserSettings(UNSAVED_PRESET_NAME, false, false);
//        }
//
//        // Insert preset to the intent.
//        Utility.insertPresetToDataInent(mPreset, intent, mImgSize);
//        setResult(Activity.RESULT_OK, intent);
//    }

    private List<String> loadPresetsNameList()
    {
        Log.d(TAG, "loadPresetsNameList: entering");
        Vector<String> list = new Vector<>();
        Map<String, ?> map = mPresetPref.getAll();
        list.add(getString(R.string.DefaultPresetName));
        for (Map.Entry<String, ?> entry : map.entrySet())
        {
            list.add(entry.getKey());
        }
        return list;
    }


    public boolean storePreset(Preset preset)
    {
        if (preset.validate())
        {
            SharedPreferences.Editor editor = mPresetPref.edit();
            // Put the string representation of the JSON object holding the mapping of
            // the preset parameters.
            editor.putString(preset.getName(), new JSONObject(preset.presetToMap()).toString());
            return editor.commit();
        }
        return false;
    }
    
    private void createPresetFromUserSettings(String name, boolean relative, boolean newDefault)
    {
        Log.d(TAG, "createPresetFromUserSettings: creating preset");
        mPreset = new Preset(newDefault ? DEFAULT_PRESET_NAME : name, mFilteringParametersFragment.getSigma(),
                mFilteringParametersFragment.getIter(), mFilteringParametersFragment.getWindowSize()
                , mImgSize, relative,
                mFilteringParametersFragment.getQuantizationLevel());
    }

    public void loadPreset()
    {
        Log.d(TAG, "loadPreset: entering");
        mFilteringParametersFragment.applyPreset(mPreset);
//        try
//        {
//            if (mPreset == null)
//            {
//                mPreset = loadDefaultPreset(mImgSize);
//            }
//            mFilteringParametersFragment.applyPreset(mPreset);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
    }

    public void loadPreset(String name)
    {
        Log.d(TAG, "loadPreset: entering");
        if (name.equals(DEFAULT_PRESET_NAME))
        {
            mPreset = loadDefaultPreset(mImgSize);
        }
        else
        {
            Map<String, String> map = convertJSONString2Map(mPresetPref.getString(name, new JSONObject().toString()));
            if (map != null && !map.isEmpty())
            {
                mPreset = new Preset(name, map);
                Log.i(TAG, "loadPreset: Preset loaded successfully");
            }
        }
        mFilteringParametersFragment.applyPreset(mPreset);
    }


    public void onSaveButtonClick(View view)
    {
        Log.d(TAG, "onSaveButtonClick: entering");
        // Based on code from: https://www.mkyong.com/android/android-prompt-user-input-dialog-example/
        // setup the alert builder
        LayoutInflater li = LayoutInflater.from(this);
        final View promptsView = li.inflate(R.layout.prompt, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(promptsView);


        Log.d(TAG, "onSaveButtonClick: creating widgets");
        // Add a listener to the user input to check if the name is valid
        // and display an error to the user if it isn't valid.
        final EditText userInput = (EditText) promptsView.findViewById(R.id.savePresetPromptUserInput);
        final CheckBox relativeCheckBox = promptsView.findViewById(R.id.relativePresetCheckBox);
        final CheckBox setAsDefaultCheckBox = promptsView.findViewById(R.id.SetAsDefaultCheckBox);
        final StringNameWatcher presetNameWatcher = new StringNameWatcher(userInput, setAsDefaultCheckBox);
        userInput.addTextChangedListener(presetNameWatcher);

        setAsDefaultCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    userInput.setText(DEFAULT_PRESET_NAME);
                    userInput.setEnabled(false);
                }
                else
                {
                    userInput.setText("");
                    userInput.setEnabled(true);
                }
            }
        });

        Log.d(TAG, "onSaveButtonClick: adding buttons");
        // add a button
        builder.setPositiveButton(getString(R.string.save_text), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

                String name = userInput.getText().toString();
                // Create the preset object
                createPresetFromUserSettings(name, relativeCheckBox.isChecked(), setAsDefaultCheckBox.isChecked());

                // if the input is valid (e.g no error is being displayed)
                // we attempt to storePreset the preset in the configuration file
                // and announce the success or failure of the saving.


                if(isNameValid(name, setAsDefaultCheckBox.isChecked()))
                {
                    if(setAsDefaultCheckBox.isChecked())
                    {
                        updateDefaultPreset();
                    }
                    else if(storePreset(mPreset))
                    {
                        mPresets.add(name);
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Saving failed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateCurrentPreset();
                    mFilteringParametersFragment.applyPreset(mPreset);
                    Toast.makeText(getApplicationContext(), "Preset saved", Toast.LENGTH_SHORT).show();

                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Invalid name", Toast.LENGTH_SHORT).show();
                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });

        Log.d(TAG, "onSaveButtonClick: show dialog");
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateDefaultPreset()
    {
        updatePreset(mPreset, defaultPresetFile, mImgSize);
    }


    @SuppressLint("ApplySharedPref")
    public void onDelteButtonPreset(View view)
    {
        Log.i(TAG, "onDelteButtonPress: onClick event");
        // Default preset cannot be deleted but it can be overridden.
        if (!mPreset.getName().equals(getString(R.string.DefaultPresetName)))
        {
            SharedPreferences.Editor editor = mPresetPref.edit();
            editor.remove(mPreset.getName());
            editor.commit();
            onRemovedPreset(mPreset.getName());
            Toast.makeText(getApplicationContext(), "Preset deleted.",
                    Toast.LENGTH_SHORT).show();
            mFilteringParametersFragment.setPresetName(UNSAVED_PRESET_NAME);
            Log.d(TAG, "onDelteButtonPress: deleteing preset");
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Cannot delete default preset",
                    Toast.LENGTH_SHORT).show();
        }
    }


    public void onHelpClick(View view)
    {
        Log.d(TAG, "onHelpClick: onClick event");
        //TODO - show a tutorial of the application that should be used when the user first reache a new screen. Note that the tutorial is screen-dependent
    }

    public void onSettingClick(View view)
    {
        Log.d(TAG, "onSettingClick: onClick event");
        if (!mIsADialogOpen)
        {
            mIsADialogOpen = true;
            int id = view.getId();
            switch (id)
            {
                case R.id.preset_layout:
                    Log.d(TAG, "onSettingClick: preset layout");
                    showPresetSpinnerDialog();
                    break;
                case R.id.iteration_layout:
                    Log.d(TAG, "onSettingClick: iteration layout");
                    showNumberPickerDialog("Choose number of iterations:", MAX_ITERATIONS,
                            mFilteringParametersFragment.getIter(), ITERATIONS);
                    break;
                case R.id.deviation_layout:
                    Log.d(TAG, "onSettingClick: deviation layout");
                    showSeekbarDialog();
                    break;
                case R.id.quantization_layout:
                    Log.d(TAG, "onSettingClick: quantization layout");
                    showNumberPickerDialog("Choose quantization level:", MAX_QUANTIZATION_LEVEL,
                            MIN_QUANTIZATION_LEVEL, mFilteringParametersFragment.getQuantizationLevel(),
                            QUANTIZATION);
                    break;
                case R.id.window_size_layout:
                    Log.d(TAG, "onSettingClick: window size layout");
                    showNumberPickerDialog("Choose window size", mImgSize,
                            mFilteringParametersFragment.getWindowSize(), WINDOW_SIZE);
                    break;
                default:
                    Log.d(TAG, "onSettingClick: no button chosen");
                    break;
            }
        }
    }

    private void showPresetSpinnerDialog()
    {
        Log.d(TAG, "showPresetSpinnerDialog: entering");

        // setup the alert builder
        LayoutInflater li = LayoutInflater.from(this);

        Log.d(TAG, "showPresetSpinnerDialog: creating widgets");
        final View promptsView = li.inflate(R.layout.spinner_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(promptsView);
        builder.setTitle(R.string.select_a_preset);
        final SpinnerReselect spinnerReselect = promptsView.findViewById(R.id.presetSpinner);

        Log.d(TAG, "showPresetSpinnerDialog: populating the spinner");
        List<String> array = new ArrayList<>(loadPresetsNameList());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, array);
        spinnerReselect.setAdapter(adapter);

        Log.d(TAG, "showPresetSpinnerDialog: adding buttons");
        // Accept button
        builder.setPositiveButton(getString(R.string.load_preset), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                loadPreset(spinnerReselect.getSelectedItem().toString());
                updateCurrentPreset();
            }
        });

        // Cancel button
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                mIsADialogOpen = false;
            }
        });
        Log.d(TAG, "showPresetSpinnerDialog: show dialog");
        AlertDialog dialog = builder.create();
        dialog.show();


    }

    private void updateCurrentPreset()
    {
        Utility.updateCurrentPreset(mPreset, mImgSize);
    }

    private void showNumberPickerDialog(String msg, int maxValue, int currentValue, String tag)
    {
        Log.d(TAG, "showNumberPickerDialog: wrapper method for showNumberPickerDialog when no min value is provided");
        showNumberPickerDialog(msg, maxValue, 1, currentValue, tag);
    }

    private void showNumberPickerDialog(String msg, int maxValue, int minValue,
                                        int currentValue, String tag)
    {
        Log.d(TAG, "showNumberPickerDialog: entering");
        final int curVal = currentValue;
        final String internalTag = tag;
        // setup the alert builder
        LayoutInflater li = LayoutInflater.from(this);
        final View promptsView = li.inflate(R.layout.numberpicker_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(promptsView);
        Log.d(TAG, "showNumberPickerDialog: creating widgets");

        builder.setTitle(msg);
        final NumberPicker numberPicker = promptsView.findViewById(R.id.NumberPicker);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setMinValue(minValue);
        numberPicker.setValue(currentValue);

        Log.d(TAG, "showNumberPickerDialog: adding buttons");
        // Accept button
        builder.setPositiveButton(getString(R.string.okMsg), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                int newVal = numberPicker.getValue();
                if(newVal != curVal)
                {
                    switch (internalTag)
                    {
                        case QUANTIZATION:
                            mFilteringParametersFragment.setQuantizationLevel(newVal);
                            break;
                        case ITERATIONS:
                            mFilteringParametersFragment.setIterationCount(newVal);
                            break;
                        case WINDOW_SIZE:
                            mFilteringParametersFragment.setWindowSize(newVal);
                            mFilteringParametersFragment.setSigma(2 * Math.sqrt(newVal) + 1);
                            break;
                    }
                    // Once the values loaded from the preset have been changed, we display
                    // the string "Unsaved Preset" in the current value of the preseet
                    // in the settings activity.
                    mFilteringParametersFragment.setPresetName("Unsaved Preset");
                }
            }
        });

        // Cancel button
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog) { mIsADialogOpen = false; }
        });

        Log.d(TAG, "showNumberPickerDialog: showing dialog");
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSeekbarDialog()
    {
        Log.d(TAG, "showSeekbarDialog: entering");

        // setup the alert builder
        LayoutInflater li = LayoutInflater.from(this);

        Log.d(TAG, "showSeekbarDialog: creating widgets");
        final View promptsView = li.inflate(R.layout.seekbar_dialog, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(promptsView);

        builder.setTitle(R.string.select_spatial_u03c3_value);

        final SeekBar seekBar = promptsView.findViewById(R.id.seekbar);
        seekBar.setMax(((int) Math.floor(MAX_SIGMA * SIGMA_SEEKBAR_LENGTH)));
        seekBar.setProgress(mapSigmaToProgress(mFilteringParametersFragment.getSigma()));

        final EditText editText = promptsView.findViewById(R.id.progress);
        editText.setText(String.format(Locale.ENGLISH,"%.02f",
                mFilteringParametersFragment.getSigma()));
        editText.addTextChangedListener(new SigmaValueWatcher(editText, seekBar));

        Log.d(TAG, "showSeekbarDialog: adding seekbar listener");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser)
                {
                    editText.setText(String.format(Locale.ENGLISH, "%04.002f",
                            mapSeekbarToSigma(progress)));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Log.d(TAG, "showSeekbarDialog: adding buttons");
        // Accept button
        builder.setPositiveButton(getString(R.string.okMsg), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Change the parameter value only if it positive.
                if (seekBar.getProgress() >= 0)
                {
                    double newSigmaValue =  mapSeekbarToSigma(seekBar.getProgress());
                    if (mFilteringParametersFragment.getSigma() != newSigmaValue
                            && newSigmaValue > 0.00 && editText.getError() == null)
                    {
                        // Once the values loaded from the preset have been changed, we display
                        // the string "Unsaved Preset" in the current value of the preseet
                        // in the settings activity.
                        mFilteringParametersFragment.setPresetName(UNSAVED_PRESET_NAME);
                        mFilteringParametersFragment.setSigma(
                                mapSeekbarToSigma(seekBar.getProgress()));
                    }

                }
            }
        });

        // Cancel button
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                mIsADialogOpen = false;
            }
        });
        Log.d(TAG, "showSeekbarDialog: showing dialog");
        AlertDialog dialog = builder.create();
        dialog.show();
    }


}

