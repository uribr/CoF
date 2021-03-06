package cofproject.tau.android.cof;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;

@SuppressWarnings("WeakerAccess")
public class StringNameWatcher implements TextWatcher
{
    private final EditText mTarget;
    private final CheckBox mCheckBox;

    public StringNameWatcher(EditText target, CheckBox checkBox)
    {
        super();
        mTarget = target;
        mCheckBox = checkBox;
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {

    }

    @Override
    public void afterTextChanged(Editable s)
    {
        if (!Utilities.isNameValid(s.toString(), mCheckBox.isChecked()))
        {
            mTarget.setError("Preset name must be alphanumeric and contain at least one letter.");
        }
        else
        {
            mTarget.setError(null);
        }
    }
}
