package cofproject.tau.android.cof;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Uri on 31/10/2017.
 */

public class PostFilteringButtonsFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.result_buttons_fragment, container, false);
        return view;
    }
}