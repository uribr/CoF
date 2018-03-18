package cofproject.tau.android.cof;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.SeekBar;

import java.util.Locale;

import static cofproject.tau.android.cof.Utilities.DEFAULT_SIGMA;
import static cofproject.tau.android.cof.Utilities.MAX_SIGMA;
import static cofproject.tau.android.cof.Utilities.ZERO_SIGMA;
import static cofproject.tau.android.cof.Utilities.mapSigmaToProgress;


@SuppressWarnings("WeakerAccess")
public class SigmaValueWatcher implements TextWatcher
{
    private Float currentVal;
    private final SeekBar mSigmaSeekBar;
    private final EditText mTarget;

    SigmaValueWatcher (EditText target, SeekBar sigmaSeekBar)
    {
        currentVal = DEFAULT_SIGMA;
        mTarget = target;
        mSigmaSeekBar = sigmaSeekBar;
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        try
        {
            currentVal = Float.parseFloat(s.toString());
        }
        catch (NumberFormatException numberFormatEx)
        {
            mTarget.setError("σ can only be a floating point number.");
        }}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s)
    {
        Float newValue;
        Integer seekBarProgress;
        boolean needToSetString = false;
        try
        {
            newValue = Float.parseFloat(s.toString());
            if (currentVal.equals(newValue))
            {
                return;
            }
            if (newValue > MAX_SIGMA)
            {
                seekBarProgress = mapSigmaToProgress(MAX_SIGMA);
                newValue = MAX_SIGMA;
                needToSetString = true;
            }
            else if (newValue < 0)
            {
                seekBarProgress = mapSigmaToProgress(ZERO_SIGMA);
                newValue = ZERO_SIGMA;
                needToSetString = true;
            }
            else
            {
                seekBarProgress = mapSigmaToProgress(newValue);
            }
            currentVal = newValue;

            if (needToSetString)
            {
                String str = String.format(Locale.ENGLISH,"%04.02f", newValue);
                mTarget.setText(str);
            }

            if (mSigmaSeekBar.getProgress() != seekBarProgress)
            {
                mSigmaSeekBar.setProgress(seekBarProgress);
            }

        }
        catch (Exception ex)
        {
            mTarget.setError("σ can only be a floating point number.");
        }
//        catch (NumberFormatException numberFormatEx)
//        {
//            mTarget.setError("σ can only be a floating point number.");
//            String str = Double.toString(currentVal);
//            s.replace(0, str.length()-1, str);
//        }
//        catch (IndexOutOfBoundsException ex)
//        {
//            if (ex.getMessage().contains("setSpan".subSequence(0, 6)))
//            {
//
//            }
//        }

    }
}
