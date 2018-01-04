package cofproject.tau.android.cof;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;

/**
 * Created by Uri on 16/11/2017.
 */

public class ParametersFragment extends Fragment
{

    public interface OnCompleteListener
    {
        /**
         * Fill the spinner with presets and limit the range of parameters to the image
         * @param spinner
         */
        void onComplete(Spinner spinner);

        /**
         * Retrieve the preset that corresponds to name
         * @param name
         * @return
         */
        Preset getPreset (String name);

        /**
         * Retrieve the current preset
         * @return
         */
        Preset getCurrentPreset();
    }

    private OnCompleteListener mListener;
    private NumberPicker mIterationPicker;
    private NumberPicker mIntegerSigmaPicker;
    private NumberPicker mFractionSigmaPicker;
    private NumberPicker mSizePicker;
    private ArrayAdapter<String> mAdapter;
    private NumberPicker mWidthPicker;
    private Spinner mPresetSpinner;
    private Integer mImgSize;
    private List<String> mPresets;
    private View view;
    private static final int SIGMA_INTEGER_LIMIT = 10;
    private static final int SIGMA_FRACTION_LIMIT = 99;
    private static final int ONE = 1;
    private static final int ZERO = 0;
    private static final int ITER_LIMIT = 10;

    private void updateSpinnerMenu()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getView().getContext(), android.R.layout.simple_spinner_item, mPresets);
        mPresetSpinner.setAdapter(adapter);
    }

    public void onStored(String name)
    {
        mPresets.add(name);
        updateSpinnerMenu();
    }

    public void onRemovedPreset(String name)
    {
        mPresets.remove(name);
        updateSpinnerMenu();

    }

    public void onAttach(Context context)
    {
        super.onAttach(context);
        try
        {
            this.mListener = (OnCompleteListener)context;
        }
        catch (final ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }

    public void applyPreset(Preset preset)
    {
        mIntegerSigmaPicker.setValue(preset.getIntergerPartSigma());
        mFractionSigmaPicker.setValue(preset.getFractionalPartSigma());
        mIterationPicker.setValue(preset.getNumberOfIteration());
        mSizePicker.setValue(preset.getWindowSize(mImgSize));


        // TODO? mQuantizationPicker.setValue(preset.getQuantizationLevel());
    }


    private boolean isNameValid(String str)
    {
        boolean atLeastOneChar = false;
        CharacterIterator cI = new StringCharacterIterator(str);
        for (char c = cI.first(); c != CharacterIterator.DONE; c = cI.next())
        {
            if(Character.isAlphabetic(c)) { atLeastOneChar = true; }
            else if(!Character.isDigit(c)) { return false; }
        }
        return atLeastOneChar;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.parameters_fragment, container, false);
        GridLayout gridLayout = view.findViewById(R.id.spatialGridLayout);

        mIterationPicker = view.findViewById(R.id.IterationPicker);
        mIterationPicker.setMinValue(ONE);

        mSizePicker = view.findViewById(R.id.WindowSizePicker);
        mSizePicker.setMinValue(ONE);

        mIntegerSigmaPicker = view.findViewById(R.id.SigmaIntegerPicker);
        mIntegerSigmaPicker.setMaxValue(SIGMA_INTEGER_LIMIT);
        mIntegerSigmaPicker.setMinValue(ZERO);
        mIntegerSigmaPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                if(newVal == SIGMA_INTEGER_LIMIT && mFractionSigmaPicker.getValue() != ZERO)
                {
                    mFractionSigmaPicker.setValue(ZERO);
                }
            }
        });

        mFractionSigmaPicker = view.findViewById(R.id.SigmaFractionPicker);
        mFractionSigmaPicker.setMinValue(ZERO);
        mFractionSigmaPicker.setMaxValue(SIGMA_FRACTION_LIMIT);
        mIntegerSigmaPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
            {
                if(mIntegerSigmaPicker.getValue() == SIGMA_INTEGER_LIMIT)
                {
                    mFractionSigmaPicker.setValue(ZERO);
                }
            }
        });

        mPresetSpinner = view.findViewById(R.id.presetSpinner);
        mPresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Spinner temp = (Spinner)parent.findViewById(R.id.presetSpinner);
                String name = temp.getSelectedItem().toString();
                if(isNameValid(name))
                {
                    Preset preset = mListener.getPreset(name);

                    if(preset.getName().length() > 0)
                    {
                        applyPreset(preset);
                        //Toast.makeText(getContext(), "Preset Loaded", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(getContext(), "Invalid preset, modify preset to make it valid before applying it", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(getContext(), "Invalid name, preset loading failed.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        mListener.onComplete(mPresetSpinner);

        return view;
    }

    public double getSigma() { return (mIntegerSigmaPicker.getValue()+mFractionSigmaPicker.getValue()/100); }
    public int getHeight()   { return mSizePicker.getValue(); }
    public int getWidth()    { return mWidthPicker.getValue(); }
    public int getIter()     { return mIterationPicker.getValue(); }
    public void setDimensionsLimit(int size)
    {
        mImgSize = size;
    }

    public void applyLimiters()
    {
        mIterationPicker.setMaxValue(ITER_LIMIT);
        mSizePicker.setMaxValue(mImgSize);
    }
    public void setSigma(Double sigma)
    {
        mIntegerSigmaPicker.setValue(((Double)Math.floor(sigma)).intValue());
        mFractionSigmaPicker.setValue(((Double)(100*(sigma-Math.floor(sigma)))).intValue());
    }

    public void setIter(Integer iter) { mIterationPicker.setValue(iter); }
    public void setSize(Integer height) { mSizePicker.setValue(height); }
    public void setPresetList(List<String> presets) { mPresets = presets; }
    public void updatePreset(Preset cPreset)
    {
        cPreset.setNumberOfIteration(mIterationPicker.getValue());
        cPreset.setSigma(mIntegerSigmaPicker.getValue()+((double)mFractionSigmaPicker.getValue())/100);
        cPreset.setmName("Unsaved");
        cPreset.setRelative(false);
        cPreset.setWindowSize(mSizePicker.getValue());
    }
}
