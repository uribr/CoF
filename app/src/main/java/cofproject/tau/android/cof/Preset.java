package cofproject.tau.android.cof;

import android.util.Log;

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
import static cofproject.tau.android.cof.Utilities.PRESET_NAME;
import static cofproject.tau.android.cof.Utilities.QUANTIZATION;
import static cofproject.tau.android.cof.Utilities.RELATIVE_FILT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.RELATIVE_FILT_WINDOW_SIZE_FB;
import static cofproject.tau.android.cof.Utilities.RELATIVE_STAT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.RELATIVE_STAT_WINDOW_SIZE_FB;
import static cofproject.tau.android.cof.Utilities.STAT_SIGMA;
import static cofproject.tau.android.cof.Utilities.STAT_SIGMA_FB;
import static cofproject.tau.android.cof.Utilities.STAT_WINDOW_SIZE;
import static cofproject.tau.android.cof.Utilities.STAT_WINDOW_SIZE_FB;

@SuppressWarnings("WeakerAccess")
public class Preset
{
    private static final String TAG = "Preset";

    private final String mName;
    private double mRelativeStatWindowSize;
    private final boolean mRelative;
    // general
    private int mQuantizationLevels;
    //CoF
    private double mRelativeFiltWindowSize;
    private int mStatWindowSize;
    private double mStatSigma;
    private int mFiltWindowSize;
    private double mFiltSigma;
    private final int mNumberOfIteration;

    //FB-CoF
    private int mFiltWindowSizeFB;
    private double mRelativeFiltWindowSizeFB;
    private double mStatSigmaFB;
    private int mStatWindowSizeFB;
    private double mRelativeStatWindowSizeFB;
    private final int mNumberOfIterationFB;


    public Preset(String name, Map<String, String> map, Integer imgSize)
    {
        Log.i(TAG, "Preset: Initializing preset");
        mName = map.containsKey(PRESET_NAME) ? map.get(PRESET_NAME) : name;
        mRelative = Boolean.parseBoolean(map.get(IS_RELATIVE));
        setQuantizationLevel(Integer.parseInt(map.get(QUANTIZATION)));

        mNumberOfIteration = Integer.parseInt(map.get(ITERATIONS));
        mNumberOfIterationFB = Integer.parseInt(map.get(ITERATIONS_FB));

        this.setSigma(Double.parseDouble(map.get(STAT_SIGMA)), Double.parseDouble(map.get(FILT_SIGMA)),
                Double.parseDouble(map.get(STAT_SIGMA_FB)));
        if (imgSize != null)
        {
            setWindowSize(Integer.parseInt(map.get(STAT_WINDOW_SIZE)), Integer.parseInt(map.get(FILT_WINDOW_SIZE_FB)), Integer.parseInt(map.get(FILT_WINDOW_SIZE)), Integer.parseInt(map.get(STAT_WINDOW_SIZE_FB)), imgSize);

            if (map.containsKey(IS_RELATIVE))
            {
                if(map.containsKey(RELATIVE_FILT_WINDOW_SIZE_FB) &&
                        map.containsKey(RELATIVE_FILT_WINDOW_SIZE) &&
                        map.containsKey(RELATIVE_STAT_WINDOW_SIZE_FB) &&
                        map.containsKey(RELATIVE_STAT_WINDOW_SIZE))
                {
                    mRelativeStatWindowSize = Double.parseDouble(map.get(RELATIVE_STAT_WINDOW_SIZE));
                    mRelativeFiltWindowSize = Double.parseDouble(map.get(RELATIVE_FILT_WINDOW_SIZE));
                    mRelativeFiltWindowSizeFB = Double.parseDouble(map.get(RELATIVE_FILT_WINDOW_SIZE_FB));
                    mRelativeStatWindowSizeFB = Double.parseDouble(map.get(RELATIVE_STAT_WINDOW_SIZE_FB));
                }
                else
                {
                    setRelativeWindowSize(mStatWindowSize, mFiltWindowSize, mFiltWindowSizeFB, imgSize, mStatWindowSizeFB);
                }
            }
        }

        else
        {
            mStatWindowSize = Integer.parseInt(map.get(STAT_WINDOW_SIZE));
            mFiltWindowSize = Integer.parseInt(map.get(FILT_WINDOW_SIZE));
            mFiltWindowSizeFB = Integer.parseInt(map.get(FILT_WINDOW_SIZE_FB));
            mStatWindowSizeFB = Integer.parseInt(map.get(STAT_WINDOW_SIZE_FB));
        }
    }

    public String getName()
    {
        return mName;
    }

    private void setRelativeWindowSize(int statWindowSize, int filtWindowSize, int filtWindowSizeFB, int imageSize, int statWindowSizeFB)
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
        if (statWindowSizeFB <= imageSize && statWindowSizeFB > 0)
        {
            this.mRelativeStatWindowSizeFB = (double) (statWindowSizeFB) / (double) (imageSize);
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
        return computeWindowSize(size, mStatWindowSize, mRelativeStatWindowSize);
    }

    public Integer getFiltWindowSize(int size)
    {
        return computeWindowSize(size, mFiltWindowSize, mRelativeFiltWindowSize);
    }

    public Integer getFiltWindowSizeFB(int size)
    {
        return computeWindowSize(size, mFiltWindowSizeFB, mRelativeFiltWindowSizeFB);
    }

    public Integer getStatWindowSizeFB(int size)
    {
        return computeWindowSize(size, mStatWindowSizeFB, mRelativeStatWindowSizeFB);
    }

    private Integer computeWindowSize(int size, int windowSize, double relativeSize)
    {
        if (mRelative || windowSize > size)
        {
            double tmp = relativeSize * size;
            int res = (int) (tmp);
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
            return windowSize;
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

    private void setWindowSize(int statWindowSize, int filtWindowSizeFB, int filtWindowSize, int statWindowSizeFB, int imgSize)
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
        if (statWindowSizeFB <= imgSize && statWindowSizeFB > 0)
        {
            mStatWindowSizeFB = statWindowSizeFB;
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

    private void setSigma(double statSigma, double filtSigma, double statSigmaFB)
    {
        if (statSigma <= MAX_SIGMA && statSigma > 0)
        {
            mStatSigma = statSigma;
        }
        if (filtSigma <= MAX_SIGMA && filtSigma > 0)
        {
            mFiltSigma = filtSigma;
        }
        if(statSigmaFB <= MAX_SIGMA && statSigmaFB >0)
        {
            mStatSigmaFB = statSigmaFB;
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

    public Integer getQuantization()
    {
        return mQuantizationLevels;
    }

    private void setQuantizationLevel(int quantizationLevel)
    {
        if (quantizationLevel >= MIN_QUANTIZATION_LEVEL && quantizationLevel <= MAX_QUANTIZATION_LEVEL)
        {
            mQuantizationLevels = quantizationLevel;
        }
    }

    private Boolean isRelative()
    {
        return mRelative;
    }

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
        map.put(STAT_WINDOW_SIZE_FB, getStatWindowSizeFB().toString());
        map.put(RELATIVE_STAT_WINDOW_SIZE_FB, getRelativeStatWindowSizeFB().toString());
        map.put(STAT_SIGMA_FB, getStatSigmaFB().toString());
        map.put(ITERATIONS_FB, getNumberOfIterationFB().toString());
        map.put(QUANTIZATION, getQuantization().toString());
        map.put(IS_RELATIVE, isRelative().toString());
//        map.put(SCRIBBLE_WIDTH, getScribbleWidth());
//        map.put(SCRIBBLE_COLOR, getScribbleColor());
        return map;
    }

    private Double getRelativeStatWindowSizeFB()
    {
        return mRelativeStatWindowSizeFB;
    }

    private Integer getStatWindowSizeFB()
    {
        return mStatWindowSizeFB;
    }

    public Double getStatSigmaFB()
    {
        return mStatSigmaFB;
    }
}
