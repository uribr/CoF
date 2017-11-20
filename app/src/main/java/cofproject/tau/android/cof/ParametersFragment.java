package cofproject.tau.android.cof;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

/**
 * Created by Uri on 16/11/2017.
 */

public class ParametersFragment extends Fragment
{
    private EditText mSigmaET;
    private EditText mIterET;
    private EditText mHeighET;
    private EditText mWidthET;
    private NumberPicker nm;
    private Integer imgHeight, imgWidth;
    private static final double SIGMA_LIMIT = 10;
    private static final int ITER_LIMIT = 10;

    private void addOnTextChangeListeners()
    {
        String negMsg = "must be non-negative.";
        String posMsg = "must be positive.";
        String aboveLimMsg = "must be less then ";
        String natMsg = "must be a natural number";

        mSigmaET.addTextChangedListener(new DoubleParameterWatcher(SIGMA_LIMIT, negMsg, aboveLimMsg + Double.toString(SIGMA_LIMIT), " ", "\u03C3 ", mSigmaET));
        mIterET.addTextChangedListener(new IntegerParameterWatcher(ITER_LIMIT, posMsg, aboveLimMsg + Integer.toString(ITER_LIMIT), natMsg,"The number of iterations ", mIterET));
        mHeighET.addTextChangedListener(new IntegerParameterWatcher(imgHeight, negMsg, aboveLimMsg + imgHeight.toString(), natMsg, "The height ", mHeighET));
        mWidthET.addTextChangedListener(new IntegerParameterWatcher(imgWidth, negMsg, aboveLimMsg + imgWidth.toString(), natMsg, "The width ", mWidthET));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.parameters_fragment, container, false);

        mSigmaET = view.findViewById(R.id.spatial_sigma_input);
        mHeighET = view.findViewById(R.id.window_height_input);
        mWidthET = view.findViewById(R.id.window_width_input);
        mIterET = view.findViewById(R.id.number_of_iterations);
        nm = view.findViewById(R.id.numberPicker2);
        nm.setMaxValue(ITER_LIMIT);
        nm.setMinValue(0);

        addOnTextChangeListeners();

        return view;
    }

    public boolean verifyValues()
    {
        boolean bool_sigma = mSigmaET.getError() == null && !mSigmaET.getText().toString().isEmpty();
        boolean bool_iter = mIterET.getError() == null && !mIterET.getText().toString().isEmpty();
        boolean bool_height = mHeighET.getError() == null && !mHeighET.getText().toString().isEmpty();
        boolean bool_width = mWidthET.getError() == null && !mWidthET.getText().toString().isEmpty();

        return bool_height && bool_width && bool_iter && bool_sigma;
    }

    public double getSigma() {return Double.parseDouble(mSigmaET.getText().toString());}
    public int getHeight()   {return Integer.parseInt(mHeighET.getText().toString());}
    public int getWidth()    {return Integer.parseInt(mWidthET.getText().toString());}
    public int getIter()     { return nm.getValue(); }//return Integer.parseInt(mIterET.getText().toString());}
    public void setDimensions(int height, int width)
    {
        imgHeight = height;
        imgWidth = width;
    }

}
