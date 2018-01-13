package cofproject.tau.android.cof;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by Uri on 16/11/2017.
 */

public class ParametersFragment extends Fragment
{

    public static final String TAG = "ParameterFragment";
    private OnFinishedCreateView mListener;
    private TextView mIterationCount;
    private TextView mCurrentSigma;
    private TextView mCurrentWindowSize;
    private TextView mCurrentPresetName;
    private TextView mCurrentQuantizationLevel;
    private List<Integer> mLayoutIds;


//    private void updateSpinnerMenu()
//    {
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, mPresets);
//        mPresetSpinner.setAdapter(adapter);
//    }



    public void onAttach(Context context)
    {
        super.onAttach(context);
        try
        {
            this.mListener = (OnFinishedCreateView) context;
        } catch (final ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }

    public void applyPreset(Preset preset)
    {
        applyPreset(preset.getName(), preset.getQuantization(), preset.getWindowSize(),
                preset.getSigma(), preset.getNumberOfIteration(), preset.getQuantization());
    }

    public void applyPreset(String name, int quantLvl, int windowSize, double sigma, int iterations,
                            int quantization)
    {
        setPresetName(name);
        setQuantizationLevel(quantLvl);
        setWindowSize(windowSize);
        setSigma(sigma);
        setIterationCount(iterations);
        setQuantizationLevel(quantization);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.parameters_fragment, container, false);
        mLayoutIds = Arrays.asList(R.id.current_deviation_value, R.id.current_iteration_value, R.id.current_preset_name, R.id.current_quantization_level, R.id.current_window_size);
        mCurrentPresetName = view.findViewById(R.id.current_preset_name);
        mCurrentSigma = view.findViewById(R.id.current_deviation_value);
        mCurrentQuantizationLevel = view.findViewById(R.id.current_quantization_level);
        mCurrentWindowSize = view.findViewById(R.id.current_window_size);
        mIterationCount = view.findViewById(R.id.current_iteration_value);
//        GridLayout gridLayout = view.findViewById(R.id.spatialGridLayout);
//
//        mIterationCount = view.findViewById(R.id.IterationPicker);
//        mIterationCount.setMinValue(ONE);
//
//        mCurrentWindowSize = view.findViewById(R.id.WindowSizePicker);
//        mCurrentWindowSize.setMinValue(ONE);
//
//        mCurrentSigma = view.findViewById(R.id.SigmaIntegerPicker);
//        mCurrentSigma.setMaxValue(SIGMA_INTEGER_LIMIT);
//        mCurrentSigma.setMinValue(ZERO);
//        mCurrentSigma.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
//        {
//            @Override
//            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
//            {
//                if(newVal == SIGMA_INTEGER_LIMIT && mFractionSigmaPicker.getValue() != ZERO) { mFractionSigmaPicker.setValue(ZERO); }
//            }
//        });
//
//        mFractionSigmaPicker = view.findViewById(R.id.SigmaFractionPicker);
//        mFractionSigmaPicker.setMinValue(ZERO);
//        mFractionSigmaPicker.setMaxValue(SIGMA_FRACTION_LIMIT);
//        mCurrentSigma.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
//        {
//            @Override
//            public void onValueChange(NumberPicker picker, int oldVal, int newVal)
//            {
//                if(mCurrentSigma.getValue() == SIGMA_INTEGER_LIMIT) { mFractionSigmaPicker.setValue(ZERO); }
//            }
//        });
//
//        mPresetSpinner = view.findViewById(R.id.presetSpinner);
//        mPresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
//        {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
//            {
//                Log.i(TAG, "onItemSelected: preset selected");
//                String name = ((Spinner)parent.findViewById(R.id.presetSpinner)).getSelectedItem().toString();
//                Preset temp = mListener.getCurrentPreset();
//                if (mListener.toLoadDefault() || !mListener.isReturning())
//                {
//                    Preset preset = mListener.getPreset(mPresetSpinner.getSelectedItem().toString());
//                    applyPreset(preset);
//                    mLastSelectedPreset = position;
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {}
//        });
////        mPresetSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener()
////        {
////            @Override
////            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
////            {
////                Log.d(TAG, "onClick: of preset item in spinner " + view.toString());
////                if (position == mLastSelectedPreset)
////                {
////                    applyPreset(mListener.getPreset(mPresetSpinner.getSelectedItem().toString()));
////                }
////            }
////        });
//        mListener.onComplete(mPresetSpinne
        mListener.loadPreset();
        return view;
    }

    public String getPresetName() { return mCurrentPresetName.getText().toString(); }

    public double getSigma()
    {
        return Double.parseDouble(mCurrentSigma.getText().toString());
    }

    public void setSigma(Double sigma)
    {
        mCurrentSigma.setText(String.format(Locale.ENGLISH, "%.02f", sigma));
    }

    public int getWindowSize()
    {
        return Integer.parseInt(mCurrentWindowSize.getText().toString());
    }

    public void setWindowSize(int windowSize)
    {
        mCurrentWindowSize.setText(String.format(Locale.ENGLISH, "%d", windowSize));
    }

    public int getIter()
    {
        return Integer.parseInt(mIterationCount.getText().toString());
    }

    public void setIterationCount(int iter)
    {
        mIterationCount.setText(String.format(Locale.ENGLISH, "%d", iter));
    }

    public int getQuantizationLevel()
    {
        return Integer.parseInt(mCurrentQuantizationLevel.getText().toString());
    }

    public void setPresetName(String presetName)
    {
        this.mCurrentPresetName.setText(presetName);
    }

    public void setQuantizationLevel(int quantizationLevel) { mCurrentQuantizationLevel.setText(String.format(Locale.ENGLISH, "%d", quantizationLevel)); }


    public interface OnFinishedCreateView
    {

        /**
         * Loads the current preset held by the parent activity (or whom ever it is that implements
         * the method if it is not null, otherwise the stored default preset will be loaded, if it
         * does not exists or is inapplicable to the chosen image the factory default preset will be
         * loaded instead.
         */
        void loadPreset();

    }
}
