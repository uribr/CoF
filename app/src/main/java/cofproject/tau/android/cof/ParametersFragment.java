package cofproject.tau.android.cof;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Uri on 16/11/2017.
 */

public class ParametersFragment extends Fragment
{

    public static interface OnCompleteListener
    {
        public abstract void onComplete(Spinner spinner);
    }

    private OnCompleteListener mListener;
    private NumberPicker mIterationPicker;
    private NumberPicker mIntegerSigmaPicker;
    private NumberPicker mFractionSigmaPicker;
    private NumberPicker mHeightPicker;
    private ArrayAdapter<String> mAdapter;
    private NumberPicker mWidthPicker;
    private Spinner mPresetSpinner;
    private Preset mPreset;
    private Integer imgHeight, imgWidth;
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
        mHeightPicker.setValue(preset.getHeight(imgHeight));
        mWidthPicker.setValue(preset.getWidth(imgWidth));
        // TODO? mQuantizationPicker.setValue(preset.getQuantizationLevel());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.parameters_fragment, container, false);
        GridLayout gridLayout = view.findViewById(R.id.spatialGridLayout);

        mIterationPicker = view.findViewById(R.id.IterationPicker);
        mIterationPicker.setMinValue(ONE);

        mHeightPicker = view.findViewById(R.id.HeightPicker);
        mHeightPicker.setMinValue(ONE);

        mWidthPicker=view.findViewById(R.id.WidthPicker);
        mWidthPicker.setMinValue(ONE);

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

        mListener.onComplete(mPresetSpinner);

        return view;
    }

    public double getSigma() { return (mIntegerSigmaPicker.getValue()+mFractionSigmaPicker.getValue()/100); }
    public int getHeight()   { return mHeightPicker.getValue(); }
    public int getWidth()    { return mWidthPicker.getValue(); }
    public int getIter()     { return mIterationPicker.getValue(); }
    public void setDimensionsLimit(int height, int width)
    {
        imgHeight = height;
        imgWidth = width;
    }

    public void applyLimiters()
    {
        mIterationPicker.setMaxValue(ITER_LIMIT);
        mHeightPicker.setMaxValue(imgHeight);
        mWidthPicker.setMaxValue(imgWidth);
    }
    public void setSigma(Double sigma)
    {
        mIntegerSigmaPicker.setValue(((Double)Math.floor(sigma)).intValue());
        mFractionSigmaPicker.setValue(((Double)(100*(sigma-Math.floor(sigma)))).intValue());
    }

    public void setIter(Integer iter) { mIterationPicker.setValue(iter); }
    public void setHeight(Integer height) { mHeightPicker.setValue(height); }
    public void setWidth(Integer width) { mWidthPicker.setValue(width); }
    public void setPresetList(List<String> presets) { mPresets = presets; }
}
