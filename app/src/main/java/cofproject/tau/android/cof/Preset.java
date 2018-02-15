package cofproject.tau.android.cof;

import java.util.HashMap;
import java.util.Map;

import static cofproject.tau.android.cof.Utilities.FILT_SIGMA;
import static cofproject.tau.android.cof.Utilities.FILT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.FILT_WINDOW_SIZE_FB;
import static cofproject.tau.android.cof.Utilities.IS_RELATIVE;
import static cofproject.tau.android.cof.Utilities.ITERATIONS;
import static cofproject.tau.android.cof.Utilities.ITERATIONS_FB;
import static cofproject.tau.android.cof.Utilities.MAX_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utilities.MAX_SIGMA;
import static cofproject.tau.android.cof.Utilities.MIN_QUANTIZATION_LEVEL;
import static cofproject.tau.android.cof.Utilities.QUANTIZATION;
import static cofproject.tau.android.cof.Utilities.RELATIVE_FILT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.RELATIVE_FILT_WINDOW_SIZE_FB;
import static cofproject.tau.android.cof.Utilities.RELATIVE_STAT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.STAT_SIGMA;
import static cofproject.tau.android.cof.Utilities.STAT_WINDOW_SIZE;

public class Preset
{
    private static final String TAG = "Preset";

    private String mName;
    private double mRelativeStatWindowSize;
    private boolean mRelative;
    // general
    private int mQuantizationLevels;
    //CoF
    private double mRelativeFiltWindowSize;
    private int mStatWindowSize;
    private double mStatSigma;
    private int mFiltWindowSize;
    private double mFiltSigma;
    private int mNumberOfIteration;

    //FB-CoF
    private int mFiltWindowSizeFB;
    private double mRelativeFiltWindowSizeFB;
    private int mNumberOfIterationFB;

    //Scribble cosmetics
//    private Integer mScribbleWidth;
//    private Object mScribbleColor;

//    public Preset(String name, float statSigma, int numberOfIteration, int statWindowSize,
//                  boolean relative, int quantization)
//    {
//        mName = name;
//        mRelative = relative;
//        mQuantizationLevels = quantization;
//        mStatWindowSize = statWindowSize;
//        mStatSigma = statSigma;
//        mNumberOfIteration = numberOfIteration;
//    }

    public Preset(String name, boolean relative, int imgSize, int quantizationLevels,
                  int statWindowSize, double statSigma, int filtWindowSize, double filtSigma,
                  int numberOfIteration, int filtWindowSizeFB, int numberOfIterationFB/*, int scribbleWidth, Object scribbleColor*/)
    {
        mName = name;
        mRelative = relative;
        setRelativeWindowSize(statWindowSize, filtWindowSize, filtWindowSizeFB, imgSize);
        setSigma(statSigma, filtSigma);
        setQuantizationLevel(quantizationLevels);
        setWindowSize(statWindowSize, filtWindowSizeFB, filtWindowSize, imgSize);
        mNumberOfIteration = numberOfIteration;
        mNumberOfIterationFB = numberOfIterationFB;
    }

    public Preset(String name, Map<String, String> map)
    {
        // General Parameters
        mName = name;
        mRelative = Boolean.parseBoolean(map.get(IS_RELATIVE));
        mQuantizationLevels = Integer.parseInt(map.get(QUANTIZATION));

        // CoF Parameters
        mStatWindowSize = Integer.parseInt(map.get(STAT_WINDOW_SIZE));
        mRelativeStatWindowSize = Double.parseDouble(map.get(RELATIVE_STAT_WINDOW_SIZE));
        mStatSigma = Double.parseDouble(map.get(STAT_SIGMA));

        mFiltWindowSize = Integer.parseInt(map.get(FILT_WINDOW_SIZE));
        mRelativeFiltWindowSize = Double.parseDouble(map.get(RELATIVE_FILT_WINDOW_SIZE));
        mFiltSigma = Double.parseDouble(map.get(FILT_SIGMA));
        mNumberOfIteration = Integer.parseInt(map.get(ITERATIONS));

        // FB-CoF Parameters
        mFiltWindowSizeFB = Integer.parseInt(map.get(FILT_WINDOW_SIZE_FB));
        mRelativeFiltWindowSizeFB = Double.parseDouble(map.get(RELATIVE_FILT_WINDOW_SIZE_FB));
        mNumberOfIterationFB = Integer.parseInt(map.get(ITERATIONS_FB));

        // Scribble costmetics parameters
//        mScribbleWidth = Integer.parseInt(map.get(SCRIBBLE_WIDTH));
//        mScribbleColor = map.get(SCRIBBLE_COLOR);
    }

    public boolean validate()
    {
        return !(mStatWindowSize <= 0 ||
                mStatSigma == 0 ||
                mNumberOfIteration == 0 ||
                mName.isEmpty() ||
                (mRelativeStatWindowSize == 0.0 && mRelative));
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

    private void setRelativeWindowSize(int statWindowSize, int filtWindowSize, int filtWindowSizeFB, int imageSize)
    {
        if (statWindowSize <= imageSize && statWindowSize > 0)
        {
            this.mRelativeStatWindowSize = (double) (statWindowSize) / (double) (imageSize);
        }
        if (filtWindowSize <= imageSize && filtWindowSize > 0)
        {
            this.mRelativeFiltWindowSize = (double) (filtWindowSize) / (double) (imageSize);
        }
        if (filtWindowSizeFB <= imageSize && filtWindowSizeFB > 0)
        {
            this.mRelativeFiltWindowSizeFB = (double) (filtWindowSizeFB) / (double) (imageSize);
        }
    }

    private Double getRelativeStatWindowSize()
    {
        return mRelativeStatWindowSize;
    }

    private Double getRelativeFiltWindowSize()
    {
        return mRelativeFiltWindowSize;
    }

    private Double getRelativeFiltWindowSizeFB()
    {
        return mRelativeFiltWindowSizeFB;
    }

    public Integer getStatWindowSize(int size)
    {
        if (mRelative || mStatWindowSize > size)
        {
            int res = (int) (mRelativeStatWindowSize * (double) (size));
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

    public Integer getFiltWindowSize(int size)
    {
        if (mRelative || mFiltWindowSize > size)
        {
            int res = (int) (mRelativeFiltWindowSize * (double) (size));
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
            return mFiltWindowSize;
        }
    }

    public Integer getFiltWindowSizeFB(int size)
    {
        if (mRelative || mFiltWindowSizeFB > size)
        {
            int res = (int) (mRelativeFiltWindowSizeFB * (double) (size));
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
            return mFiltWindowSizeFB;
        }
    }

    private Integer getStatWindowSize()
    {
        return mStatWindowSize;
    }

    private Integer getFiltWindowSize()
    {
        return mFiltWindowSize;
    }

    private Integer getFiltWindowSizeFB()
    {
        return mFiltWindowSizeFB;
    }

    private void setWindowSize(int statWindowSize, int filtWindowSizeFB, int filtWindowSize, int imgSize)
    {
        if (statWindowSize <= imgSize && statWindowSize > 0)
        {
            mStatWindowSize = statWindowSize;
        }
        if (filtWindowSize <= imgSize && filtWindowSize > 0)
        {
            mFiltWindowSize = filtWindowSize;
        }
        if (filtWindowSizeFB <= imgSize && filtWindowSizeFB > 0)
        {
            mFiltWindowSizeFB = filtWindowSizeFB;
        }
    }

    public Double getStatSigma()
    {
        return mStatSigma;
    }

    public Double getFiltSigma()
    {
        return mFiltSigma;
    }

    private void setSigma(double statSigma, double filtSigma)
    {
        if (statSigma <= MAX_SIGMA && statSigma > 0)
        {
            this.mStatSigma = statSigma;
        }
        if (filtSigma <= MAX_SIGMA && filtSigma > 0)
        {
            this.mFiltSigma = filtSigma;
        }
    }

    public Integer getNumberOfIteration()
    {
        return mNumberOfIteration;
    }

    public Integer getNumberOfIterationFB()
    {
        return mNumberOfIterationFB;
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

//    public Integer getScribbleWidth()
//    {
//        return mScribbleWidth;
//    }
//
//    public Object getScribbleColor()
//    {
//        return mScribbleColor;
//    }


    public Map<String, String> presetToMap()
    {
        Map<String, String> map = new HashMap<>();
        map.put(STAT_WINDOW_SIZE, getStatWindowSize().toString());
        map.put(RELATIVE_STAT_WINDOW_SIZE, getRelativeStatWindowSize().toString());
        map.put(STAT_SIGMA, getStatSigma().toString());
        map.put(FILT_WINDOW_SIZE, getFiltWindowSize().toString());
        map.put(RELATIVE_FILT_WINDOW_SIZE, getRelativeFiltWindowSize().toString());
        map.put(FILT_SIGMA, getFiltSigma().toString());
        map.put(ITERATIONS, getNumberOfIteration().toString());
        map.put(FILT_WINDOW_SIZE_FB, getFiltWindowSizeFB().toString());
        map.put(RELATIVE_FILT_WINDOW_SIZE_FB, getRelativeFiltWindowSizeFB().toString());
        map.put(ITERATIONS_FB, getNumberOfIterationFB().toString());
        map.put(QUANTIZATION, getQuantization().toString());
        map.put(IS_RELATIVE, isRelative().toString());
//        map.put(SCRIBBLE_WIDTH, getScribbleWidth());
//        map.put(SCRIBBLE_COLOR, getScribbleColor());
        return map;
    }



}
