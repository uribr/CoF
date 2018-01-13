package cofproject.tau.android.cof;

import android.content.SharedPreferences;
import android.util.Log;

import static cofproject.tau.android.cof.Utility.DEFAULT_NUMBER_OF_ITERATIONS;
import static cofproject.tau.android.cof.Utility.DEFAULT_QUNTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utility.DEFAULT_SIGMA;
import static cofproject.tau.android.cof.Utility.DEFAULT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utility.MAX_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utility.MIN_QUANTIZATION_LEVEL;

/**
 * Created by Uri on 27/11/2017.
 */

public class Preset
{
    static final String DEFAULT_PRESET_NAME = "Default";
    private static final int PARAMETER_LIMIT = 8;
    private static final String DELIMITER = ",";
    private static final String TAG = "Preset";
    private String mName;
    private double mRelativeWindowSize;
    private double mSigma;
    private int mWindowSize;
    private int mQuantizationLevels;
    private int mNumberOfIteration;
    private boolean mRelative;

    Preset(String name, double sigma, int numberOfIteration, int windowSize, int imageSize, boolean relative, int quantization)
    {
        this.setRelativeWindowSize(windowSize, imageSize);
        this.setSigma(sigma);
        this.setNumberOfIteration(numberOfIteration);
        this.setQuantizationLevel(quantization);
        this.setWindowSize(windowSize, imageSize);
        mName = name;
        mRelative = relative;
    }

    Preset(String name, String params)
    {
        String[] paramsArr = params.split(DELIMITER, PARAMETER_LIMIT);
        mRelativeWindowSize = Double.parseDouble(paramsArr[0]);
        mWindowSize = Integer.parseInt(paramsArr[1]);
        mSigma = Double.parseDouble(paramsArr[2]);
        mNumberOfIteration = Integer.parseInt(paramsArr[3]);
        mRelative = Boolean.parseBoolean(paramsArr[4]);
        mName = name;
    }

    static Preset createPreset(int imgSize)
    {
        Log.d(TAG, "createPresetFromUserSettings: hardcoded default preset");
        return new Preset(DEFAULT_PRESET_NAME, DEFAULT_SIGMA, DEFAULT_NUMBER_OF_ITERATIONS,
                DEFAULT_WINDOW_SIZE, imgSize, false, DEFAULT_QUNTIZATION_LEVEL);
    }

    public boolean validate()
    {
        return !(mWindowSize <= 0 || mWindowSize <= 0 || mSigma == 0
                || mNumberOfIteration == 0 || mName.isEmpty()
                || (mRelativeWindowSize == 0.0 && mRelative));

    }



    public String getName()
    {
        return mName;
    }

    public void setName(String name)
    {
        if (name != null)
        {
            this.mName = name;
        }
    }

    public void setRelativeWindowSize(int windowSize, int imageSize)
    {
        if (windowSize <= imageSize && windowSize > 0)
        {
            this.mRelativeWindowSize = (double) (windowSize) / (double) (imageSize);
        }
    }

    public Double getRelativeWindowSize()
    {
        return mRelativeWindowSize;
    }

    public Integer getWindowSize(int size)
    {
        if (mRelative || mWindowSize > size)
        {
            int res = (int) (mRelativeWindowSize * (double) (size));
            if (res == 0)
            {
                return ++res;
            } else if (res > size)
            {
                return --res;
            }
            return res;
        } else
        {
            return mWindowSize;
        }
    }

    public Integer getWindowSize()
    {
        return mWindowSize;
    }

    public void setWindowSize(int windowSize, int imgSize)
    {
        if (windowSize <= imgSize && windowSize > 0)
        {
            mWindowSize = windowSize;
        }
    }

    public Double getSigma()
    {
        return mSigma;
    }

    public Integer getIntergerPartSigma()
    {
        return (int) Math.floor(mSigma);
    }

    public Integer getFractionalPartSigma()
    {
        return (int) ((mSigma - Math.floor(mSigma)) * 100);
    }

    public boolean setSigma(double sigma)
    {
        if (sigma <= 10 && sigma > 0)
        {
            this.mSigma = sigma;
            return true;
        }
        return false;
    }

    public Integer getNumberOfIteration()
    {
        return mNumberOfIteration;
    }

    public boolean setNumberOfIteration(int numberOfIteration)
    {
        if (numberOfIteration <= 10 && numberOfIteration > 0)
        {
            this.mNumberOfIteration = numberOfIteration;
            return true;
        }
        return false;
    }

    public Integer getQuantization()
    {
        return mQuantizationLevels;
    }

    public boolean setQuantizationLevel(int quantizationLevel)
    {
        if (quantizationLevel >= MIN_QUANTIZATION_LEVEL && quantizationLevel <= MAX_QUANTIZATION_LEVEL)
        {
            mQuantizationLevels = quantizationLevel;
            return true;
        }
        return false;
    }

    public boolean isRelative()
    {
        return mRelative;
    }

    public void setRelative(boolean relative)
    {
        mRelative = relative;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mRelativeWindowSize);
        sb.append(',');
        sb.append(mWindowSize);
        sb.append(',');
        sb.append(mSigma);
        sb.append(',');
        sb.append(mNumberOfIteration);
        sb.append(',');
        sb.append(mQuantizationLevels);
        sb.append(',');
        sb.append(mRelative);
        return sb.toString();
    }

    public boolean store(SharedPreferences prefs)
    {
        if (validate())
        {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(mName, toString());
            return editor.commit();
        }
        return false;
    }


}
