package cofproject.tau.android.cof;

import android.util.Log;

import com.google.common.primitives.Floats;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
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

public class CoF {
    private static final String TAG = "CoF";
    private static final double SAMPLE_RATE_DEFAULT = 0.1;

    // TermCriteria constant parameters (for the k-means quantization)
    private static final int TERM_CRITERIA_MAX_COUNT = 100;
    private static final double TERM_CRITERIA_EPSILON = 1.0;
    private static final float EPSILON = Float.MIN_NORMAL;


//    public static void applyFilter(Mat imToProcess, Mat filteredImage, Preset params) {
//        Mat maskToCollect = Mat.ones(imToProcess.size(), CvType.CV_32FC1);
//        applyFilter(imToProcess, filteredImage, maskToCollect, params);
//        maskToCollect.release();
//    }


//    public static void applyFilter(Mat imToProcess, Mat filteredImage, Mat maskToCollect, Preset params) {
//        Log.i(TAG, "applyFilter: Started applying filter");
//
//        // extract parmeters from params
//        int iterCnt = Utility.DEFAULT_NUMBER_OF_ITERATIONS;
//        int winSize = Utility.DEFAULT_WINDOW_SIZE;
//        double sigma = Utility.DEFAULT_SIGMA;
//        int nBins = Utility.DEFAULT_QUNTIZATION_LEVEL;
//        if (params != null) {
//            iterCnt = params.getNumberOfIteration();
//            winSize = params.getStatWindowSize();
//            sigma = params.getStatSigma();
//            nBins = params.getQuantization();
//        }
//
//        if (winSize % 2 == 0) {
//            winSize--;
//        }
//
//        //int hws = winSize / 2; // half window size, floored
//        if (imToProcess.rows() != filteredImage.rows() || imToProcess.cols() != filteredImage.cols()) {
//            Log.e(TAG, "applyFilter: imToProcess.size() != filteredImage.size()", new IllegalArgumentException("imToProcess.size() != filteredImage.size()"));
//        }
//
//        Mat idx;
//        Mat pab;
//        Mat rowSum;
//        Mat colSum;
//        Mat pmi;
//        Mat prod;
//        Stopwatch sw = Stopwatch.createUnstarted(); // stopwatch to measure times
//
//
//        // this matrix will hold the quantization mapping.
//        // we assume there are no more than 256 bins, so we can use byte-typed matrix
//        idx = new Mat(imToProcess.size(), CvType.CV_8UC1);
//
//        sw.reset();
//        sw.start();
//        quantize(imToProcess, idx, nBins);
//        sw.stop();
//        Log.d(TAG, "applyFilter: qunatize time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
//
//        pab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
//        sw.reset();
//        sw.start();
//        collectPab(idx, maskToCollect, pab, nBins, winSize, sigma);
//        sw.stop();
//        Log.d(TAG, "applyFilter: collectPab time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
//
//
//        rowSum = new Mat(); // will be a single column
//        colSum = new Mat(); // will be a single row
//        pmi = new Mat(pab.size(), pab.type());
//
//        Core.reduce(pab, rowSum, 1, Core.REDUCE_SUM);
//        Core.reduce(pab, colSum, 0, Core.REDUCE_SUM);
//
//        prod = new Mat(rowSum.rows(), colSum.cols(), rowSum.type());
//        Core.gemm(rowSum, colSum, 1, new Mat(), 0, prod);
//
//        // release unnecessary Mats:
//        rowSum.release();
//        colSum.release();
//
//        // adding small constant to prevenet division by 0
//        Core.add(prod, new Scalar(EPSILON), prod);
//
//        Core.divide(pab, prod, pmi);
//
//        prod.release();
//        pab.release();
//
//        Mat imToProcessCopy = imToProcess.clone();
//
//        sw.reset();
//        sw.start();
//        for (int i = 0; i < iterCnt; i++) {
//            Log.d(TAG, "applyFilter: cofilter iteration no. " + (i + 1));
//            coFilter(imToProcessCopy, idx, filteredImage, pmi, winSize, sigma);
//            filteredImage.copyTo(imToProcessCopy);
//            System.gc();
//        }
//        sw.stop();
//        Log.d(TAG, "applyCoF: coFilter time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");
//
//        imToProcessCopy.release();
//
//        // release all inner Mats:
//        idx.release();
//        maskToCollect.release();
//        pmi.release();
//    }

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

    public static void quantize(Mat imToFilter, Mat quantizedIm, int k) {
        quantize(imToFilter, quantizedIm, k, SAMPLE_RATE_DEFAULT);
    }

    public static void quantize(Mat imToFilter, Mat quantizedIm, int k, double sampleRate) {

        Log.i(TAG, "quantize: started");
        Mat imToFilter32f;
        Mat reshaped;
        Mat sampled;
        Mat labels;
        Mat centers;
        Mat minErr;
        Mat currCentMat;
        Mat diffMat;
        Mat diffMatReshaped;
        Mat minIndices;

        // convert RGB input to float-typed matrix
        imToFilter32f = new Mat();
        imToFilter.convertTo(imToFilter32f, CvType.CV_32F);

        // reshape the input image to RGB columns
        reshaped = imToFilter32f.reshape(1, (int) imToFilter32f.total());
        int nPoints = reshaped.rows(); // == number of pixels in the image

        // we'll sample the image according to sampleRate
        int nSamplePoints = (int) Math.round(sampleRate * nPoints);
        sampled = new Mat(nSamplePoints, reshaped.cols(), reshaped.type());


        // sample unique nSamplePoints indices randomly in the range [0,nPoints)
        List<Integer> perm = getRandomPermutation(nPoints, nSamplePoints);


        // choose only the nSamplePoints first row indices from the shuffled list
        for (int i = 0; i < nSamplePoints; i++) {
            reshaped.row(perm.get(i)).copyTo(sampled.row(i));
        }

        reshaped.release(); // don't need it anymore - free memory

        // perform k-means in order to create clusters for the quantization
        labels = new Mat();
        centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, TERM_CRITERIA_MAX_COUNT, TERM_CRITERIA_EPSILON);
        Core.kmeans(sampled, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

        // free unnecessary memory
        sampled.release();
        labels.release();

        // initialize min-err matrix with infinite float values
        minErr = new Mat(imToFilter.size(), CvType.CV_32F, new Scalar(Float.POSITIVE_INFINITY));


        float[] currCent = new float[3]; // will hold RGB of the current center

        currCentMat = new Mat(imToFilter32f.size(), imToFilter32f.type());
        minIndices = new Mat();
        diffMat = new Mat();

        for (int i = 0; i < k; i++) {

            // initialize a full-sized matrix with the current center values in all its pixels
            centers.get(i, 0, currCent);
            currCentMat.setTo(new Scalar(currCent[0], currCent[1], currCent[2]));

            // calculate the squared difference between each pixel of the input image vs. the current center
            // first subtract
            Core.subtract(imToFilter32f, currCentMat, diffMat);
            // then square the result (pixel-wise)
            Core.multiply(diffMat, diffMat, diffMat);

            // we turn diffMat into a 3-column matrix (each line is 1 pixel "RGB" values)
            diffMatReshaped = diffMat.reshape(1, (int) diffMat.total());

            // we sum the elements of diffMatReshaped row-wise (i.e sum along the "RGB" channels)
            Core.reduce(diffMatReshaped, diffMatReshaped, 1, Core.REDUCE_SUM);

            diffMat.release();

            // reshape back to the original dimensions
            diffMat = diffMatReshaped.reshape(1, imToFilter32f.rows());

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
        imToFilter32f.release();
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

//    public static void collectPab(Mat im2Collect, Mat mask2Collect, Mat pab, int nBins) {
//        collectPab(im2Collect, mask2Collect, pab, nBins, Utility.DEFAULT_WINDOW_SIZE, Utility.DEFAULT_SIGMA);
//    }

    public static void collectPab(Mat imToCollect, Mat pab, int nBins, int winSize, double sigma) {
        Mat mask = Mat.ones(imToCollect.size(), CvType.CV_32FC1);
        collectPab(imToCollect, mask, pab, nBins, winSize, sigma);
        mask.release();
    }


    public static void collectPab(Mat imToCollect, Mat maskToCollect, Mat pab, int nBins, int winSize, double sigma) {

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
                tmp.setTo(Utility.ZERO_SCALAR);

                // copy only values matching to level i indices
                w.copyTo(tmp, cmpMat);
                Scalar sc = Core.sumElems(tmp);
                pabArr[iLevel][j] = (float) sc.val[0];
            }
            masked.setTo(Utility.ZERO_SCALAR);
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
    }

    private static void modMatChannels(Mat imToFilter, Mat mat, Mat updatedMat) {
        try {
            if (imToFilter.channels() == 3 * mat.channels()) {
                // duplicate 3 channels if needed
                List<Mat> triChans = Arrays.asList(mat, mat, mat);
                Core.merge(triChans, updatedMat);
            } else {
                mat.copyTo(updatedMat);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "modMatChannels: mat == null", e);
        }

    }

//    public static void coFilter(Mat im2Filter, Mat im2Collect, Mat filteredImage, Mat pab) {
//        coFilter(im2Filter, im2Collect, filteredImage, pab, Utility.DEFAULT_WINDOW_SIZE, Utility.DEFAULT_SIGMA);
//    }

    public static void coFilter(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat pab, int winSize, double sigma) {
        // default parameters
        if (imToFilter.type() == CvType.CV_8UC(imToFilter.channels())) {
            imToFilter.convertTo(imToFilter, CvType.CV_32FC(imToFilter.channels()));
        }

        if (filteredImage != null) {
            filteredImage.convertTo(filteredImage, imToFilter.type());
        } else {
            Log.e(TAG, "coFilter: filteredImage == null", new NullPointerException());
            return;
        }

        List<Mat> filtImChans = new ArrayList<>(filteredImage.channels());

        Mat innerPab = new Mat();
        modMatChannels(imToFilter, pab, innerPab);

        Mat innerIm2Collect = new Mat();
        modMatChannels(imToFilter, imToCollect, innerIm2Collect);

        // split imToFilter, innerIm2Collect and innerPab  into channels
        List<Mat> channels2Filter = new ArrayList<>(imToFilter.channels());
        Core.split(imToFilter, channels2Filter);

        List<Mat> channelsPab = new ArrayList<>(innerPab.channels());
        Core.split(innerPab, channelsPab);
        innerPab.release();

        List<Mat> channels2Collect = new ArrayList<>(innerIm2Collect.channels());
        Core.split(innerIm2Collect, channels2Collect);
        innerIm2Collect.release();

        // kernel size for the gaussian filter
        Size kerSize = new Size(winSize, winSize);

        int nBins = pab.rows();
        int channelsCnt = imToFilter.channels();
        Size sz = channels2Collect.get(0).size();
        Mat cim = null;
        Mat mVals = null;
        Mat pabCurrChan = null;
        Mat W = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumW = Mat.zeros(sz, CvType.CV_32FC1);
        Mat S = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumS = Mat.zeros(sz, CvType.CV_32FC1);
        Mat wpl = new Mat(sz, CvType.CV_32FC1);
        Mat cmpMat = new Mat();
        // our LUT is the iLevel-th row of the current channel pab matrix
        float[] LUT = new float[nBins];

        // smooth per channel
        for (int iChannel = 0; iChannel < channelsCnt; iChannel++) {

            Log.d(TAG, "coFilter: smoothing channel " + (iChannel + 1));
            cim = channels2Filter.get(iChannel);
            mVals = channels2Collect.get(iChannel);
            pabCurrChan = channelsPab.get(iChannel);

            // per level smoothing
            for (int iLevel = 0; iLevel < nBins; iLevel++) {

                Core.compare(mVals, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
                pabCurrChan.get(iLevel, 0, LUT);
                applyLUTonData(mVals, LUT, wpl, nBins);
                // smooth wpl
                Imgproc.GaussianBlur(wpl, W, kerSize, sigma, sigma);
                // take only necessary pixels
                Imgproc.accumulate(W, sumW, cmpMat);
                ///////////////////////////////////////////
                Core.multiply(wpl, cim, S);
                Imgproc.GaussianBlur(S, S, kerSize, sigma, sigma);
                Imgproc.accumulate(S, sumS, cmpMat);
            }
            Core.add(sumW, new Scalar(EPSILON), sumW);
            filtImChans.add(iChannel,new Mat());

            Core.divide(sumS, sumW, filtImChans.get(iChannel));

            sumW.setTo(Utility.ZERO_SCALAR);
            sumS.setTo(Utility.ZERO_SCALAR);
        }
        Core.merge(filtImChans, filteredImage);

        // release!!!
        Utility.releaseMats(channels2Collect, channels2Filter, channelsPab, filtImChans);
        Utility.releaseMats(W, sumW, S, sumS, wpl, cmpMat, cim, mVals, pabCurrChan);

    }

//    public static void FBCoFilter(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat fgPab, Mat bgPab, int winSize) {
//
//        if (imToFilter.type() == CvType.CV_8UC(imToFilter.channels())) {
//            imToFilter.convertTo(imToFilter, CvType.CV_32FC(imToFilter.channels()));
//        }
//
//        if (filteredImage != null) {
//            filteredImage.convertTo(filteredImage, imToFilter.type());
//        } else {
//            Log.e(TAG, "FBCoFilter: filteredImage == null", new NullPointerException());
//        }
//
//        assert filteredImage != null;
//        List<Mat> filtImChans = new ArrayList<>(filteredImage.channels());
//
//        // split imToFilter, innerImToCollect and innerFgPab  into channels
//        List<Mat> channelsToFilter = new ArrayList<>(imToFilter.channels());
//        Core.split(imToFilter, channelsToFilter);
//
//        // kernel size for the box filter
//        Size kerSize = new Size(winSize, winSize);
//
//        int nBins = fgPab.rows();
//        int channelsCnt = imToFilter.channels();
//        Size sz = imToCollect.size();
//        Mat cim = null;
//        Mat W = Mat.zeros(sz, CvType.CV_32FC1);
//        Mat sumW = Mat.zeros(sz, CvType.CV_32FC1);
//        Mat S = Mat.zeros(sz, CvType.CV_32FC1);
//        Mat sumS = Mat.zeros(sz, CvType.CV_32FC1);
//        Mat fgWpl = new Mat(sz, CvType.CV_32FC1);
//        Mat bgWpl = new Mat(sz, CvType.CV_32FC1);
//        Mat cmpMat = new Mat();
//        Mat tmp = new Mat();
//        // our LUT is the iLevel-th row of the current channel pab matrix
//        float[] LUT = new float[nBins];
//        Point anchor = new Point(-1, -1);
//
//        // smooth per channel
//        for (int iChannel = 0; iChannel < channelsCnt; iChannel++) {
//            Log.d(TAG, "FBCoFilter: filtering channel " + (iChannel + 1));
//            cim = channelsToFilter.get(iChannel);
//            // per level smoothing
//            for (int iLevel = 0; iLevel < nBins; iLevel++) {
//
//                //Core.compare(mVals, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
//                Core.compare(imToCollect, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
//                fgPab.get(iLevel, 0, LUT);
//                applyLUTonData(imToCollect, LUT, fgWpl, nBins);
//                bgPab.get(iLevel, 0, LUT);
//                applyLUTonData(imToCollect, LUT, bgWpl,nBins);
//
//                Core.add(fgWpl, bgWpl, W);
//                Imgproc.boxFilter(W, W, -1, kerSize, anchor, false);
//                // take only necessary pixels
//                Imgproc.accumulate(W, sumW, cmpMat);
//
//                Imgproc.boxFilter(fgWpl, tmp, -1, kerSize, anchor, false);
//                Core.multiply(tmp, cim, tmp);
//
//                Core.multiply(bgWpl, cim, S);
//                Imgproc.boxFilter(S, S, -1, kerSize, anchor, false);
//                Core.add(S, tmp, S);
//                Imgproc.accumulate(S, sumS, cmpMat);
//            }
//            Core.add(sumW, new Scalar(EPSILON), sumW);
//            filtImChans.add(iChannel,new Mat());
//
//            Core.divide(sumS, sumW, filtImChans.get(iChannel));
//
//            sumW.setTo(Utility.ZERO_SCALAR);
//            sumS.setTo(Utility.ZERO_SCALAR);
//        }
//        Core.merge(filtImChans, filteredImage);
//
//        // release!!!
//        Utility.releaseMats(channelsToFilter, filtImChans);
//        Utility.releaseMats(W, sumW, S, sumS, fgWpl, cmpMat, cim, tmp);
//
//    }

    public static void FBCoFilterGaussian(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat fgPab, Mat bgPab, int winSize, double sigma) {
        FBCoFilterGaussian(imToFilter, imToCollect, filteredImage, fgPab, bgPab,winSize, sigma, 0.5);
    }

    public static void FBCoFilterGaussian(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat fgPab, Mat bgPab, int winSize, double sigma, double alpha) {

        if (imToFilter.type() == CvType.CV_8UC(imToFilter.channels())) {
            imToFilter.convertTo(imToFilter, CvType.CV_32FC(imToFilter.channels()));
        }

        if (filteredImage != null) {
            filteredImage.convertTo(filteredImage, imToFilter.type());
        } else {
            Log.e(TAG, "FBCoFilter: filteredImage == null", new NullPointerException());
            return;
        }

        if (alpha < 0 || alpha > 1) {
            Log.e(TAG, "coFilter: invalid value for alpha: " + alpha, new IllegalArgumentException("coFilter: invalid value for alpha: " + alpha));
            return;
        }

        List<Mat> filtImChans = new ArrayList<>(filteredImage.channels());

        // split imToFilter, innerImToCollect and innerFgPab  into channels
        List<Mat> channelsToFilter = new ArrayList<>(imToFilter.channels());
        Core.split(imToFilter, channelsToFilter);

        // kernel size for the box filter
        Size kerSize = new Size(winSize, winSize);

        int nBins = fgPab.rows();
        int channelsCnt = imToFilter.channels();
        //Size sz = channelsToCollect.get(0).size();
        Size sz = imToCollect.size();
        Mat cim = null;
        Mat W = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumW = Mat.zeros(sz, CvType.CV_32FC1);
        Mat S = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumS = Mat.zeros(sz, CvType.CV_32FC1);
        Mat fgWpl = new Mat(sz, CvType.CV_32FC1);
        Mat bgWpl = new Mat(sz, CvType.CV_32FC1);
        Mat cmpMat = new Mat();
        Mat tmp = new Mat();
        Mat wfg = new Mat();
        Mat wbg = new Mat();
        // our LUT is the iLevel-th row of the current channel pab matrix
        float[] LUT = new float[nBins];
        Point anchor = new Point(-1, -1);

        // smooth per channel
        for (int iChannel = 0; iChannel < channelsCnt; iChannel++) {
            Log.d(TAG, "FBCoFilter: filtering channel " + (iChannel + 1));
            cim = channelsToFilter.get(iChannel);

            // per level smoothing
            for (int iLevel = 0; iLevel < nBins; iLevel++) {

                // denominator
                Core.compare(imToCollect, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
                fgPab.get(iLevel, 0, LUT);
                applyLUTonData(imToCollect, LUT, fgWpl, nBins);
                bgPab.get(iLevel, 0, LUT);
                applyLUTonData(imToCollect, LUT, bgWpl,nBins);
                Imgproc.GaussianBlur(fgWpl, wfg, kerSize, sigma, sigma);
                Imgproc.GaussianBlur(bgWpl, wbg, kerSize, sigma, sigma);
                Core.addWeighted(wfg, alpha, wbg, 1-alpha,0, W);
                // take only necessary pixels
                Imgproc.accumulate(W, sumW, cmpMat);

                //nominator
                Core.multiply(wfg, cim, S);
                Core.multiply(bgWpl, cim, tmp);
                Imgproc.GaussianBlur(tmp, tmp, kerSize, sigma, sigma);
                Core.addWeighted(S, alpha, tmp, 1-alpha,0, S);
                // take only necessary pixels
                Imgproc.accumulate(S, sumS, cmpMat);

            }
            Core.add(sumW, new Scalar(EPSILON), sumW);
            filtImChans.add(iChannel,new Mat());

            Core.divide(sumS, sumW, filtImChans.get(iChannel));

            sumW.setTo(Utility.ZERO_SCALAR);
            sumS.setTo(Utility.ZERO_SCALAR);
        }
        Core.merge(filtImChans, filteredImage);

        // release!!!
        Utility.releaseMats(channelsToFilter, filtImChans);
        Utility.releaseMats(W, sumW, S, sumS, fgWpl, bgWpl, wfg, wbg, cmpMat, cim, tmp);
    }

    public static void FBCoFilter(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat fgPab, Mat bgPab, int winSize) {
        FBCoFilter(imToFilter, imToCollect, filteredImage, fgPab, bgPab,winSize, 0.5);
    }

    public static void FBCoFilter(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat fgPab, Mat bgPab, int winSize, double alpha) {

        if (imToFilter.type() == CvType.CV_8UC(imToFilter.channels())) {
            imToFilter.convertTo(imToFilter, CvType.CV_32FC(imToFilter.channels()));
        }

        if (filteredImage != null) {
            filteredImage.convertTo(filteredImage, imToFilter.type());
        } else {
            Log.e(TAG, "FBCoFilter: filteredImage == null", new NullPointerException());
            return;
        }

        if (alpha < 0 || alpha > 1) {
            Log.e(TAG, "coFilter: invalid value for alpha: " + alpha, new IllegalArgumentException("coFilter: invalid value for alpha: " + alpha));
            return;
        }

        List<Mat> filtImChans = new ArrayList<>(filteredImage.channels());

        // split imToFilter, innerImToCollect and innerFgPab  into channels
        List<Mat> channelsToFilter = new ArrayList<>(imToFilter.channels());
        Core.split(imToFilter, channelsToFilter);

        // kernel size for the box filter
        Size kerSize = new Size(winSize, winSize);

        int nBins = fgPab.rows();
        int channelsCnt = imToFilter.channels();
        Size sz = imToCollect.size();
        Mat cim = null;
        Mat W = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumW = Mat.zeros(sz, CvType.CV_32FC1);
        Mat S = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumS = Mat.zeros(sz, CvType.CV_32FC1);
        Mat fgWpl = new Mat(sz, CvType.CV_32FC1);
        Mat bgWpl = new Mat(sz, CvType.CV_32FC1);
        Mat cmpMat = new Mat();
        Mat tmp = new Mat();
        Mat wfg = new Mat();
        Mat wbg = new Mat();
        // our LUT is the iLevel-th row of the current channel pab matrix
        float[] LUT = new float[nBins];
        Point anchor = new Point(-1, -1);

        // smooth per channel
        for (int iChannel = 0; iChannel < channelsCnt; iChannel++) {
            Log.d(TAG, "FBCoFilter: filtering channel " + (iChannel + 1));
            cim = channelsToFilter.get(iChannel);

            // per level smoothing
            for (int iLevel = 0; iLevel < nBins; iLevel++) {

                // denominator
                Core.compare(imToCollect, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
                fgPab.get(iLevel, 0, LUT);
                applyLUTonData(imToCollect, LUT, fgWpl, nBins);
                bgPab.get(iLevel, 0, LUT);
                applyLUTonData(imToCollect, LUT, bgWpl,nBins);
                Imgproc.boxFilter(fgWpl, wfg,-1, kerSize, anchor, false);
                Imgproc.boxFilter(bgWpl, wbg, -1, kerSize, anchor, false);
                Core.addWeighted(wfg, alpha, wbg, 1-alpha,0, W);
                // take only necessary pixels
                Imgproc.accumulate(W, sumW, cmpMat);

                //nominator
                Core.multiply(wfg, cim, S);
                Core.multiply(bgWpl, cim, tmp);
                Imgproc.boxFilter(tmp, tmp, -1, kerSize, anchor, false);
                Core.addWeighted(S, alpha, tmp, 1-alpha,0, S);
                // take only necessary pixels
                Imgproc.accumulate(S, sumS, cmpMat);

            }
            Core.add(sumW, new Scalar(EPSILON), sumW);
            filtImChans.add(iChannel,new Mat());

            Core.divide(sumS, sumW, filtImChans.get(iChannel));

            sumW.setTo(Utility.ZERO_SCALAR);
            sumS.setTo(Utility.ZERO_SCALAR);
        }
        Core.merge(filtImChans, filteredImage);

        // release!!!
        Utility.releaseMats(channelsToFilter, filtImChans);
        Utility.releaseMats(W, sumW, S, sumS, fgWpl, bgWpl, wfg, wbg, cmpMat, cim, tmp);
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

    public static void pabToPmi(Mat pab, Mat pmi) {
        Mat rowSum = new Mat(); // will be a single column
        Mat colSum = new Mat(); // will be a single row
        Core.reduce(pab, rowSum, 1, Core.REDUCE_SUM);
        Core.reduce(pab, colSum, 0, Core.REDUCE_SUM);

        Mat prod = new Mat(pab.size(), pab.type());
        Mat tmp = new Mat();
        Core.gemm(rowSum, colSum, 1, tmp, 0, prod);

        // adding small constant to prevent division by 0
        Core.add(prod, new Scalar(EPSILON), prod);
        Core.divide(pab, prod, pmi);

        Mat pmiDiagonal = pmi.diag();
        int nBins = pmi.rows();
        float[] diag = new float[nBins];
        pmiDiagonal.get(0,0,diag);
        for(int i = 0; i < nBins; i++){
            if (diag[i] == 0) {
                pmi.put(i,i,1);
            }
        }

        pmiDiagonal.release();
        tmp.release();
        rowSum.release();
        colSum.release();
        prod.release();
    }
}

