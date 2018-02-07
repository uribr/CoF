package cofproject.tau.android.cof;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ButtonsFragment extends Fragment {

    public static final String BUTTONS_FRAGMENT_KEY = "KEY";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (container != null) {
            container.removeAllViews();
        }
        int layoutId = getArguments().getInt(BUTTONS_FRAGMENT_KEY);
        return inflater.inflate(layoutId, container, false);
    }

    public static ButtonsFragment newInstance(int layoutId) {
        ButtonsFragment f = new ButtonsFragment();
        Bundle args = new Bundle();
        args.putInt(BUTTONS_FRAGMENT_KEY, layoutId);
        f.setArguments(args);
        return f;
    }


}
