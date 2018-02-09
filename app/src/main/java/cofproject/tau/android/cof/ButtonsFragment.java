package cofproject.tau.android.cof;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

import static cofproject.tau.android.cof.Utilities.*;


public class ButtonsFragment extends Fragment {

    private static final String TAG = "ButtonsFragment";
    private static final String BUTTONS_FRAGMENT_KEY = "KEY";

    private View mView;
    private int mLayoutId;
    private ButtonsFragmentListener mListener;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            if (!(context instanceof FilterSettingsActivity)) {
                mListener = (ButtonsFragmentListener) context;
            }
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
        mLayoutId = getArguments().getInt(BUTTONS_FRAGMENT_KEY);
        mView =  inflater.inflate(mLayoutId, container, false);
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mListener != null) {
            switch (mLayoutId) {
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
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        String tutorialKey = null;
        Boolean firstTime = false;
        int numOfViews = 0;
        int titleId = 0;
        int[] viewIds = null;
        int[] textIds = null;
        List<ShowcaseViewParams> params = new ArrayList<>();

        switch (mLayoutId) {
            case R.layout.pre_filtering_buttons_fragment:
                tutorialKey = getString(R.string.pre_filtering_tutorial_key);
                titleId = R.string.pre_filtering_tutorial_title;
                numOfViews = 4;
                viewIds = new int[]{R.id.apply_filter_btn, R.id.scribble_switch, R.id.clear_scribble_btn, R.id.settings_btn};
                textIds = new int[]{R.string.apply_filter_tutorial_text, R.string.scribble_switch_tutorial_text,
                                    R.string.clear_scribble_tutorial_text, R.string.settings_button_tutorial_text};

                break;
            case R.layout.scribble_mask_threshold_fragment:
                tutorialKey = getString(R.string.scribble_mask_threshold_tutorial_key);
                titleId = R.string.scribble_mask_threshold_tutorial_title;
                numOfViews = 1;
                viewIds = new int[]{R.id.scribble_threshold_seekbar};
                textIds = new int[]{R.string.scribble_seekbar_tutorial};
                break;
            case R.layout.filter_settings_buttons_fragment:
                tutorialKey = getString(R.string.filter_settings_tutorial_key);
                titleId = R.string.filter_settings_tutorial_title;
                numOfViews = 2;
                viewIds = new int[]{R.id.save_preset_btn, R.id.delete_preset_btn};
                textIds = new int[]{R.string.save_preset_btn_tutorial, R.string.delete_preset_btn_tutorial};
                break;
            case R.layout.post_filtering_buttons_fragment:
                tutorialKey = getString(R.string.post_filtering_tutorial_key);
                titleId = R.string.post_filtering_tutorial_title;
                numOfViews = 1;
                viewIds = new int[]{R.id.post_filtering_buttons_layout};
                textIds = new int[]{R.string.share_or_save_tutorial};
                break;

        }
        if (tutorialKey != null) {
            firstTime = preferences.getBoolean(tutorialKey, true);
        }
        if (firstTime) {
            for (int i = 0; i < numOfViews; i++) {
                params.add(new ShowcaseViewParams(viewIds[i], textIds[i]));
            }
            showTutorial(getActivity(), titleId, numOfViews, params);
            preferences.edit().putBoolean(tutorialKey, false).apply();
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
