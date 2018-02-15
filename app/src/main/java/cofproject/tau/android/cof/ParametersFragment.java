package cofproject.tau.android.cof;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by Uri on 16/11/2017.
 */

public class ParametersFragment extends Fragment
{

    public static final String TAG = "ParameterFragment";
    private OnFinishedCreateView mListener;
    private TextView mCurrentPresetName;
    // general
    private TextView mCurrentQuantizationLevels;
    // CoF
    private TextView mCurrentStatWindowSize;
    private TextView mCurrentStatSigma;
    private TextView mCurrentFiltWindowSize;
    private TextView mCurrentFiltSigma;
    private TextView mIterationCount;
    // FB-CoF
    private TextView mCurrentFiltWindowSizeFB;
    private TextView mIterationCountFB;
    // Scribble Cosmetics
//    private TextView mCurre

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        try
        {
            this.mListener = (OnFinishedCreateView) context;
        } catch (final ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnFinishCreateView");
        }
    }

    public void applyPreset(Preset preset, int imgSize)
    {
        setPresetName(preset.getName());
        setQuantizationLevel(preset.getQuantization());
        setStatWindowSize(preset.getStatWindowSize(imgSize));
        setStatSigma(preset.getStatSigma());
        setFiltWindowSize(preset.getFiltWindowSize(imgSize));
        setFiltSigma(preset.getFiltSigma());
        setIterationCount(preset.getNumberOfIteration());
        setFiltWindowSizeFB(preset.getFiltWindowSizeFB(imgSize));
        setIterationCountFB(preset.getNumberOfIterationFB());
//        setScribbleWidth(preset.getScribbleWidth());
//        setScribbleColor(preset.getScribbleColor());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreateView: creating parameters fragment view");
        View view = inflater.inflate(R.layout.parameters_fragment, container, false);
        //mLayoutIds = Arrays.asList(R.id.current_deviation_value, R.id.current_iteration_value, R.id.current_preset_name, R.id.current_quantization_level, R.id.current_window_size);
        mCurrentPresetName = view.findViewById(R.id.current_preset_name);
        // general
        mCurrentQuantizationLevels = view.findViewById(R.id.current_quantization_level);
        //CoF
        mCurrentStatWindowSize = view.findViewById(R.id.current_stat_window_size);
        mCurrentStatSigma = view.findViewById(R.id.current_stat_deviation_value);
        mCurrentFiltWindowSize = view.findViewById(R.id.current_filt_window_size);
        mCurrentFiltSigma = view.findViewById(R.id.current_filt_deviation_value);
        mIterationCount = view.findViewById(R.id.current_iteration_value);
        //FB-CoF
        mCurrentFiltWindowSizeFB = view.findViewById(R.id.current_fb_filt_window_size);
        mIterationCountFB = view.findViewById(R.id.current_fb_iteration_value);

        mListener.loadPreset();
        return view;
    }

    public String getPresetName() { return mCurrentPresetName.getText().toString(); }

    public float getStatSigma()
    {
        Log.d(TAG, "getStatSigma: " + mCurrentStatSigma.getText().toString());
        Log.d(TAG, String.format("getStatSigma; %f", Float.parseFloat(mCurrentStatSigma.getText().toString())) );
        return Float.parseFloat(mCurrentStatSigma.getText().toString());
    }

    public float getFiltSigma()
    {
        Log.d(TAG, "getFiltSigma: " + mCurrentStatSigma.getText().toString());
        Log.d(TAG, String.format("getFiltSigma; %f", Float.parseFloat(mCurrentFiltSigma.getText().toString())) );
        return Float.parseFloat(mCurrentFiltSigma.getText().toString());
    }

    public void setStatSigma(Double sigma)
    {
        mCurrentStatSigma.setText(String.format(Locale.ENGLISH, "%.02f", sigma));
    }

    public void setFiltSigma(Double sigma)
    {
        mCurrentFiltSigma.setText(String.format(Locale.ENGLISH, "%.02f", sigma));
    }

    public int getStatWindowSize()
    {
        return Integer.parseInt(mCurrentStatWindowSize.getText().toString());
    }

    public int getWindowSize()
    {
        return Integer.parseInt(mCurrentFiltWindowSize.getText().toString());
    }

    public int getWindowSizeFB()
    {
        return Integer.parseInt(mCurrentFiltWindowSizeFB.getText().toString());
    }

    public void setStatWindowSize(int windowSize)
    {
        mCurrentStatWindowSize.setText(String.format(Locale.ENGLISH, "%d", windowSize));
    }

    public void setFiltWindowSize(int windowSize)
    {
        mCurrentFiltWindowSize.setText(String.format(Locale.ENGLISH, "%d", windowSize));
    }

    public void setFiltWindowSizeFB(int windowSize)
    {
        mCurrentFiltWindowSizeFB.setText(String.format(Locale.ENGLISH, "%d", windowSize));
    }

    public int getIter()
    {
        return Integer.parseInt(mIterationCount.getText().toString());
    }

    public int getIterFB()
    {
        return Integer.parseInt(mIterationCountFB.getText().toString());
    }

    public void setIterationCount(int iter)
    {
        mIterationCount.setText(String.format(Locale.ENGLISH, "%d", iter));
    }

    public void setIterationCountFB(int iter)
    {
        mIterationCountFB.setText(String.format(Locale.ENGLISH, "%d", iter));
    }

    public int getQuantizationLevel()
    {
        return Integer.parseInt(mCurrentQuantizationLevels.getText().toString());
    }

//    public void setScribbleWidth(int scribbleWidth)
//    {
//        mScribbleWidth.setText(String.format(Locale.ENGLISH, "%d", scribbleWidth))
//    }
//
//    public void setScribbleColor(Object scribbleColor)
//    {
//        mScribbleColor.setColor(scribbleColor);
//    }

    public void setPresetName(String presetName)
    {
        this.mCurrentPresetName.setText(presetName);
    }

    public void setQuantizationLevel(int quantizationLevel) { mCurrentQuantizationLevels.setText(String.format(Locale.ENGLISH, "%d", quantizationLevel)); }

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
