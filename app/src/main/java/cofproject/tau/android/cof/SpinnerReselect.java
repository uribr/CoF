// Class is taken from: http://androidtechnicalblog.blogspot.co.il/2014/05/spinner-with-re-select-functionality.html
// with minor modification.
package cofproject.tau.android.cof;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Spinner;

import java.lang.reflect.Field;


@SuppressLint("AppCompatCustomView")
public class SpinnerReselect extends Spinner
{

    public SpinnerReselect(Context context)
    {
        super(context);
    }

    public SpinnerReselect(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public SpinnerReselect(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void setSelection(int position, boolean animate)
    {
        ignoreOldSelectionByReflection();
        super.setSelection(position, animate);
    }

    private void ignoreOldSelectionByReflection()
    {
        try
        {
            Class<?> c = this.getClass().getSuperclass().getSuperclass().getSuperclass();
            Field reqField = c.getDeclaredField("mOldSelectedPosition");
            reqField.setAccessible(true);
            reqField.setInt(this, -1);
        } catch (Exception e)
        {
            Log.d("Exception Private", "ex", e);

        }
    }

    @Override
    public void setSelection(int position)
    {
        ignoreOldSelectionByReflection();
        super.setSelection(position);
    }
}