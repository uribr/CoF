package cofproject.tau.android.cof;

import android.content.SharedPreferences;

import java.util.StringTokenizer;

/**
 * Created by Uri on 27/11/2017.
 */

public class Preset
{
    private static final int PARAMETER_LIMIT = 8;
    private static final String DELIMITER = ",";
    private String mName = "";
    private double mRelativeHeight = 0.0;
    private double mRelativeWidth = 0.0;
    private double mSigma = 0.0;
    private int mActualHeight = 0;
    private int mActualtWidth = 0;
    private int mNumberOfIteration = 0;
    private boolean mValid = false;
    private boolean mRelative = false;

    Preset(String name, double sigma, int numberOfIteration, int windowWidth, int imageWidth, int windowHeight, int imageHeight, boolean relative)
    {
        this.setRelativeHeight(windowHeight, imageHeight);
        this.setRelativeWidth(windowWidth, imageWidth);
        this.setSigma(sigma);
        this.setNumberOfIteration(numberOfIteration);
        mActualHeight = windowHeight;
        mActualtWidth = windowWidth;
        mName = name;
        mRelative = relative;

        validate(true);
    }

    Preset(String name, String params)
    {
        String[] paramsArr = params.split(DELIMITER, PARAMETER_LIMIT);
        mRelativeHeight = Double.parseDouble(paramsArr[0]);
        mRelativeWidth = Double.parseDouble(paramsArr[1]);
        mActualHeight = Integer.parseInt(paramsArr[2]);
        mActualtWidth = Integer.parseInt(paramsArr[3]);
        mSigma = Double.parseDouble(paramsArr[4]);
        mNumberOfIteration = Integer.parseInt(paramsArr[5]);
        mValid = Boolean.parseBoolean(paramsArr[6]);
        mRelative = Boolean.parseBoolean(paramsArr[7]);
        mName = name;

        validate(true);
    }

    public void validate(boolean set)
    {
        if(mActualtWidth <= 0 || mActualHeight <= 0 || mSigma == 0
                || mNumberOfIteration == 0 || mName.isEmpty()
                || (((mRelativeWidth == 0.0 || mRelativeWidth  == 0.0) && mRelative)))
        {
            mValid = false;
        }
        else if (set){ mValid = true; }
    }

    public boolean isValid() { return mValid; }

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

    public double getRelativeHeight() { return mRelativeHeight; }

    public void setRelativeHeight(int windowHeight, int imageHeight)
    {
        if(windowHeight <= imageHeight && windowHeight > 0) { this.mRelativeHeight = (double)(windowHeight)/(double)(imageHeight); }
    }

    public double getRelativeWidth() { return mRelativeWidth; }

    public int getWidth(int width)
    {
        if(mRelative || mActualtWidth > width)
        {
            int res = (int)(mRelativeWidth * (double)(width));
            if (res == 0) { return ++res; }
            else if (res > width) { return --res; }
            return  res;
        }
        else { return mActualtWidth; }
    }

    public int getHeight(int height)
    {
        if(mRelative || mActualHeight > height)
        {
            int res = (int)(mRelativeHeight * (double)(height));
            if (res == 0) { return ++res; }
            else if (res > height) { return --res; }
            return  res;
        }
        else { return mActualHeight; }
    }

    public void setRelativeWidth(int windowWidth, int imageWidth)
    {
        if(windowWidth <= imageWidth && windowWidth > 0) { this.mRelativeWidth = (double)(windowWidth)/(double)(imageWidth); }
    }

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
        sb.append(mRelativeHeight);     sb.append(',');
        sb.append(mRelativeWidth);      sb.append(',');
        sb.append(mActualHeight);       sb.append(',');
        sb.append(mActualtWidth);       sb.append(',');
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
