package cofproject.tau.android.cof;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Uri on 31/10/2017.
 */

public class InFilterButtonsFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.in_filtering_button_fragment, container, false);
        return view;
    }
}
