
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

/**
 * This class implements the different methods involved with the Co-Occurrence Filter, as described
 * in the original article.
 * @see <a href="https://arxiv.org/pdf/1703.04111.pdf">Co-Occurrence Filter Paper</a>
 */
public class CoF {

    private static final String TAG = "CoF";
    private static final double SAMPLE_RATE_DEFAULT = 0.1;
    // TermCriteria constant parameters (for the k-means quantization)
    private static final int TERM_CRITERIA_MAX_COUNT = 100;
    private static final double TERM_CRITERIA_EPSILON = 1.0;
    private static final float EPSILON = Float.MIN_NORMAL;

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

    /**
     * This function creates the guidance image for CoF. It samples the input image, and performs
     * k-means clustering for that image. The output image is the guidance image, where instead
     * of a pixel's intensity value there's the cluster index (between 0 to k-1) this pixel is related to.
     *
     * @param imToFilter  Input image
     * @param quantizedIm Output image
     * @param k           The number of clusters for the k-means algorithm
     * @param sampleRate  the sample rate of the image - a positive number no larger than 1,
     *                    determines the percentage of pixels to be sampled from the image.
     */
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

        // perform k-means in order to create clusters for the quantization
        labels = new Mat();
        centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, TERM_CRITERIA_MAX_COUNT, TERM_CRITERIA_EPSILON);
        Core.kmeans(sampled, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

        // free unnecessary memory
        Utilities.releaseMats(sampled, labels, reshaped);

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
        Utilities.releaseMats(diffMat, minIndices, currCentMat, minErr, centers, imToFilter32f);
    }

    /**
     * An overload for {@link #quantize(Mat, Mat, int, double)}
     *
     * @param imToFilter  Input image
     * @param quantizedIm Output image
     */
    public static void quantize(Mat imToFilter, Mat quantizedIm) {

        quantize(imToFilter, quantizedIm, Utilities.DEFAULT_QUNTIZATION_LEVEL);
    }

    /**
     * An overload for {@link #quantize(Mat, Mat, int, double)}
     *
     * @param imToFilter  Input image
     * @param quantizedIm Output image
     * @param k           The number of clusters for the k-means algorithm
     */
    public static void quantize(Mat imToFilter, Mat quantizedIm, int k) {
        quantize(imToFilter, quantizedIm, k, SAMPLE_RATE_DEFAULT);
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


    /**
     * This method gets the guidance image and a mask, and outputs a squared matrix with the co-occurrence
     * statistics. The size of the output matrix is k*k, where k is the number of clusters in the guidance image
     * (produced by {@link #quantize(Mat, Mat, int, double) quantize}) .
     *
     * @param imToCollect   A guidance image.
     * @param maskToCollect A binary mask. The statistics will be collected only from non-zero pixels
     *                      with regards to that mask.
     * @param pab           Output matrix - the co-occurrence matrix.
     * @param nBins         Number of clusters in the guidance image.
     * @param winSize       The co-occurrences are collected within a squared window around a pixel.
     *                      This parameter determines the size of that window. Namely, that is the size
     *                      of the window for the Gaussian filter applied in the process.
     * @param sigma         The standard deviation for the Gaussian filter applied in the process.
     */
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
                tmp.setTo(Utilities.ZERO_SCALAR);

                // copy only values matching to level i indices
                w.copyTo(tmp, cmpMat);
                Scalar sc = Core.sumElems(tmp);
                pabArr[iLevel][j] = (float) sc.val[0];
            }
            masked.setTo(Utilities.ZERO_SCALAR);
        }

        float[] flatened = Floats.concat(pabArr);

        pab.put(0, 0, flatened);

        Mat pabt = pab.t();
        Core.add(pab, pabt, pab);
        // normalize
        Scalar s = Core.sumElems(pab);
        Core.divide(pab, s, pab);

        // release memory
        Utilities.releaseMats(cmpMat, masked, tmp, w, pabt);
    }

    /**
     * An overload for {@link #collectPab(Mat, Mat, Mat, int, int, double)}
     *
     * @param imToCollect   A guidance image.
     * @param maskToCollect A binary mask. The statistics will be collected only from non-zero pixels
     *                      with regards to that mask.
     * @param pab           Output matrix - the co-occurrence matrix.
     * @param nBins         Number of clusters in the guidance image.
     */
    public static void collectPab(Mat imToCollect, Mat maskToCollect, Mat pab, int nBins) {
        collectPab(imToCollect, maskToCollect, pab, nBins, Utilities.DEFAULT_WINDOW_SIZE, Utilities.DEFAULT_SIGMA);
    }

    /**
     * An overload for {@link #collectPab(Mat, Mat, Mat, int, int, double)}. Defaults the statistics
     * collection mask to the all-ones matrix (collecting statistics from the whole image.
     *
     * @param imToCollect A guidance image.
     * @param pab         Output matrix - the co-occurrence matrix.
     * @param nBins       Number of clusters in the guidance image.
     */
    public static void collectPab(Mat imToCollect, Mat pab, int nBins, int winSize, double sigma) {
        Mat mask = Mat.ones(imToCollect.size(), CvType.CV_32FC1);
        collectPab(imToCollect, mask, pab, nBins, winSize, sigma);
        mask.release();
    }


    /**
     * Performs conversion of co-occurrence matrix (P(a,b) - the output of {@link #collectPab(Mat, Mat, int, int, double)}
     * to M(a,b) (PMI - Pointwise Mutual Information - normalized CO matrix).
     * @param pab input matrix - P(a,b)
     * @param pmi output matrix - PMI
     */
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
        pmiDiagonal.get(0, 0, diag);
        for (int i = 0; i < nBins; i++) {
            if (diag[i] == 0) {
                pmi.put(i, i, 1);
            }
        }

        Utilities.releaseMats(pmiDiagonal, tmp, rowSum, colSum, prod);
    }

    /**
     * The co-occurrence filter - performs CoF on an input image according to the guidance image
     * and the CO matrix.
     * @param imToFilter Input image.
     * @param imToCollect Guidance image.
     * @param filteredImage Output image.
     * @param pab Co-Occurrence matrix.
     * @param winSize Window size for the Gaussian filter.
     * @param sigma Standard deviation for the Gaussian filter.
     */
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

        int channelsCnt = imToFilter.channels();
        List<Mat> filtImChans = new ArrayList<>(channelsCnt);

        // split imToFilter into channels
        List<Mat> channelsToFilter = new ArrayList<>(channelsCnt);
        Core.split(imToFilter, channelsToFilter);

        // kernel size for the Gaussian filter
        Size kerSize = new Size(winSize, winSize);

        int nBins = pab.rows();
        Size sz = imToCollect.size();
        Mat currentChannelIm = null;
        Mat W = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumW = Mat.zeros(sz, CvType.CV_32FC1);
        Mat S = Mat.zeros(sz, CvType.CV_32FC1);
        Mat sumS = Mat.zeros(sz, CvType.CV_32FC1);
        Mat WPL = new Mat(sz, CvType.CV_32FC1); // WPL = Weight Per Level
        Mat cmpMat = new Mat();
        // our LUT is the iLevel-th row of the current channel pab matrix
        float[] LUT = new float[nBins];

        // smooth per channel
        for (int iChannel = 0; iChannel < channelsCnt; iChannel++) {

            Log.d(TAG, "coFilter: smoothing channel " + (iChannel + 1));
            currentChannelIm = channelsToFilter.get(iChannel);
            // per level smoothing
            for (int iLevel = 0; iLevel < nBins; iLevel++) {
                // denominator
                Core.compare(imToCollect, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
                pab.get(iLevel, 0, LUT);
                applyLUTonData(imToCollect, LUT, WPL, nBins);
                // smooth WPL
                Imgproc.GaussianBlur(WPL, W, kerSize, sigma, sigma);
                // take only necessary pixels
                Imgproc.accumulate(W, sumW, cmpMat);

                // nominator
                Core.multiply(WPL, currentChannelIm, S);
                Imgproc.GaussianBlur(S, S, kerSize, sigma, sigma);
                Imgproc.accumulate(S, sumS, cmpMat);
            }
            Core.add(sumW, new Scalar(EPSILON), sumW);
            filtImChans.add(iChannel, new Mat());

            Core.divide(sumS, sumW, filtImChans.get(iChannel));

            sumW.setTo(Utilities.ZERO_SCALAR);
            sumS.setTo(Utilities.ZERO_SCALAR);
        }
        Core.merge(filtImChans, filteredImage);

        // release memory
        Utilities.releaseMats(channelsToFilter, filtImChans);
        Utilities.releaseMats(W, sumW, S, sumS, WPL, cmpMat, currentChannelIm);

    }

    /**
     * Overload for {@link #coFilter(Mat, Mat, Mat, Mat, int, double)}
     * @param imToFilter Input image.
     * @param imToCollect Guidance image.
     * @param filteredImage Output image.
     * @param pab Co-Occurrence matrix.
     */
    public static void coFilter(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat pab) {
        coFilter(imToFilter, imToCollect, filteredImage, pab, Utilities.DEFAULT_WINDOW_SIZE, Utilities.DEFAULT_SIGMA);
    }


    /**
     * This method implement the FB-CoF as described in the original paper. It is based on the OpenCV
     * boxFilter, in its un-normalized version.
     * Another variant of this function, using the Gaussian filter, is also available ({@link #FBCoFilterGaussian(Mat, Mat, Mat, Mat, Mat, int, double, double)}).
     * @see <a href="https://docs.opencv.org/2.4/modules/imgproc/doc/filtering.html#void boxFilter(InputArray src, OutputArray dst, int ddepth, Size ksize, Point anchor, bool normalize, int borderType)">OpenCV boxFilter</a>
     * @param imToFilter Input image.
     * @param imToCollect Guidance image.
     * @param filteredImage Output image.
     * @param fgPab Foreground co-occurrence statistics.
     * @param bgPab Background co-occurrence statistics.
     * @param winSize Window size for the box filter.
     * @param alpha A weight parameter determines the weight of the image produced from the foreground statistics.
     *              The weight of the complementary image (produced from the background statistics) is 1-alpha.
     */
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
                applyLUTonData(imToCollect, LUT, bgWpl, nBins);
                Imgproc.boxFilter(fgWpl, wfg, -1, kerSize, anchor, false);
                Imgproc.boxFilter(bgWpl, wbg, -1, kerSize, anchor, false);
                Core.addWeighted(wfg, alpha, wbg, 1 - alpha, 0, W);
                // take only necessary pixels
                Imgproc.accumulate(W, sumW, cmpMat);

                //nominator
                Core.multiply(wfg, cim, S);
                Core.multiply(bgWpl, cim, tmp);
                Imgproc.boxFilter(tmp, tmp, -1, kerSize, anchor, false);
                Core.addWeighted(S, alpha, tmp, 1 - alpha, 0, S);
                // take only necessary pixels
                Imgproc.accumulate(S, sumS, cmpMat);

            }
            Core.add(sumW, new Scalar(EPSILON), sumW);
            filtImChans.add(iChannel, new Mat());

            Core.divide(sumS, sumW, filtImChans.get(iChannel));

            sumW.setTo(Utilities.ZERO_SCALAR);
            sumS.setTo(Utilities.ZERO_SCALAR);
        }
        Core.merge(filtImChans, filteredImage);

        // release!!!
        Utilities.releaseMats(channelsToFilter, filtImChans);
        Utilities.releaseMats(W, sumW, S, sumS, fgWpl, bgWpl, wfg, wbg, cmpMat, cim, tmp);
    }

    /**
     * An overload for {@link #FBCoFilter(Mat, Mat, Mat, Mat, Mat, int, double)}, using a default value for alpha.
     * @param imToFilter Input image.
     * @param imToCollect Guidance image.
     * @param filteredImage Output image.
     * @param fgPab Foreground co-occurrence statistics.
     * @param bgPab Background co-occurrence statistics.
     * @param winSize Window size for the box filter.
     */
    public static void FBCoFilter(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat fgPab, Mat bgPab, int winSize) {
        FBCoFilter(imToFilter, imToCollect, filteredImage, fgPab, bgPab, winSize, Utilities.DEFAULT_ALPHA);
    }

    /**
     * A variant of the {@link #FBCoFilter(Mat, Mat, Mat, Mat, Mat, int)} method, using a Gaussian filter
     * instead of a box filter.
     * @param imToFilter Input image.
     * @param imToCollect Guidance image.
     * @param filteredImage Output image.
     * @param fgPab Foreground co-occurrence statistics.
     * @param bgPab Background co-occurrence statistics.
     * @param winSize Window size for the Gaussian filter.
     * @param sigma Standard deviation for the Gaussian filter.
     * @param alpha A weight parameter determines the weight of the image produced from the foreground statistics.
     *              The weight of the complementary image (produced from the background statistics) is 1-alpha.
     */
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
                applyLUTonData(imToCollect, LUT, bgWpl, nBins);
                Imgproc.GaussianBlur(fgWpl, wfg, kerSize, sigma, sigma);
                Imgproc.GaussianBlur(bgWpl, wbg, kerSize, sigma, sigma);
                Core.addWeighted(wfg, alpha, wbg, 1 - alpha, 0, W);
                // take only necessary pixels
                Imgproc.accumulate(W, sumW, cmpMat);

                //nominator
                Core.multiply(wfg, cim, S);
                Core.multiply(bgWpl, cim, tmp);
                Imgproc.GaussianBlur(tmp, tmp, kerSize, sigma, sigma);
                Core.addWeighted(S, alpha, tmp, 1 - alpha, 0, S);
                // take only necessary pixels
                Imgproc.accumulate(S, sumS, cmpMat);

            }
            Core.add(sumW, new Scalar(EPSILON), sumW);
            filtImChans.add(iChannel, new Mat());

            Core.divide(sumS, sumW, filtImChans.get(iChannel));

            sumW.setTo(Utilities.ZERO_SCALAR);
            sumS.setTo(Utilities.ZERO_SCALAR);
        }
        Core.merge(filtImChans, filteredImage);

        // release!!!
        Utilities.releaseMats(channelsToFilter, filtImChans);
        Utilities.releaseMats(W, sumW, S, sumS, fgWpl, bgWpl, wfg, wbg, cmpMat, cim, tmp);
    }

    /**
     * An overload for {@link #FBCoFilterGaussian(Mat, Mat, Mat, Mat, Mat, int, double, double)}, using a default value for alpha.
     * @param imToFilter Input image.
     * @param imToCollect Guidance image.
     * @param filteredImage Output image.
     * @param fgPab Foreground co-occurrence statistics.
     * @param bgPab Background co-occurrence statistics.
     * @param winSize Window size for the Gaussian filter.
     * @param sigma Standard deviation for the Gaussian filter.
     */
    public static void FBCoFilterGaussian(Mat imToFilter, Mat imToCollect, Mat filteredImage, Mat fgPab, Mat bgPab, int winSize, double sigma) {
        FBCoFilterGaussian(imToFilter, imToCollect, filteredImage, fgPab, bgPab, winSize, sigma, Utilities.DEFAULT_ALPHA);
    }

    /**
     * This method applies a lookup-table data on an input image, and stores it in an output image of the same size.
     * @param inputData Input image - hold values from 0 to nBins.
     * @param LUT Lookup table of length nBins.
     * @param outputData Output image.
     * @param nBins the number of different values in inputData, and the number of bins in LUT.
     */
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

