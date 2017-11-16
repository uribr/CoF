package cofproject.tau.android.cof;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Created by Uri on 07/11/2017.
 */

public class IntegerParameterWatcher implements TextWatcher
{
    private Integer mLimiter;
    private String mNegativeMessage;
    private String mAboveLimiterMessage;
    private String mExceptionMessage;
    private String mFinallyMessage;
    private EditText mTarget;

    public IntegerParameterWatcher(int limiter, String negativeMessage, String aboveLimiterMessage, String exceptionMessage, String finallyMessage, EditText target)
    {
        super();
        mLimiter = limiter;
        mNegativeMessage = negativeMessage;
        mAboveLimiterMessage = aboveLimiterMessage;
        mExceptionMessage = exceptionMessage;
        mFinallyMessage = finallyMessage;
        mTarget = target;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s)
    {
        StringBuilder stringBuilder = new StringBuilder(mFinallyMessage);
        if(s.length() == 0)
        {
            return;
        }
        try
        {
            Integer num = Integer.parseInt(s.toString());
            if (num < 0) { stringBuilder.append(mNegativeMessage); }
            else if (num >= mLimiter)
            {
                stringBuilder.append(mAboveLimiterMessage);
            }
        }
        catch (Exception ex)
        {
            stringBuilder.append(mExceptionMessage);
        }
        finally
        {
            if (stringBuilder.length() > mFinallyMessage.length()) { mTarget.setError(stringBuilder.toString());}
            else { mTarget.setError(null); }
        }
    }
}
