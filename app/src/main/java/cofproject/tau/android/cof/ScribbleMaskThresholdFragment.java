package cofproject.tau.android.cof;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;


public class ScribbleMaskThresholdFragment extends Fragment {


    protected View mView;
    private SeekBar mSeekBar;
    private OnFinishedCreateView mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try
        {
            this.mListener = (OnFinishedCreateView) context;
        } catch (final ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnFinishedCreateView");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (container != null ){
            container.removeAllViews();
        }
        mView = inflater.inflate(R.layout.scribble_mask_threshold_fragment, container, false);
        mSeekBar = mView.findViewById(R.id.scribble_threshold_seekbar);
        mListener.configSeekBar(mSeekBar);
        return mView;
    }

    @Nullable
    @Override
    public View getView() {
        return mView;
    }

    public interface OnFinishedCreateView {
        void configSeekBar(SeekBar seekBar);
    }

}
