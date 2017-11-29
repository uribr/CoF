package cofproject.tau.android.cof;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * Created by Uri on 29/11/2017.
 */

public class StringNameWatcher implements TextWatcher
{
    private EditText mTarget;

    public StringNameWatcher(EditText target)
    {
        super();
        mTarget = target;
    }

    private boolean isNameValid(String str)
    {
        boolean atLeastOneChar = false;
        CharacterIterator cI = new StringCharacterIterator(str);
        for (char c = cI.first(); c != CharacterIterator.DONE; c = cI.next())
        {
            if(Character.isAlphabetic(c)) { atLeastOneChar = true; }
            else if(!Character.isDigit(c)) { return false; }
        }
        return atLeastOneChar;
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
        if(!isNameValid(s.toString()))
        {
            mTarget.setError("Preset name must be alphanumeric and contain at least one letter.");
        }
        else { mTarget.setError(null); }
    }
}
