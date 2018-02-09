package cofproject.tau.android.cof;

import java.util.HashMap;
import java.util.Map;

import static cofproject.tau.android.cof.Utilities.IS_RELATIVE;
import static cofproject.tau.android.cof.Utilities.ITERATIONS;
import static cofproject.tau.android.cof.Utilities.MAX_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utilities.MAX_SIGMA;
import static cofproject.tau.android.cof.Utilities.MIN_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utilities.QUANTIZATION;
import static cofproject.tau.android.cof.Utilities.RELATIVE_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.STAT_SIGMA;
import static cofproject.tau.android.cof.Utilities.STAT_WINDOW_SIZE;

public class Preset
{
    static final String DEFAULT_PRESET_NAME = "Default";
    private static final String TAG = "Preset";

    private String mName;
    private double mRelativeWindowSize;
    private boolean mRelative;
    // general
    private int mQuantizationLevels;
    //CoF
    private int mStatWindowSize;
    private double mStatSigma;
    //private int mFiltWindowSize;
    //private double mFiltSigma;

    private int mNumberOfIteration;
    //FB-CoF
    //private int mFiltWindowSizeFB;
    //private int mNumberOfIterationFB;


    Preset(String name, float statSigma, int numberOfIteration, int statWindowSize, int imageSize, boolean relative, int quantization)
    {
        this.setRelativeWindowSize(statWindowSize, imageSize);
        this.setSigma(statSigma);
        this.setNumberOfIteration(numberOfIteration);
        this.setQuantizationLevel(quantization);
        this.setWindowSize(statWindowSize, imageSize);
        mName = name;
        mRelative = relative;
    }

    Preset(String name, float statSigma, int numberOfIteration, int statWindowSize, boolean relative, int quantization)
    {
        mName = name;
        mRelative = relative;
        mQuantizationLevels = quantization;
        mStatWindowSize = statWindowSize;
        mStatSigma = statSigma;
        mNumberOfIteration = numberOfIteration;
    }

    //todo = uncomment
//    public Preset(String name, boolean relative, int quantizationLevels, int statWindowSize, double statSigma, int filtWindowSize, double filtSigma, int numberOfIteration, int filtWindowSizeFB, int numberOfIterationFB) {
//        mName = name;
//        mRelative = relative;
//        mQuantizationLevels = quantizationLevels;
//        mStatWindowSize = statWindowSize;
//        mStatSigma = statSigma;
//        mFiltWindowSize = filtWindowSize;
//        mFiltSigma = filtSigma;
//        mNumberOfIteration = numberOfIteration;
//        mFiltWindowSizeFB = filtWindowSizeFB;
//        mNumberOfIterationFB = numberOfIterationFB;
//    }

    Preset(String name, Map<String, String> map)
    {
        mName = name;
        mRelative = Boolean.parseBoolean(map.get(IS_RELATIVE));
        mRelativeWindowSize = Double.parseDouble(map.get(RELATIVE_WINDOW_SIZE));

        mQuantizationLevels = Integer.parseInt(map.get(QUANTIZATION));
        mStatWindowSize = Integer.parseInt(map.get(STAT_WINDOW_SIZE));
        mStatSigma = Double.parseDouble(map.get(STAT_SIGMA));
        mNumberOfIteration = Integer.parseInt(map.get(ITERATIONS));



    }

//    Preset(String name, String params)
//    {
//        String[] paramsArr = params.split(DELIMITER, PARAMETER_LIMIT);
//        mRelativeWindowSize = Double.parseDouble(paramsArr[0]);
//        mStatWindowSize = Integer.parseInt(paramsArr[1]);
//        mStatSigma = Double.parseDouble(paramsArr[2]);
//        mNumberOfIteration = Integer.parseInt(paramsArr[3]);
//        mQuantizationLevels = Integer.parseInt(paramsArr[4]);
//        mRelative = Boolean.parseBoolean(paramsArr[5]);
//        mName = name;
//    }

//    static Preset createPreset(int imgSize)
//    {
//        Log.d(TAG, "createPresetFromUserSettings: hardcoded default preset");
//        return new Preset(DEFAULT_PRESET_NAME, DEFAULT_SIGMA, DEFAULT_NUMBER_OF_ITERATIONS,
//                DEFAULT_WINDOW_SIZE, imgSize, false, DEFAULT_QUNTIZATION_LEVEL);
//    }

    public boolean validate()
    {
        return !(mStatWindowSize <= 0 ||
                mStatSigma == 0 ||
                mNumberOfIteration == 0 ||
                mName.isEmpty() ||
                (mRelativeWindowSize == 0.0 && mRelative));

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

    private void setRelativeWindowSize(int windowSize, int imageSize)
    {
        if (windowSize <= imageSize && windowSize > 0)
        {
            this.mRelativeWindowSize = (double) (windowSize) / (double) (imageSize);
        }
    }

    private Double getRelativeWindowSize()
    {
        return mRelativeWindowSize;
    }

    public Integer getWindowSize(int size)
    {
        if (mRelative || mStatWindowSize > size)
        {
            int res = (int) (mRelativeWindowSize * (double) (size));
            if (res == 0)
            {
                return ++res;
            }
            else if (res > size)
            {
                return --res;
            }
            return res;
        }
        else
        {
            return mStatWindowSize;
        }
    }

    public Integer getStatWindowSize()
    {
        return mStatWindowSize;
    }

    private void setWindowSize(int windowSize, int imgSize)
    {
        if (windowSize <= imgSize && windowSize > 0)
        {
            mStatWindowSize = windowSize;
        }
    }

    public Double getStatSigma()
    {
        return mStatSigma;
    }

//    public Integer getIntergerPartSigma()
//    {
//        return (int) Math.floor(mStatSigma);
//    }
//
//    public Integer getFractionalPartSigma()
//    {
//        return (int) ((mStatSigma - Math.floor(mStatSigma)) * 100);
//    }

    private boolean setSigma(double sigma)
    {
        if (sigma <= MAX_SIGMA && sigma > 0)
        {
            this.mStatSigma = sigma;
            return true;
        }
        return false;
    }

    public Integer getNumberOfIteration()
    {
        return mNumberOfIteration;
    }

    private boolean setNumberOfIteration(int numberOfIteration)
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

    private boolean setQuantizationLevel(int quantizationLevel)
    {
        if (quantizationLevel >= MIN_QUANTIZATION_LEVEL && quantizationLevel <= MAX_QUANTIZATION_LEVEL)
        {
            mQuantizationLevels = quantizationLevel;
            return true;
        }
        return false;
    }

    public Boolean isRelative()
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

        return String.valueOf(mRelativeWindowSize) +
                ',' +
                mStatWindowSize +
                ',' +
                mStatSigma +
                ',' +
                mNumberOfIteration +
                ',' +
                mQuantizationLevels +
                ',' +
                mRelative;
    }


    public Map<String, String> presetToMap()
    {
        Map<String, String> map = new HashMap<>();
        map.put(STAT_WINDOW_SIZE, getStatWindowSize().toString());
        map.put(STAT_SIGMA, getStatSigma().toString());
        map.put(ITERATIONS, getNumberOfIteration().toString());
        map.put(QUANTIZATION, getQuantization().toString());
        map.put(RELATIVE_WINDOW_SIZE, getRelativeWindowSize().toString());
        map.put(IS_RELATIVE, isRelative().toString());
        return map;
    }



}
