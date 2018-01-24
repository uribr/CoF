package cofproject.tau.android.cof;

import android.util.Log;

import com.google.common.base.Stopwatch;
import com.google.common.primitives.Floats;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CoF {
    private static final String TAG = "CoF";
    private static final double SAMPLE_RATE_DEFAULT = 0.1;

    // TermCriteria constant parameters (for the k-means quantization)
    private static final int TERM_CRITERIA_MAX_COUNT = 100;
    private static final double TERM_CRITERIA_EPSILON = 1.0;


    public static void applyFilter(Mat imToProcess, Mat filteredImage, Preset params) {
        Mat maskToCollect = Mat.ones(imToProcess.size(), CvType.CV_32FC1);
        applyFilter(imToProcess, filteredImage, maskToCollect, params);
        maskToCollect.release();
    }


    public static void applyFilter(Mat imToProcess, Mat filteredImage, Mat maskToCollect, Preset params) {
        Log.i(TAG, "applyFilter: Started applying filter");

        // extract parmeters from params
        int iterCnt = Utility.DEFAULT_NUMBER_OF_ITERATIONS;
        int winSize = Utility.DEFAULT_WINDOW_SIZE;
        double sigma = Utility.DEFAULT_SIGMA;
        int nBins = Utility.DEFAULT_QUNTIZATION_LEVEL;
        if (params != null) {
            iterCnt = params.getNumberOfIteration();
            winSize = params.getWindowSize();
            sigma = params.getSigma();
            nBins = params.getQuantization();
        }

        if (winSize % 2 == 0) {
            winSize--;
        }

        //int hws = winSize / 2; // half window size, floored
        if (imToProcess.rows() != filteredImage.rows() || imToProcess.cols() != filteredImage.cols()) {
            Log.e(TAG, "applyFilter: imToProcess.size() != filteredImage.size()", new IllegalArgumentException("imToProcess.size() != filteredImage.size()"));
        }

        Mat idx;
        Mat pab;
        Mat rowSum;
        Mat colSum;
        Mat pmi;
        Mat prod;
        Stopwatch sw = Stopwatch.createUnstarted(); // stopwatch to measure times


        // this matrix will hold the quantization mapping.
        // we assume there are no more than 256 bins, so we can use byte-typed matrix
        idx = new Mat(imToProcess.size(), CvType.CV_8UC1);

        sw.reset();
        sw.start();
        quantize(imToProcess, idx, nBins);
        sw.stop();
        Log.d(TAG, "applyFilter: qunatize time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

        pab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
        sw.reset();
        sw.start();
        collectPab(idx, maskToCollect, pab, nBins, winSize, sigma);
        sw.stop();
        Log.d(TAG, "applyFilter: collectPab time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");


        rowSum = new Mat(); // will be a single column
        colSum = new Mat(); // will be a single row
        pmi = new Mat(pab.size(), pab.type());

        Core.reduce(pab, rowSum, 1, Core.REDUCE_SUM);
        Core.reduce(pab, colSum, 0, Core.REDUCE_SUM);

        prod = new Mat(rowSum.rows(), colSum.cols(), rowSum.type());
        Core.gemm(rowSum, colSum, 1, new Mat(), 0, prod);

        // release unnecessary Mats:
        rowSum.release();
        colSum.release();

        // adding small constant to prevenet division by 0
        Core.add(prod, new Scalar(Float.MIN_NORMAL), prod);

        Core.divide(pab, prod, pmi);

        prod.release();
        pab.release();

        Mat imToProcessCopy = imToProcess.clone();

        sw.reset();
        sw.start();
        for (int i = 0; i < iterCnt; i++) {
            Log.d(TAG, "applyFilter: cofilter iteration no. " + (i + 1));
            coFilter(imToProcessCopy, idx, filteredImage, pmi, winSize, sigma);
            filteredImage.copyTo(imToProcessCopy);
            System.gc();
        }
        sw.stop();
        Log.d(TAG, "applyCoF: coFilter time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

        imToProcessCopy.release();

        // release all inner Mats:
        idx.release();
        maskToCollect.release();
        pmi.release();
    }

    /**
     * @param n - a positive integer
     * @return a new Integer list ranged from 0 to n-1
     */
    private static List<Integer> zeroTo(int n) {
        if (n < 0) {
            Log.e(TAG, "zeroTo: n < 0", new IllegalArgumentException("n < 0"));
        }
        List<Integer> lst = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lst.add(i);
        }
        return lst;
    }

//    private static void quantize(Mat rgbInput, Mat quantizedIm) {
//
//        quantize(rgbInput, quantizedIm, NUM_BINS_DEFAULT);
//    }

    private static void quantize(Mat rgbInput, Mat quantizedIm, int k) {
        quantize(rgbInput, quantizedIm, k, SAMPLE_RATE_DEFAULT);
    }

    private static void quantize(Mat rgbInput, Mat quantizedIm, int k, double sampleRate) {

        Log.i(TAG, "quantize: started");
        Mat rgbInput32f;
        Mat rgbReshaped;
        Mat sampledRgb;
        Mat labels;
        Mat centers;
        Mat minErr;
        Mat currCentMat;
        Mat diffMat;
        Mat diffMatReshaped;
        Mat minIndices;

        // convert RGB input to float-typed matrix
        rgbInput32f = new Mat();
        rgbInput.convertTo(rgbInput32f, CvType.CV_32F);

        // reshape the input image to RGB columns
        rgbReshaped = rgbInput32f.reshape(1, (int) rgbInput32f.total());
        int nPoints = rgbReshaped.rows(); // == number of pixels in the image

        // we'll sample the image according to sampleRate
        int nSamplePoints = (int) Math.round(sampleRate * nPoints);
        sampledRgb = new Mat(nSamplePoints, rgbReshaped.cols(), rgbReshaped.type());


        // sample unique nSamplePoints indices randomly in the range [0,nPoints)
        List<Integer> perm = getRandomPermutation(nPoints, nSamplePoints);


        // choose only the nSamplePoints first row indices from the shuffled list
        for (int i = 0; i < nSamplePoints; i++) {
            rgbReshaped.row(perm.get(i)).copyTo(sampledRgb.row(i));
        }

        rgbReshaped.release(); // don't need it anymore - free memory

        // perform k-means in order to create clusters for the quantization
        labels = new Mat();
        centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, TERM_CRITERIA_MAX_COUNT, TERM_CRITERIA_EPSILON);
        Core.kmeans(sampledRgb, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

        // free unnecessary memory
        sampledRgb.release();
        labels.release();

        //Size sz = rgbInput.size();

        // initialize min-err matrix with infinite float values
        minErr = new Mat(rgbInput.size(), CvType.CV_32F, new Scalar(Float.MAX_VALUE));

        float[] currCent = new float[3]; // will hold RGB of the current center

        currCentMat = new Mat(rgbInput32f.size(), rgbInput32f.type());
        minIndices = new Mat();
        diffMat = new Mat();

        for (int i = 0; i < k; i++) {

            //Log.d(TAG, "quantize: updating quantized image - level " + Integer.toString(i));

            // initialize a full-sized matrix with the current center values in all its pixels
            centers.get(i, 0, currCent);
            currCentMat.setTo(new Scalar(currCent[0], currCent[1], currCent[2]));

            // calculate the squared difference between each pixel of the input image vs. the current center
            // first subtract
            Core.subtract(rgbInput32f, currCentMat, diffMat);
            // then square the result (pixel-wise)
            Core.multiply(diffMat, diffMat, diffMat);

            // we turn diffMat into a 3-column matrix (each line is 1 pixel "RGB" values)
            diffMatReshaped = diffMat.reshape(1, (int) diffMat.total());

            // we sum the elements of diffMatReshaped row-wise (i.e sum along the "RGB" channels)
            Core.reduce(diffMatReshaped, diffMatReshaped, 1, Core.REDUCE_SUM);

            diffMat.release();

            // reshape back to the original dimensions
            diffMat = diffMatReshaped.reshape(1, rgbInput32f.rows());

            // save indices of minimal value - nonzero means it came from diffMat
            Core.compare(diffMat, minErr, minIndices, Core.CMP_LT);

            // update minErr
            Core.min(diffMat, minErr, minErr);

            // in qunatized, update only the minimum indices came from diffMat:
            quantizedIm.setTo(new Scalar(i), minIndices);
            diffMatReshaped.release();
        }
        diffMat.release();
        minIndices.release();
        currCentMat.release();
        minErr.release();
        centers.release();
        rgbInput32f.release();
    }


    /**
     * @param n number of elements in the permutation
     * @param k number of elements to draw from 0 to n-1
     * @return a list with k unique random numbers between 0 to n-1
     */
    private static List<Integer> getRandomPermutation(int n, int k) {
        if (k > n) {
            Log.e(TAG, "getRandomPermutation: k > n", new IllegalArgumentException("k > n"));
        }
        // we'll use the first version for values of k that are small enough
        if (k < n / 2) {
            Set<Integer> set = new HashSet<>(k);
            Random rand = new Random(System.currentTimeMillis());
            while (set.size() < k) {
                set.add(rand.nextInt(n));
            }
            return new ArrayList<>(set);
        } else {
            List<Integer> perm = zeroTo(n);
            Collections.shuffle(perm, new Random(System.currentTimeMillis()));
            return perm;
        }
    }

    private static void collectPab(Mat im2Collect, Mat mask2Collect, Mat pab, int nBins) {
        collectPab(im2Collect, mask2Collect, pab, nBins, Utility.DEFAULT_WINDOW_SIZE, Utility.DEFAULT_SIGMA);
    }


    /**
     * @param imToCollect
     * @param maskToCollect
     * @param pab           output matrix with the co-occurrence statistics
     * @param nBins
     * @param winSize
     * @param sigma
     */
    private static void collectPab(Mat imToCollect, Mat maskToCollect, Mat pab, int nBins, int winSize, double sigma) {

        Log.i(TAG, "collectPab: started");
        if (pab.rows() != nBins || pab.cols() != nBins) {
            Log.e(TAG, "collectPab: bad pab.size()", new IllegalArgumentException("bad pab.size()"));
            return;
        }

        float[][] pabArr = new float[nBins][nBins];
        Mat cmpMat = new Mat();
        Mat masked = new Mat();
        Mat tmp = new Mat();
        Mat w = new Mat();

        for (int iLevel = 0; iLevel < nBins; iLevel++) {

            Core.compare(imToCollect, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
            maskToCollect.copyTo(masked, cmpMat);
            Imgproc.GaussianBlur(masked, tmp, new Size(winSize, winSize), sigma, sigma);
            Core.multiply(tmp, maskToCollect, w);

            for (int j = 0; j < nBins; j++) {

                Core.compare(imToCollect, new Scalar(j), cmpMat, Core.CMP_EQ);
                tmp.setTo(new Scalar(0));

                // copy only values matching to level i indices
                w.copyTo(tmp, cmpMat);
                Scalar sc = Core.sumElems(tmp);
                pabArr[iLevel][j] = (float) sc.val[0];
            }
            masked.setTo(new Scalar(0));
        }
        cmpMat.release();
        masked.release();
        tmp.release();
        w.release();

        float[] flatened = Floats.concat(pabArr);

        pab.put(0, 0, flatened);

        Mat pabt = pab.t();

        Core.add(pab, pabt, pab);
        // normalize
        Scalar s = Core.sumElems(pab);
        Core.divide(pab, s, pab);
        pabt.release();
        //Core.normalize(pab, pa b, 1, 0, Core.NORM_L1);
    }

    private static void modMatChannels(Mat im2Filter, Mat mat, Mat updatedMat) {
        try {
            if (im2Filter.channels() == 3 * mat.channels()) {
                // duplicate 3 channels if needed
                List<Mat> pab3chans = Arrays.asList(mat, mat, mat);
                Core.merge(pab3chans, updatedMat);
            } else {
                mat.copyTo(updatedMat);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "modMatChannels: mat == null", e);
        }

    }

    private static void coFilter(Mat im2Filter, Mat im2Collect, Mat filteredImage, Mat pab) {
        coFilter(im2Filter, im2Collect, filteredImage, pab, Utility.DEFAULT_WINDOW_SIZE, Utility.DEFAULT_SIGMA);

    }

    private static void coFilter(Mat im2Filter, Mat im2Collect, Mat filteredImage, Mat pab, int winSize, double sigma) {

        Log.i(TAG, "coFilter: started");

        // default parameters
        if (im2Filter.type() == CvType.CV_8UC(im2Filter.channels())) {
            im2Filter.convertTo(im2Filter, CvType.CV_32FC(im2Filter.channels()));
        }

        if (filteredImage != null) {
            filteredImage.convertTo(filteredImage, im2Filter.type());
        } else {
            Log.e(TAG, "coFilter: filteredImage == null", new NullPointerException());
        }

        assert filteredImage != null;
        List<Mat> filtImChans = new ArrayList<>(filteredImage.channels());

        //Mat[] filtImChans = new Mat[filteredImage.channels()];

        Mat innerPab = new Mat();
        modMatChannels(im2Filter, pab, innerPab);

        Mat innerIm2Collect = new Mat();
        modMatChannels(im2Filter, im2Collect, innerIm2Collect);

        Size sz = im2Filter.size();
        int nBins = pab.rows();

        // split im2Filter, innerIm2Collect and innerPab  into channels
        List<Mat> channels2Filter = new ArrayList<>(im2Filter.channels());
        Core.split(im2Filter, channels2Filter);

        List<Mat> channelsPab = new ArrayList<>(innerPab.channels());
        Core.split(innerPab, channelsPab);
        innerPab.release();

        List<Mat> channels2Collect = new ArrayList<>(innerIm2Collect.channels());
        Core.split(innerIm2Collect, channels2Collect);
        innerIm2Collect.release();

        // kernel size for the gaussian filter
        Size kerSize = new Size(winSize, winSize);

        Mat cim = null;
        Mat mVals = null;

        // smooth per channel
        for (int iChannel = 0; iChannel < im2Filter.channels(); iChannel++) {

            Log.d(TAG, "coFilter: smoothing channel " + (iChannel + 1));
            cim = channels2Filter.get(iChannel);
            mVals = channels2Collect.get(iChannel);

            byte[] mValsArr = new byte[(int) mVals.total()];
            mVals.get(0, 0, mValsArr);

            Mat wpl = new Mat(mVals.size(), CvType.CV_32FC1);

            Mat pabCurrChan = channelsPab.get(iChannel);

            // our LUT is the iLevel-th row of the current channel pab matrix
            float[] LUT = new float[pabCurrChan.cols()];

            Mat W = Mat.zeros(mVals.size(), CvType.CV_32FC1);
            Mat sumW = Mat.zeros(mVals.size(), CvType.CV_32FC1);
            Mat S = Mat.zeros(mVals.size(), CvType.CV_32FC1);
            Mat sumS = Mat.zeros(mVals.size(), CvType.CV_32FC1);
            System.gc();

            // per level smoothing
            Mat cmpMat = new Mat();
            Mat tmp = new Mat();
            //Mat W = new Mat();
            for (int iLevel = 0; iLevel < nBins; iLevel++) {

                Core.compare(mVals, new Scalar(iLevel), cmpMat, Core.CMP_EQ);

                pabCurrChan.get(iLevel, 0, LUT);

                applyLUTonData(mVals, LUT, wpl, nBins);

                // smooth wpl
                Imgproc.GaussianBlur(wpl, tmp, kerSize, sigma, sigma);

                // take only necessary pixels
                tmp.copyTo(W, cmpMat);

                Imgproc.accumulate(W, sumW);

                Core.multiply(wpl, cim, tmp);

                Imgproc.GaussianBlur(tmp, tmp, kerSize, sigma, sigma);
                tmp.copyTo(S, cmpMat);

                Imgproc.accumulate(S, sumS);

                System.gc();
            }
            cmpMat.release();
            tmp.release();
            W.release();
            S.release();

            Core.add(sumW, new Scalar(Float.MIN_NORMAL), sumW);
            filtImChans.add(iChannel,new Mat());
            //filtImChans[iChannel] = new Mat();

            Core.divide(sumS, sumW, filtImChans.get(iChannel));

            sumS.release();
            sumW.release();
            pabCurrChan.release();
            wpl.release();

        }
        Core.merge(filtImChans, filteredImage);

        for (Mat m : channels2Collect) {
            m.release();
        }
        for (Mat m : channels2Filter) {
            m.release();
        }
        for (Mat m : channelsPab) {
            m.release();
        }

        for (Mat m : filtImChans) {
            m.release();
        }

        if (cim != null) {
            cim.release();
        }
        if (mVals != null) {
            mVals.release();
        }

    }

    private static void applyLUTonData(Mat inputData, float[] LUT, Mat outputData, int nBins) {
        if (inputData.rows() != outputData.rows() || inputData.cols() != outputData.cols()) {
            Log.e(TAG, "applyLUTonData: inputData.size() != outputData.size()", new IllegalArgumentException("inputData.size() != outputData.size()"));
            return;
        }

        if (LUT.length != nBins) {
            Log.e(TAG, "applyLUTonData: Invalid length for LUT - " + nBins, new IllegalArgumentException("Invalid length for LUT - " + nBins));
            return;
        }

        Core.MinMaxLocResult res = Core.minMaxLoc(inputData);
        if (res.minVal < 0 || res.maxVal >= nBins) {
            Log.e(TAG, "applyLUTonData: Invalid inputData values", new IllegalArgumentException("Invalid inputData values"));
            return;
        }

        Mat cmpMat = new Mat();
        for (int i = 0; i < nBins; i++) {
            Core.compare(inputData, new Scalar(i), cmpMat, Core.CMP_EQ);
            outputData.setTo(new Scalar(LUT[i]), cmpMat);
        }
        cmpMat.release();


    }

    private static void applyLUTonData(byte[] inputData, float[] LUT, float[] outputData) {

        if (inputData.length != outputData.length) {
            Log.e(TAG, "applyLUTonData: inputData.length != outputData.length", new IllegalArgumentException("inputData.length != outputData.length"));
        }

        byte[] tmp = inputData.clone();
        Arrays.sort(tmp);
        if (tmp[0] < 0 || tmp[tmp.length - 1] > LUT.length) {
            Log.e(TAG, "applyLUTonData: Invalid values in inputData", new IllegalArgumentException("Invalid values in inputData"));
        }

        for (int i = 0; i < inputData.length; i++) {
            outputData[i] = LUT[inputData[i]];
        }
    }
}

