package cofproject.tau.android.cof;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/**
 * Created by Uri on 31/10/2017.
 */

public class PreFilteringButtonsFragment extends Fragment
{
    protected  View mView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mView = inflater.inflate(R.layout.pre_filtering_buttons_fragment, container, false);
        return mView;
    }

    public boolean isScribbleOn()
    {
        return ((Switch)mView.findViewById(R.id.scribble_switch)).isChecked();
    }

}
