package cofproject.tau.android.cof;

import android.content.SharedPreferences;

/**
 * Created by Uri on 27/11/2017.
 */

public class Preset
{
    private static final int PARAMETER_LIMIT = 8;
    private static final String DELIMITER = ",";
    private String mName = "";
    private double mRelativeWindowSize = 0.0;
    private double mSigma = 0.0;
    private int mWindowSize = 0;
    private int mNumberOfIteration = 0;
    private boolean mValid = false;
    private boolean mRelative = false;

    Preset(String name, double sigma, int numberOfIteration, int windowSize, int imageSize, boolean relative)
    {
        this.setRelativeWindowSize(windowSize, imageSize);
        this.setSigma(sigma);
        this.setNumberOfIteration(numberOfIteration);
        mWindowSize = windowSize;
        mName = name;
        mRelative = relative;

        validate(true);
    }

    Preset(String name, String params)
    {
        String[] paramsArr = params.split(DELIMITER, PARAMETER_LIMIT);
        mRelativeWindowSize = Double.parseDouble(paramsArr[0]);
        mWindowSize = Integer.parseInt(paramsArr[1]);
        mSigma = Double.parseDouble(paramsArr[2]);
        mNumberOfIteration = Integer.parseInt(paramsArr[3]);
        mValid = Boolean.parseBoolean(paramsArr[4]);
        mRelative = Boolean.parseBoolean(paramsArr[5]);
        mName = name;

        validate(true);
    }

    public void validate(boolean set)
    {
        if(mWindowSize <= 0 || mWindowSize <= 0 || mSigma == 0
                || mNumberOfIteration == 0 || mName.isEmpty()
                || (((mRelativeWindowSize == 0.0 || mRelativeWindowSize  == 0.0) && mRelative)))
        {
            mValid = false;
        }
        else if (set){ mValid = true; }
    }

    public boolean isValid()
    {
        validate(true);
        return mValid;
    }

    public void validate()
    {
        validate(false);
    }


    public String getName()
    {
        return mName;
    }

    public void setmName(String name)
    {
        if(!name.isEmpty())
        {
            this.mName = name;
        }
    }

    public void setRelativeWindowSize(int windowSize, int imageSize)
    {
        if(windowSize <= imageSize && windowSize > 0) { this.mRelativeWindowSize = (double)(windowSize)/(double)(imageSize); }
    }

    public void setRelative(boolean relative) { mRelative = relative; }

    public double getRelativeWindowSize() { return mRelativeWindowSize; }

    public int getWindowSize(int size)
    {
        if(mRelative || mWindowSize > size)
        {
            int res = (int)(mRelativeWindowSize * (double)(size));
            if (res == 0) { return ++res; }
            else if (res > size) { return --res; }
            return  res;
        }
        else { return mWindowSize; }
    }

    public int getWindowSize() { return mWindowSize; }

    public void setWindowSize(int size) { mWindowSize = size; }

    public double getSigma()
    {
        return mSigma;
    }

    public int getIntergerPartSigma()
    {
        return (int)Math.floor(mSigma);
    }

    public int getFractionalPartSigma()
    {
        return (int)((mSigma - Math.floor(mSigma))*100);
    }

    public void setSigma(double sigma)
    {
        if(sigma <= 10 && sigma > 0) { this.mSigma = sigma; }
    }

    public int getNumberOfIteration() { return mNumberOfIteration; }

    public void setNumberOfIteration(int numberOfIteration)
    {
        if(numberOfIteration <= 10 && numberOfIteration > 0)  { this.mNumberOfIteration = numberOfIteration; }
    }

    public boolean isRelative()
    {
        return mRelative;
    }



    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mRelativeWindowSize);     sb.append(',');
        sb.append(mWindowSize);       sb.append(',');
        sb.append(mSigma);              sb.append(',');
        sb.append(mNumberOfIteration);  sb.append(',');
        sb.append(mValid);              sb.append(',');
        sb.append(mRelative);
        return sb.toString();
    }

    public boolean store(SharedPreferences prefs)
    {
        if(mValid)
        {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(mName, toString());
            return editor.commit();
        }
        return false;
    }
}
