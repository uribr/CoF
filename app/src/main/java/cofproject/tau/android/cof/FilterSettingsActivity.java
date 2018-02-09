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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import static cofproject.tau.android.cof.Utilities.IMG_SIZE;
import static cofproject.tau.android.cof.Utilities.ITERATIONS;
import static cofproject.tau.android.cof.Utilities.MAX_ITERATIONS;
import static cofproject.tau.android.cof.Utilities.MAX_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utilities.MAX_SIGMA;
import static cofproject.tau.android.cof.Utilities.MIN_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utilities.QUANTIZATION;
import static cofproject.tau.android.cof.Utilities.SIGMA_SEEKBAR_LENGTH;
import static cofproject.tau.android.cof.Utilities.STAT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.UNSAVED_PRESET_NAME;
import static cofproject.tau.android.cof.Utilities.convertJSONString2Map;
import static cofproject.tau.android.cof.Utilities.defaultPresetFile;
import static cofproject.tau.android.cof.Utilities.isNameValid;
import static cofproject.tau.android.cof.Utilities.loadCurrentPreset;
import static cofproject.tau.android.cof.Utilities.loadDefaultPreset;
import static cofproject.tau.android.cof.Utilities.mapSeekbarToSigma;
import static cofproject.tau.android.cof.Utilities.mapSigmaToProgress;
import static cofproject.tau.android.cof.Utilities.updatePreset;

public class FilterSettingsActivity extends AppCompatActivity implements ParametersFragment.OnFinishedCreateView
{

    private static final String TAG = "FilterSettingsActivity";

    private SharedPreferences mPresetPref;
    private Preset mPreset;
    private ParametersFragment mFilteringParametersFragment;
    private boolean mIsADialogOpen;
    private int mImgSize;
    private List<String> mPresets;

    //private boolean mIsLandscape;

    private void onRemovedPreset(String name)
    {
        Log.d(TAG, "onRemovedPreset: ");
        mPresets.remove(name);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle("Settings");
        Log.d(TAG, "onCreate: entering");
        mIsADialogOpen = false;
        mPresets = new ArrayList<>();
        Intent intent = this.getIntent();
        //mIsLandscape = intent.getBooleanExtra(LANDSCAPE, false);
        mImgSize = intent.getIntExtra(IMG_SIZE, 0);
        if (mImgSize == 0)
        {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        // Load preset
        mPreset = loadCurrentPreset();

        Log.d(TAG, "onCreate: adding fragments");

//        if (mIsLandscape)
//        {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            mFilteringParametersFragment = new ParametersFragment();
//        }
//        else
//        {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        }
        setContentView(R.layout.activity_filter_settings);
        initFragments();

        Log.d(TAG, "onCreate: loading shared preference file for presests");
        mPresetPref = this.getSharedPreferences(getString(R.string.PresetsConfigFileName), Context.MODE_PRIVATE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_filter_settings);
        if (mFilteringParametersFragment.getPresetName().equals(UNSAVED_PRESET_NAME) || mPreset == null)
        {
            createPresetFromUserSettings(UNSAVED_PRESET_NAME, false, false);
        }
        // Update current preset
        updateCurrentPreset();
        initFragments();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();

        // If the current preset is null, create it.
        Log.d(TAG, "onBackPressed: started");
        if (mFilteringParametersFragment.getPresetName().equals(UNSAVED_PRESET_NAME) || mPreset == null)
        {
            createPresetFromUserSettings(UNSAVED_PRESET_NAME, false, false);
        }

        // Update current preset
        updateCurrentPreset();
//        Utilities.insertPresetToDataInent(mPreset, intent, mImgSize);
        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

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


    public void onSavePresetClick(View view)
    {
        Log.d(TAG, "onSavePresetClick: entering");
        // Based on code from: https://www.mkyong.com/android/android-prompt-user-input-dialog-example/
        // setup the alert builder
        LayoutInflater li = LayoutInflater.from(this);
        final View promptsView = li.inflate(R.layout.prompt, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(promptsView);


        Log.d(TAG, "onSavePresetClick: creating widgets");
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

        Log.d(TAG, "onSavePresetClick: adding buttons");
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

        Log.d(TAG, "onSavePresetClick: show dialog");
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateDefaultPreset()
    {
        updatePreset(mPreset, defaultPresetFile, mImgSize);
    }


    @SuppressLint("ApplySharedPref")
    public void onDeleteButtonPreset(View view)
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
                            mFilteringParametersFragment.getWindowSize(), STAT_WINDOW_SIZE);
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, array);
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
        Utilities.updateCurrentPreset(mPreset, mImgSize);
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
                        case STAT_WINDOW_SIZE:
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

    private void initFragments() {
        mFilteringParametersFragment = new ParametersFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        ButtonsFragment buttonsFragment = ButtonsFragment.newInstance(R.layout.filter_settings_buttons_fragment);
        transaction.add(R.id.settings_activity_button_container, buttonsFragment);
        transaction.add(R.id.main_settings_view_container, mFilteringParametersFragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onClickHelpButton(MenuItem item) {
        String tutorialKey = getString(R.string.filter_settings_tutorial_key);
        getPreferences(MODE_PRIVATE).edit().putBoolean(tutorialKey, true).apply();
        // re-create the buttons fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_activity_button_container,
                        ButtonsFragment.newInstance(R.layout.filter_settings_buttons_fragment))
                .commit();
    }
}

