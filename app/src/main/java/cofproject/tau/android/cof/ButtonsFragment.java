package cofproject.tau.android.cof;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;


public class ButtonsFragment extends Fragment {

    public static final String BUTTONS_FRAGMENT_KEY = "KEY";

    private static final String TAG = "ButtonsFragment";
    private View mView;
    private int mLayoudId;
    private ButtonsFragmentListener mListener;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ButtonsFragmentListener) context;
        } catch (final  ClassCastException e) {
            Log.e(TAG, "onAttach: " + context.toString() +
                    " - must implement listener", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (container != null) {
            container.removeAllViews();
        }
        mLayoudId = getArguments().getInt(BUTTONS_FRAGMENT_KEY);
        mView =  inflater.inflate(mLayoudId, container, false);
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();

        switch (mLayoudId) {
            case R.layout.pre_filtering_buttons_fragment:
                SwitchCompat switchCompat = mView.findViewById(R.id.scribble_switch);
                mListener.configSwitch(switchCompat);
                break;
            case R.layout.scribble_mask_threshold_fragment:
                SeekBar seekBar = mView.findViewById(R.id.scribble_threshold_seekbar);
                mListener.configSeekBar(seekBar);
                break;
        }
    }



    public static ButtonsFragment newInstance(int layoutId) {
        ButtonsFragment f = new ButtonsFragment();
        Bundle args = new Bundle();
        args.putInt(BUTTONS_FRAGMENT_KEY, layoutId);
        f.setArguments(args);
        return f;
    }

    public interface ButtonsFragmentListener{
        void configSeekBar(SeekBar seekBar);
        void configSwitch(SwitchCompat switchCompat);
    }


}
