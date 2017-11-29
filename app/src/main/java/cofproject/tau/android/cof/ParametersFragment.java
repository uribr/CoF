package cofproject.tau.android.cof;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;

/**
 * Created by Uri on 16/11/2017.
 */

public class ParametersFragment extends Fragment
{
//    private EditText mSigmaET;
//    private EditText mIterET;
//    private EditText mHeighET;
//    private EditText mWidthET;
    private NumberPicker mIterationPicker;
    private NumberPicker mIntegerSigmaPicker;
    private NumberPicker mFractionSigmaPicker;
    private NumberPicker mHeightPicker;
    private NumberPicker mWidthPicker;
    private Preset mPreset;
    private Integer imgHeight, imgWidth;
    private static final int SIGMA_INTEGER_LIMIT = 10;
    private static final int SIGMA_FRACTION_LIMIT = 99;
    private static final int ONE = 1;
    private static final int ZERO = 0;
    private static final int ITER_LIMIT = 10;

    private void addOnTextChangeListeners()
    {
//        String negMsg = "must be non-negative.";
//        String posMsg = "must be positive.";
//        String aboveLimMsg = "must be less then ";
//        String natMsg = "must be a natural number";

//        mSigmaET.addTextChangedListener(new DoubleParameterWatcher(SIGMA_INTEGER_LIMIT, negMsg, aboveLimMsg + Double.toString(SIGMA_INTEGER_LIMIT), " ", "\u03C3 ", mSigmaET));
//        mIterET.addTextChangedListener(new IntegerParameterWatcher(ITER_LIMIT, posMsg, aboveLimMsg + Integer.toString(ITER_LIMIT), natMsg,"The number of iterations ", mIterET));
//        mHeighET.addTextChangedListener(new IntegerParameterWatcher(imgHeight, negMsg, aboveLimMsg + imgHeight.toString(), natMsg, "The height ", mHeighET));
//        mWidthET.addTextChangedListener(new IntegerParameterWatcher(imgWidth, negMsg, aboveLimMsg + imgWidth.toString(), natMsg, "The width ", mWidthET));
    }

    public void applyPreset(Preset preset)
    {
        mPreset = preset;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.parameters_fragment, container, false);
        GridLayout gridLayout = view.findViewById(R.id.spatialGridLayout);

//        mSigmaET = view.findViewById(R.id.spatial_sigma_input);
//        mHeighET = view.findViewById(R.id.window_height_input);
//        mWidthET = view.findViewById(R.id.window_width_input);
//        mIterET = view.findViewById(R.id.number_of_iterations);

        mIterationPicker = view.findViewById(R.id.IterationPicker);
        mIterationPicker.setMaxValue(ITER_LIMIT);
        mIterationPicker.setMinValue(ONE);

        mHeightPicker = view.findViewById(R.id.HeightPicker);
        mHeightPicker.setMaxValue(imgHeight);
        mHeightPicker.setMinValue(ONE);

        mWidthPicker=view.findViewById(R.id.WidthPicker);
        mWidthPicker.setMaxValue(imgWidth);
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
//        addOnTextChangeListeners();

        mWidthPicker.setValue(mPreset.getWidth());
        mHeightPicker.setValue(mPreset.getHeight(imgHeight));
        mIntegerSigmaPicker.setValue(mPreset.getIntergerPartSigma());
        mFractionSigmaPicker.setValue(mPreset.getFractionalPartSigma());
        mIterationPicker.setValue(mPreset.getNumberOfIteration());
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

    public void setSigma(Double sigma)
    {
        mIntegerSigmaPicker.setValue(((Double)Math.floor(sigma)).intValue());
        mFractionSigmaPicker.setValue(((Double)(100*(sigma-Math.floor(sigma)))).intValue());
    }

    public void setIter(Integer iter) { mIterationPicker.setValue(iter); }
    public void setHeight(Integer height) { mHeightPicker.setValue(height); }
    public void setWidth(Integer width) { mWidthPicker.setValue(width); }

}
