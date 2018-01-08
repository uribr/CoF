package cofproject.tau.android.cof;

//package com.example.android.coftest;


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
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CoF {
    private static final String TAG = "CoF";
    private static final double SIGMA_SPATIAL_DEFAULT = 2*Math.sqrt(15) + 1;
    private static final int WINDOW_SIZE_DEFAULT = 15;
    private static final Size GAUSSIAN_KERNEL_SIZE_DEFAULT = new Size(WINDOW_SIZE_DEFAULT, WINDOW_SIZE_DEFAULT);
    private static final double SAMPLE_RATE_DEFAULT = 0.1;
    private static final int NUM_BINS_DEFAULT = 256; // quantization bins

    /**
     * @param imToProcess   - an RGB image (Mat object) to process
     * @param filteredImage - Mat object where the filtered image is stored
     */
    public static void applyFilter(Mat imToProcess, Mat filteredImage, Preset params) {
        Log.i(TAG, "applyFilter: Started applying filter");

        // extract parmeters from params
        int iterCnt = params.getNumberOfIteration();
        int winsize = params.getWindowSize();
        if (winsize % 2 == 0) {
            winsize--;
        }
        int hws = winsize / 2; // half window size, floored
        double sigma = params.getSigma();

        iterCnt = 3;
        int nBins = 32;
        sigma = SIGMA_SPATIAL_DEFAULT;



        if (imToProcess.rows() != filteredImage.rows() ||  imToProcess.cols() != filteredImage.cols()) {
            Log.e(TAG, "applyFilter: imToProcess.size() != filteredImage.size()", new IllegalArgumentException("imToProcess.size() != filteredImage.size()"));
        }

        Mat idx;
        Mat mask2Collect;
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

        Size sz = imToProcess.size();

        mask2Collect = Mat.ones(sz, CvType.CV_32FC1); // todo - turn this into a parameter


        pab = Mat.zeros(new Size(nBins, nBins), CvType.CV_32FC1);
        sw.reset();
        sw.start();
        collectPab(idx, mask2Collect, pab, nBins);
        sw.stop();
        Log.d(TAG, "applyFilter: collectPab time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

        mask2Collect.release();


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
        for(int i = 0; i < iterCnt; i++) {
            Log.d(TAG, "applyFilter: cofilter iteration no. " + i);
            coFilter(imToProcessCopy, idx, filteredImage, pmi);
            filteredImage.copyTo(imToProcessCopy);
            System.gc();
        }
        sw.stop();
        Log.d(TAG, "applyCoF: coFilter time: " + sw.elapsed(TimeUnit.MILLISECONDS) / 1000.0 + " seconds");

        imToProcessCopy.release();

        // release all inner Mats:
        idx.release();
        mask2Collect.release();
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

    private static void quantize(Mat rgbInput, Mat quantizedIm) {

        quantize(rgbInput, quantizedIm, NUM_BINS_DEFAULT);
    }

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

        // create a permutation from 0 to (nPoints-1), and shuffle it
        List<Integer> perm = zeroTo(nPoints);
        Collections.shuffle(perm, new Random(System.currentTimeMillis()));

        // choose only the nSamplePoints first row indices from the shuffled list
        for (int i = 0; i < nSamplePoints; i++) {
            rgbReshaped.row(perm.get(i)).copyTo(sampledRgb.row(i));
        }

        rgbReshaped.release(); // don't need it anymore - free memory

        // perform k-means in order to create clusters for the quantization
        labels = new Mat();
        centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, 100, 1.0);
        Core.kmeans(sampledRgb, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

        sampledRgb.release();
        labels.release(); // free unnecessary memory

        Size sz = rgbInput.size();

        // initialize min-err matrix with infinite float values
        minErr = new Mat(sz, CvType.CV_32F, new Scalar(Float.MAX_VALUE));

        float[] currCent = new float[3]; // will hold RGB of the current center

        currCentMat = new Mat(rgbInput32f.size(), rgbInput32f.type());

        minIndices = new Mat();

        for (int i = 0; i < k; i++) {

            Log.d(TAG, "quantize: updating quantized image - level " + Integer.toString(i));

            // initialize a full-sized matrix with the current center values in all its pixels
            centers.get(i, 0, currCent);
            currCentMat.setTo(new Scalar(currCent[0], currCent[1], currCent[2]));

            // calculate the squared difference between each pixel of the input image vs. the current center
            diffMat = new Mat();
            // first subtract
            Core.subtract(rgbInput32f, currCentMat, diffMat);

            // then square the result (pixel-wise)
            Mat diffMatSquared = diffMat.mul(diffMat);

            // we turn diffMat into a 3-column matrix (each line is 1 pixel "RGB" values)
            Mat diffMatReshaped = diffMatSquared.reshape(1, (int) diffMat.total());

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
            diffMatSquared.release();
            diffMatReshaped.release();
            diffMat.release();

        }
        minIndices.release();
        currCentMat.release();
        minErr.release();
        centers.release();
        rgbInput32f.release();
    }


    private static void collectPab(Mat im2Collect, Mat pab) {
        Mat mask2Collect = Mat.ones(im2Collect.size(), CvType.CV_32FC1);
        collectPab(im2Collect, mask2Collect, pab);


    }

    private static void collectPab(Mat im2Collect, Mat mask2Collect, Mat pab) {
        collectPab(im2Collect, mask2Collect, pab, 256);
    }

    private static void collectPab(Mat im2Collect, Mat mask2Collect, Mat pab, int nBins) {
        collectPab(im2Collect, mask2Collect, pab, nBins, null);
    }

    /**
     * @param im2Collect
     * @param mask2Collect
     * @param pab          - output matrix with the co-occurrence statistics
     * @param nBins
     * @param coocFilt
     * @return
     */
    private static void collectPab(Mat im2Collect, Mat mask2Collect, Mat pab, int nBins, Mat coocFilt) {

        Log.i(TAG, "collectPab: started");

        float[][] pabArr = new float[nBins][nBins];

        for (int iLevel = 0; iLevel < nBins; iLevel++) {

            //todo - check this carefully!!!!!!!!
            Mat cmpMat = new Mat();
            Mat masked = new Mat();
            Core.compare(im2Collect, new Scalar(iLevel), cmpMat, Core.CMP_EQ);

            // cmpMat should be a float-type binary Mat (i.e only 0/1, not 0/255)
            //cmpMat.convertTo(cmpMat, CvType.CV_32FC1, 1.0 / 255.0);
            mask2Collect.copyTo(masked, cmpMat);
            cmpMat.release();

            //Mat masked = cmpMat.mul(mask2Collect);

            // todo - handle general filter
            if (coocFilt == null) {
                Mat tmp = new Mat();
                //fixme - handle non-default values
                double sigma = SIGMA_SPATIAL_DEFAULT; // sigma = sigmaX = sigmaY
                Size kersize = GAUSSIAN_KERNEL_SIZE_DEFAULT; // squared kernel
                Imgproc.GaussianBlur(masked, tmp, kersize, sigma, sigma);
                Mat w = tmp.mul(mask2Collect);
                tmp.release();

                // todo - think of something more efficient, accumarray style
                for (int j = 0; j < nBins; j++) {


                    Core.compare(im2Collect, new Scalar(j), cmpMat, Core.CMP_EQ);

                    tmp = Mat.zeros(w.size(), w.type());

                    // copy only values matching to level i indices
                    w.copyTo(tmp, cmpMat);
                    Scalar sc = Core.sumElems(tmp);
                    pabArr[iLevel][j] = (float) sc.val[0];

                    cmpMat.release();
                    tmp.release();
                }
                w.release();
            }
            masked.release();
        }

        float[] flatened = Floats.concat(pabArr);

        pab.put(0, 0, flatened);

        Mat pabt = pab.t();

        Core.add(pab, pabt, pab);
        // normalize
        Scalar s = Core.sumElems(pab);
        Core.divide(pab, s, pab);


        //Core.normalize(pab, pab, 1, 0, Core.NORM_L1);
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
        coFilter(im2Filter, im2Collect, filteredImage, pab, null);

    }

    private static void coFilter(Mat im2Filter, Mat im2Collect, Mat filteredImage, Mat pab, Mat fSmooth) {

        Log.i(TAG, "coFilter: started");
        if (fSmooth == null) { //todo - handle non-null case

            //fixme - handle non-default case
            double sigma = SIGMA_SPATIAL_DEFAULT; // sigma = sigmaX = sigmaY
            Size kerSize = GAUSSIAN_KERNEL_SIZE_DEFAULT;

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
            Mat[] filtImChans = new Mat[filteredImage.channels()];

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

            // smooth per channel
            for (int iChannel = 0; iChannel < im2Filter.channels(); iChannel++) {

                Log.d(TAG, "coFilter: smoothing channel " + iChannel);
                Mat cim = channels2Filter.get(iChannel);
                Mat mVals = channels2Collect.get(iChannel);

                byte[] mValsArr = new byte[(int) mVals.total()];
                mVals.get(0, 0, mValsArr);

                Mat wpl = new Mat(mVals.size(), CvType.CV_32FC1);
                float[] wplArr = new float[mValsArr.length]; // weight per level arr

                Mat pabCurrChan = channelsPab.get(iChannel);

                // our LUT is the iLevel-th row of the current channel pab matrix
                float[] LUT = new float[pabCurrChan.cols()];

                Mat sumW = Mat.zeros(mVals.size(), CvType.CV_32FC1);
                Mat sumS = Mat.zeros(mVals.size(), CvType.CV_32FC1);

                // per level smoothing
                for (int iLevel = 0; iLevel < nBins; iLevel++) {
                    System.gc();

                    Log.d(TAG, "coFilter: smoothing level " + iLevel + " in channel " + iChannel);

                    Mat cmpMat = new Mat();
                    Mat tmp = new Mat();
                    Core.compare(mVals, new Scalar(iLevel), cmpMat, Core.CMP_EQ);
                    cmpMat.convertTo(cmpMat, CvType.CV_32FC1, 1.0 / 255.0); // binary matrix

                    pabCurrChan.get(iLevel, 0, LUT);

                    applyLUTonData(mValsArr, LUT, wplArr);
                    wpl.put(0, 0, wplArr);


                    // smooth wpl
                    Imgproc.GaussianBlur(wpl, tmp, kerSize, sigma, sigma);

                    // take only necessary pixels
                    Mat W = tmp.mul(cmpMat);

                    Imgproc.accumulate(W, sumW);


                    tmp.release();

                    tmp = wpl.mul(cim);
                    Imgproc.GaussianBlur(tmp, tmp, kerSize, sigma, sigma);
                    Mat S = tmp.mul(cmpMat);
                    Imgproc.accumulate(S, sumS);

                    cmpMat.release();
                    tmp.release();
                    W.release();
                    S.release();

                }

                Core.add(sumW, new Scalar(Float.MIN_NORMAL), sumW);
                filtImChans[iChannel] = new Mat();

                Core.divide(sumS, sumW, filtImChans[iChannel]);


                sumS.release();
                sumW.release();
                pabCurrChan.release();
                wpl.release();
                mVals.release();
                cim.release();
            }
            Core.merge(Arrays.asList(filtImChans), filteredImage);

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
        }
    }


    private static void applyLUTonData(byte[] inputData, float[] lut, float[] outputData) {

        if (inputData.length != outputData.length) {
            Log.e(TAG, "applyLUTonData: inputData.length != outputData.length", new IllegalArgumentException("inputData.length != outputData.length"));
        }

        byte[] tmp = inputData.clone();
        Arrays.sort(tmp);
        if (tmp[0] < 0 || tmp[tmp.length - 1] > lut.length) {
            Log.e(TAG, "applyLUTonData: Invalid values in inputData", new IllegalArgumentException("Invalid values in inputData"));
        }

        for (int i = 0; i < inputData.length; i++) {
            outputData[i] = lut[inputData[i]];
        }
    }
}

