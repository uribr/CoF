package cofproject.tau.android.cof;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class FilterSettingsButtonsFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (container != null) {
            container.removeAllViews();
        }
        View view = inflater.inflate(R.layout.filter_settings_buttons_fragment, container, false);
        return view;
    }
}
