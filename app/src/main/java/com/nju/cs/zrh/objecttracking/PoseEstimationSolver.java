package com.nju.cs.zrh.objecttracking;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.nju.cs.zrh.objecttracking.utils.Numerical;
import com.nju.cs.zrh.objecttracking.utils.featurematch.FeatureMatch;
import com.nju.cs.zrh.objecttracking.utils.featurematch.SIFTMatch;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.OnPoseEstimation;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.PoseEstimation2D2D;
import com.nju.cs.zrh.objecttracking.utils.transform.Transform;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PoseEstimationSolver {

    private static final String TAG = "PoseEstimationSolver";


    private static final String galleryPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator + "Camera" + File.separator + "objectTracking" + File.separator;

    private static final int cntThreshold = 30;
    private LinkedList<Mat> resultList = new LinkedList<>();
    //private FeatureMatch mFeatureMatch = new ORBFeatureMatch();
    private FeatureMatch mFeatureMatch = new SIFTMatch();
    private OnPoseEstimation mOnPoseEstimation;

    private int width;
    private int height;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private Mat intrinsic = new Mat(3, 3, CvType.CV_64FC1);
    private List<Double> intrinsicList = new ArrayList<>();
    private double fx;
    private double fy;
    private double cx;
    private double cy;
    private Mat distortion = new Mat(5, 1, CvType.CV_64FC1);
    private List<Double> distortionList = new ArrayList<>();
    private double k1;
    private double k2;
    private double k3;
    private double p1;
    private double p2;

    private Mat transformMatrix = new Mat(4, 4, CvType.CV_64FC1);
    private Mat transformMatrixBackup = new Mat(4, 4, CvType.CV_64FC1);

    private double lastError = 0.0;
    private static final int FrameInterval = 3;
    private static final double FilterProportion = 0.8;
    private int frameIntervalCnt = 0;
    private Point curPixel;

    private Mat lastFrame = null;
    private MatOfKeyPoint lastKeyPoint = null;
    private Mat lastDescriptor = null;
    private MatOfKeyPoint lastKeyPointBackup = null;
    private Mat lastDescriptorBackup = null;
    private MatOfDMatch matchMat = null;

    private static final int radius = 12;
    private static final int thickness = -1;
    private static final int lineType = Imgproc.LINE_8;

    private int totalCnt = 0;
    private int successCnt = 0;
    private int failureCnt = 0;
    private List<Mat> inliers = new ArrayList<>();
    private List<Point> trackingPixelList = new ArrayList<>();
    private List<Mat> transformMatrixList = new ArrayList<>();
    private List<Mat> matchFrameList = new ArrayList<>();

    private Mat original = new Mat();
    private LinkedList<Mat> triangulationFrameList = new LinkedList<>();
    private Mat triangulation1;
    private Mat triangulation2;
    private MatOfKeyPoint triangulationKeypoint1;
    private MatOfKeyPoint triangulationKeypoint2;
    private Point targetPixel;
    private Point3 targetPoint;
    private Double depth;
    private double depthVariance;
    private static final int keyPointsAroundThreshold = 5;
    private Map<Point, Point3> triangulationResult = new HashMap();

    private Mat curMatchFrame = null;
    private MatOfKeyPoint curMatchFrameKeyPoint = null;
    private Mat curMatchDescriptor = null;

    public PoseEstimationSolver(Mat intrinsic, Mat distortion) {
        this.intrinsic = intrinsic.clone();
        this.distortion = distortion.clone();

        double[] intrinsicArray = new double[9];
        double[] distortionArray = new double[5];
        intrinsic.get(0, 0, intrinsicArray);
        distortion.get(0, 0, distortionArray);

        for (int i = 0; i < 5; i++) {
            distortionList.add(i, distortionArray[i]);
        }

        for (int i = 0; i < 9; i++) {
            intrinsicList.add(i, intrinsicArray[i]);
        }

        fx = intrinsicList.get(0);
        fy = intrinsicList.get(4);
        cx = intrinsicList.get(2);
        cy = intrinsicList.get(5);

        k1 = distortionList.get(0);
        k2 = distortionList.get(1);
        k3 = distortionList.get(2);
        p1 = distortionList.get(3);
        p2 = distortionList.get(4);

        mOnPoseEstimation = new OnPoseEstimation(new PoseEstimation2D2D(this.intrinsic));
    }

    public void setTargetPixel(Point targetPixel) {
        this.targetPixel = targetPixel;
    }

    private boolean filterByDescriptors(@NonNull Mat descriptor1, @NonNull Mat descriptor2) {
//        double curError = 0.0;
//        int rows1 = descriptor1.rows();
//        int rows2 = descriptor2.rows();
//        int totalNum = rows1;
//        double totalError = 0.0;
//
//        for (int i = 0; i < rows1; i++) {
//            Mat desc1RowI = descriptor1.row(i);
//            double minErrorI = Double.MAX_VALUE;
//
//            for (int j = 0; j < rows2; j++) {
//                Mat desc2RowJ = descriptor2.row(j);
//                int cols = desc1RowI.cols();
//
//                Log.d(TAG, desc1RowI.dump());
//                Log.d(TAG, desc2RowJ.dump());
//
//                double error = Core.norm(desc1RowI, desc2RowJ, Core.NORM_L2);
//
//                minErrorI = Math.min(minErrorI, error);
//            }
//
//            totalError += minErrorI * minErrorI;
//        }
//
//        curError = Math.sqrt(totalError / totalNum);
//
//        if (lastError == 0.0) {
//            lastError = curError;
//            return true;
//        } else {
//            if (lastError / curError < filterProportion) {
//                Log.d(TAG, "descriptors deviation excessively");
//                return false;
//            } else {
//                lastError = curError;
//                return true;
//            }
//        }

        int rows1 = descriptor1.rows();
        int rows2 = descriptor2.rows();

        double curProportion1 = rows1 / (double) rows2;
        double curProportion2 = rows2 / (double) rows1;

        if (curProportion1 >= FilterProportion && curProportion2 >= FilterProportion) {
            return true;
        } else {
            Log.d(TAG, "descriptors deviation excessively");
            return false;
        }
    }

    public void renderFeatureExtractionFrame(@NonNull Mat rgbaFrame) {
        this.mFeatureMatch.drawKeyPoints(rgbaFrame);
    }

    public void setCurMatchFrame(Mat curMatchFrame) {
        this.curMatchFrame = curMatchFrame.colRange(new Range(0, curMatchFrame.width() / 2));

        this.curMatchFrameKeyPoint = new MatOfKeyPoint();
        this.curMatchDescriptor = new Mat();

        this.mFeatureMatch.findKeyPointsAndDescriptors(this.curMatchFrame, this.curMatchFrameKeyPoint, this.curMatchDescriptor);
    }

    public void renderFeatureMatchFrame(@NonNull Mat rgbaFrame, @NonNull Mat result) {

        Mat img = rgbaFrame.colRange(new Range(0, rgbaFrame.width() / 2));
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();
        Mat descriptor = new Mat();

        MatOfDMatch match = new MatOfDMatch();

        this.mFeatureMatch.findKeyPointsAndDescriptors(img, keyPoint, descriptor);

        //this.mFeatureMatch.drawMatches(this.curMatchFrame, img, this.curMatchFrameKeyPoint, keyPoint, this.curMatchDescriptor, descriptor, result);

//        if (filterByDescriptors(this.curMatchDescriptor, descriptor)) {
//            this.mFeatureMatch.drawMatches(this.curMatchFrame, img, this.curMatchFrameKeyPoint, keyPoint, this.curMatchDescriptor, descriptor, result);
//        } else {
//            Mat m1 = new Mat(rgbaFrame.rows(), rgbaFrame.cols() / 2, rgbaFrame.type());
//            Features2d.drawKeypoints(this.curMatchFrame, this.curMatchFrameKeyPoint, m1);
//            Mat m2 = new Mat(rgbaFrame.rows(), rgbaFrame.cols() / 2, rgbaFrame.type());
//            Features2d.drawKeypoints(img, keyPoint, m2);
//
//            m1.copyTo(result.colRange(new Range(0, result.cols() / 2)));
//            m2.copyTo(result.colRange(new Range(result.cols() / 2, result.cols())));
//        }
        MatOfKeyPoint curFrameKeyPoint = new MatOfKeyPoint(this.curMatchFrameKeyPoint.clone());
        this.mFeatureMatch.drawMatches(this.curMatchFrame, img, curFrameKeyPoint, keyPoint, this.curMatchDescriptor, descriptor, result);
        return;
    }

    public boolean renderMotionFrame(@NonNull Mat rgbaFrame) {

        this.frameIntervalCnt++;

        if (this.frameIntervalCnt < FrameInterval) {
            return false;
        }

        this.frameIntervalCnt = 0;

        MatOfKeyPoint curKeyPoint = new MatOfKeyPoint();
        Mat curDescriptor = new Mat();

        this.mFeatureMatch.findKeyPointsAndDescriptors(rgbaFrame, curKeyPoint, curDescriptor);

        MatOfDMatch match = new MatOfDMatch();
        List<KeyPoint> lastBackupKeyPointList = this.lastKeyPoint.toList();
        List<KeyPoint> curBackupKeyPointList = curKeyPoint.toList();

        /**
         * 相邻关键帧应该差别不应太大
         */
        if (filterByDescriptors(this.lastDescriptor, curDescriptor)) {

        } else {
            return false;
        }

        if (this.mFeatureMatch.findMatchesByDesc(this.lastDescriptor, curDescriptor, this.lastKeyPoint, curKeyPoint, match)) {
            if (this.poseEstimation(this.lastKeyPoint, curKeyPoint, match)) {

                this.matchMat.fromList(match.toList());
                this.triangulationKeypoint1.fromList(this.lastKeyPoint.toList());
                this.triangulationKeypoint2.fromList(curKeyPoint.toList());

                this.lastKeyPointBackup.fromList(lastBackupKeyPointList);
                this.lastDescriptorBackup = this.lastDescriptor.clone();

                this.lastKeyPoint.fromList(curBackupKeyPointList);
                this.lastDescriptor = curDescriptor.clone();

                return true;
            } else {
                /**
                 * 涉及位姿计算，但ret value为false，代表未改变transform Matrix,肯定改变了this.lastKeypoint
                 */
                this.lastKeyPoint.fromList(lastBackupKeyPointList);
                return false;
            }
        } else {
            /**
             * 搜寻匹配对数过少，尚未涉及位姿估计,可能改变了this.lastKeyPoint
             */
            this.lastKeyPoint.fromList(lastBackupKeyPointList);
            return false;
        }
    }

    public void renderTrackingFrame(@NonNull Mat rgbaFrame) {

        if (renderMotionFrame(rgbaFrame)) {
            this.totalCnt += 1;

            Mat R = new Mat(3, 3, CvType.CV_64FC1);
            Mat T = new Mat(3, 1, CvType.CV_64FC1);

            Transform.TransformConvertToRT(this.transformMatrix, R, T);

            MatOfPoint3f targetPointMat = new MatOfPoint3f(targetPoint);
            MatOfPoint2f curPixelMat = new MatOfPoint2f();

            MatOfDouble rVec = new MatOfDouble();
            MatOfDouble tVec = new MatOfDouble(T);
            MatOfDouble distCoeffs = new MatOfDouble(distortion);

            Calib3d.Rodrigues(R, rVec);
            Log.d(TAG, "tracking R: \n" + R.dump());
            Log.d(TAG, "tracking T \n" + T.dump());
            Log.d(TAG, "rVec: \n" + rVec.dump());
            Log.d(TAG, "tVec: \n" + tVec.dump());

            Calib3d.projectPoints(targetPointMat, rVec, tVec, intrinsic, distCoeffs, curPixelMat);

            Point[] curPixels = curPixelMat.toArray();
            curPixel = curPixels[0];

            if ((curPixel.x >= 0 && curPixel.x <= this.width) && (curPixel.y >= 0 && curPixel.y <= this.height)) {
                this.successCnt += 1;
                Mat result = new Mat();
                result = rgbaFrame.clone();
                Imgproc.circle(result, curPixel, radius, new Scalar(255, 0, 0, 255), thickness, lineType);
                this.inliers.add(result);
                this.trackingPixelList.add(curPixel);

                this.transformMatrixList.add(this.transformMatrix);
                Mat matchFrame = new Mat();
                Features2d.drawMatches(this.lastFrame, this.triangulationKeypoint1, result, this.triangulationKeypoint2, this.matchMat, matchFrame, Scalar.all(-1), Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
                this.matchFrameList.add(matchFrame);

                this.lastFrame = result.clone();

                rgbaFrame = result;

            } else {
                /**
                 * 通过特征匹配和位姿计算，结果仍偏离可能值，进行回溯
                 */
                this.failureCnt += 1;
                this.lastKeyPoint.fromList(this.lastKeyPointBackup.toList());
                this.lastDescriptor = this.lastDescriptorBackup.clone();
                this.transformMatrix = this.transformMatrixBackup.clone();

                Log.d(TAG, "trace back:" + transformMatrix.dump());
            }
            return;
        } else {
            this.totalCnt += 1;
            return;
        }


//        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
//        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();
//        MatOfDMatch match = new MatOfDMatch();
//        Mat RDelta = new Mat();
//        Mat TDelta = new Mat();
//
//        if (this.poseEstimation(lastFrame, rgbaFrame, keyPoint1, keyPoint2, match, RDelta, TDelta)) {
//            lastFrame = rgbaFrame.clone();
//
//            Mat R = new Mat();
//            Mat T = new Mat();
//            Transform.TransformConvertToRT(transformMatrix, R, T);
//            Log.d(TAG, "transformation matrix: " + transformMatrix.dump());
//            Log.d(TAG, "Rotation Matrix:" + R.dump());
//            Log.d(TAG, "Translation Vector" + T.dump());
//
////        Mat targetPointMatrix = Mat.zeros(3, 1, CvType.CV_64FC1);
////        targetPointMatrix.put(0, 0, targetPoint.x);
////        targetPointMatrix.put(1, 0, targetPoint.y);
////        targetPointMatrix.put(2, 0, targetPoint.z);
////
////        Mat cameraNormalizeCoordinate = new Mat(3, 1, CvType.CV_64FC1);
////        Mat temp = new Mat(3, 1, CvType.CV_64FC1);
////        //Core.multiply(R, targetPointMatrix, temp);
////        Core.gemm(R, targetPointMatrix, 1.0, new Mat(), 0.0, temp);
////
////        Core.add(temp, T, cameraNormalizeCoordinate);
////        Log.d(TAG, "1 camera: " + cameraNormalizeCoordinate.dump());
////
////        //Mat I = Mat.eye(3, 3, CvType.CV_64FC1);
////        //cameraNormalizeCoordinate = cameraNormalizeCoordinate.mul(I, 1 / depth);
////        //Core.gemm(cameraNormalizeCoordinate, I, 1 / depth, new Mat(), 0.0, cameraNormalizeCoordinate);
////        //Log.d(TAG, "1 camera: " + cameraNormalizeCoordinate.dump());
////
////        List<Double> cameraNormalizeCoordinateList = new ArrayList<>();
////        Converters.Mat_to_vector_double(cameraNormalizeCoordinate, cameraNormalizeCoordinateList);
////
////        for (int i = 0; i < cameraNormalizeCoordinateList.size(); i++) {
////            cameraNormalizeCoordinateList.set(i, cameraNormalizeCoordinateList.get(i) / depth);
////        }
////
////        double x = cameraNormalizeCoordinateList.get(0);
////        double y = cameraNormalizeCoordinateList.get(1);
////        double r = Math.sqrt(x * x + y * y);
////        double xDistorted = x * (1 + k1 * Math.pow(r, 2) + k2 * Math.pow(r, 4) + k3 * Math.pow(r, 6)) + 2 * p1 * x * y + p2 * (r * r + 2 * x * x);
////        double yDistorted = y * (1 + k1 * Math.pow(r, 2) + k2 * Math.pow(r, 4) + k3 * Math.pow(r, 6)) + 2 * p2 * x * y + p1 * (r * r + 2 * y * y);
////        double u, v;
////        u = fx * xDistorted + cx;
////        v = fy * yDistorted + cy;
////
////        Log.d(TAG, "before copy:" + rgbaFrame.dump());
////        rgbaFrame.copyTo(last);
////        Log.d(TAG, "after copy:" + rgbaFrame.dump());
////        Point curPixel = new Point(u, v);
//
//            MatOfPoint2f curPixelMat = new MatOfPoint2f();
//
//            MatOfPoint3f targetPointMat = new MatOfPoint3f(targetPoint);
//            Mat rVec = new Mat();
//            Mat tVec = T.clone();
//
//            Log.d(TAG, "T " + T.dump());
//            Log.d(TAG, "tvec" + tVec.dump());
//
//            MatOfDouble distCoeffs = new MatOfDouble(distortion);
//
//            Log.d(TAG, "distCoeffs: " + distCoeffs.dump());
//            Log.d(TAG, "distortion:" + distortion.dump());
//
//            Calib3d.Rodrigues(R, rVec);
//
//            Log.d(TAG, "R: " + R.dump());
//            Log.d(TAG, "rVec:" + rVec.dump());
//            Calib3d.projectPoints(targetPointMat, rVec, tVec, intrinsic, distCoeffs, curPixelMat);
//
//            Point[] curPixels = curPixelMat.toArray();
//            curPixel = curPixels[0];
//
//            Log.d(TAG, "curPixel " + curPixel.x + ", " + curPixel.y);
//
//            Imgproc.circle(rgbaFrame, curPixel, radius, new Scalar(255, 0, 0, 255), thickness, lineType);
//        } else {
//            Log.d(TAG, "renderTrackingFrame failed!");
//        }
    }

    public void saveTrackingResult() {

        File file = null;
        try {
            file = new File(galleryPath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            e.getStackTrace();
        }


        Bitmap savedBMP = Bitmap.createBitmap(original.cols(), original.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(original, savedBMP);
        saveBMP2Gallery(savedBMP, "original");

        for (int i = 0; i < this.inliers.size(); i++) {
            String picName = "tracking" + i;
            Bitmap bmp = Bitmap.createBitmap(this.inliers.get(i).cols(), this.inliers.get(i).rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(this.inliers.get(i), bmp);
            this.saveBMP2Gallery(bmp, picName);
        }

        File statics = new File(galleryPath, "statics.txt");
        try {
            statics.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File transFile = new File(galleryPath, "transforms.txt");
        try {
            transFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("intrinsic: ").append(this.intrinsic.dump()).append('\n');
        sb.append("discoeffs: ").append(this.distortion.dump()).append('\n');
        sb.append("target Point: ").append(this.targetPoint.toString()).append('\n');
        sb.append("total cnt: ").append(this.totalCnt).append('\n');
        sb.append("success cnt: ").append(this.successCnt).append('\n');
        sb.append("failure cnt: ").append(this.failureCnt).append('\n');

        for (int i = 0; i < this.trackingPixelList.size(); i++) {
            Point p = trackingPixelList.get(i);
            sb.append(i).append(" ").append(p.x).append(",").append(p.y).append('\n');
        }


        try {
            FileOutputStream fos = new FileOutputStream(statics);

            try {
                fos.write(sb.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            fos = new FileOutputStream(transFile);

            sb = new StringBuilder();

            for (int i = 0; i < this.transformMatrixList.size(); i++) {
                Mat transform = transformMatrixList.get(i);
                sb.append(i).append(": \n").append(transform.dump()).append("\n\n");
            }

            try {
                fos.write(sb.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < this.matchFrameList.size(); i++) {
            String picName = "match" + i;
            Bitmap bmp = Bitmap.createBitmap(this.matchFrameList.get(i).cols(), this.matchFrameList.get(i).rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(this.matchFrameList.get(i), bmp);
            this.saveBMP2Gallery(bmp, picName);
        }

    }

    private void saveBMP2Gallery(Bitmap bmp, String picName) {

        File file = null;
        FileOutputStream outputStream = null;

        try {
            file = new File(galleryPath, picName + ".jpg");

            outputStream = new FileOutputStream(file);

            if (outputStream != null) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            }

        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addTriangulationFrame(@NonNull Mat rgbaFrame) {
        triangulationFrameList.add(rgbaFrame);
    }

    public int getTriangulationFrameListSize() {
        return triangulationFrameList.size();
    }

    public LinkedList<Mat> getTriangulationFrameList() {
        return triangulationFrameList;
    }

    private boolean poseEstimation(@NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull MatOfDMatch match) {

        Mat RDelta = new Mat(3, 3, CvType.CV_64FC1);
        Mat TDelta = new Mat(3, 1, CvType.CV_64FC1);

        if (this.mOnPoseEstimation.estimation(keyPoint1, keyPoint2, match, RDelta, TDelta)) {

//            Log.d(TAG, "Rotation Matrix:" + RDelta.dump());
//            Log.d(TAG, "Translation Vector" + TDelta.dump());
            Mat transformDelta = Mat.zeros(4, 4, CvType.CV_64FC1);
            Transform.convertRTToTransform(RDelta, TDelta, transformDelta);

            Log.d(TAG, "transformation delta " + transformDelta.dump());
            this.transformMatrixBackup = this.transformMatrix.clone();

            Log.d(TAG, "before, transformation matrix: " + transformMatrix.dump());
            Mat resultMatrix = new Mat();
            Core.gemm(transformDelta, this.transformMatrix, 1.0, new Mat(), 0.0, resultMatrix);
            //transformMatrix = resultMatrix.clone();
            resultMatrix.copyTo(transformMatrix);
            Log.d(TAG, "after, transformation matrix: " + transformMatrix.dump());

            return true;
        } else {
            Log.d(TAG, "pose estimation failed!");
            return false;
        }
    }

    private void initTransformMatrix() {
        Mat R = Mat.eye(3, 3, CvType.CV_64FC1);
        Mat T = Mat.zeros(3, 1, CvType.CV_64FC1);
        Transform.convertRTToTransform(R, T, this.transformMatrix);
        this.transformMatrixBackup = this.transformMatrix.clone();
    }

    private void initTriangulate() {
        this.initTransformMatrix();

        this.totalCnt = 0;
        this.successCnt = 0;
        this.failureCnt = 0;

        this.inliers.clear();

        this.triangulation1 = this.triangulationFrameList.get(0).clone();
        this.triangulation2 = this.triangulationFrameList.get(1).clone();

        this.original = this.triangulation1.clone();
        Imgproc.circle(this.original, this.targetPixel, radius, new Scalar(0, 0, 255, 255), thickness, lineType);

        this.triangulationFrameList.clear();
    }

    public boolean triangulateEntry() {
        if (triangulationFrameList.size() == 2) {

            this.initTriangulate();

            if (this.triangulateKeyPoints(triangulation1, triangulation2)) {
                this.computeDepth();
                this.computeTargetPoint();
                return true;
            } else {
                return false;
            }
        } else {
            Log.d(TAG, "Buffer size should be 2");
            return false;
        }
    }

    private boolean triangulateKeyPoints(@NonNull Mat img1, @NonNull Mat img2) {
        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();
        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();
        MatOfDMatch match = new MatOfDMatch();
        Mat RDelta = new Mat();
        Mat TDelta = new Mat();

        this.mFeatureMatch.findKeyPointsAndDescriptors(img1, keyPoint1, descriptor1);
        this.mFeatureMatch.findKeyPointsAndDescriptors(img2, keyPoint2, descriptor2);

        List<KeyPoint> curBackupKeyPointList = keyPoint2.toList();

        /***
         * SIFT Feature Match
         */

        if (this.mFeatureMatch.findMatchesByDesc(descriptor1, descriptor2, keyPoint1, keyPoint2, match)) {
            if (this.poseEstimation(keyPoint1, keyPoint2, match)) {

                Mat R = new Mat();
                Mat T = new Mat();

                Transform.TransformConvertToRT(this.transformMatrix, R, T);
                Triangulation.getInstance().triangulation(keyPoint1, keyPoint2, match, intrinsic, R, T, triangulationResult);

                this.lastFrame = new Mat();

                this.lastKeyPoint = new MatOfKeyPoint();
                this.lastKeyPoint.fromList(curBackupKeyPointList);
                this.lastDescriptor = descriptor2.clone();

                this.lastKeyPointBackup = new MatOfKeyPoint();
                this.lastKeyPointBackup.fromList(curBackupKeyPointList);
                this.lastDescriptor = descriptor2.clone();

                this.matchMat = new MatOfDMatch(match);
                this.triangulationKeypoint1 = new MatOfKeyPoint(keyPoint1);
                this.triangulationKeypoint2 = new MatOfKeyPoint(keyPoint2);
                return true;
            } else {
                /**
                 * 位姿估计错误，未改变transformMatrix, keyPoint2肯定变化，重启过程无需重置KeyPoint2
                 */
                return false;
            }
        } else {
            /**
             * 为涉及位姿计算，但KeyPoint2有可能变化,重启过程无需重置KeyPoint2
             */
            return false;
        }
    }

    private void computeDepth() {
        if (triangulationResult.containsKey(targetPixel)) {
            depth = triangulationResult.get(targetPixel).z;
            return;
        }

        List<Double> depthDistribution = new ArrayList<>();

        List<Double> depthCandidateList = new ArrayList<>();

        Set<Point> pixelSet = triangulationResult.keySet();

        List<Point> pixelList = new ArrayList<>(pixelSet);

        Collections.sort(pixelList, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return (int) (Numerical.get2DEuclideanDistancePow(targetPixel, o1) - Numerical.get2DEuclideanDistancePow(targetPixel, o2));
            }
        });

        for (int i = 0; i < keyPointsAroundThreshold && i < pixelList.size(); i++) {
            depthDistribution.add(triangulationResult.get(pixelList.get(i)).z);
        }

//        for (Map.Entry<Point, Point3> entry : featurePixelstoPointsMap.entrySet()) {
//            depthDistribution.add(entry.getValue().z);
//        }

//        Map<Point, Point3> copyMap = new HashMap<>(featurePixelstoPointsMap);
//
//        Set<Point> pixelSet = copyMap.keySet();
//
//        List<Point> pixelList = new ArrayList<>(pixelSet);
//
//        Collections.sort(pixelList, new Comparator<Point>() {
//            @Override
//            public int compare(Point o1, Point o2) {
//                return (int) (Numerical.get2DEuclideanDistance(targetPixel, o1) - Numerical.get2DEuclideanDistance(targetPixel, o2));
//            }
//        });
//
//        int count = 0;
//        while (count < threshold) {
//            depthDistribution.add(count, Objects.requireNonNull(copyMap.get(pixelList.get(count))).z);
//            count++;
//        }

//        while (true) {
//            Set<Map.Entry<Point, Point3>> entrySet = new HashSet<>(copyMap.entrySet());
//
//            for (Map.Entry<Point, Point3> entry : entrySet) {
//                Point pixel = entry.getKey();
//                Point3 point = entry.getValue();
//
//                if (Numerical.get2DEuclideanDistance(pixel, targetPixel) <= dist) {
//                    depthDistribution.add(point.z);
//                    copyMap.remove(pixel);
//                    count++;
//                    if (count >= threshold) {
//                        depth = Numerical.getMeans(depthDistribution);
//                        depthVariance = Numerical.getVariance(depthDistribution);
//                        Log.d(TAG, "triangulate depth " + depth);
//                        Log.d(TAG, "triangulate depth variance " + depthVariance);
//                        radius = (int) dist;
//                        return;
//                    }
//                }
//            }
//            dist = Math.pow(dist, 2);
//
//            if (dist >= 1000) {
//                break;
//            }
//        }
        depth = Numerical.getMeans(depthDistribution);
        depthVariance = Numerical.getVariance(depthDistribution);
        Log.d(TAG, "triangulate depth " + depth);
        Log.d(TAG, "triangulate depth variance " + depthVariance);

        return;
    }

    private void computeTargetPoint() {
        double x, y, z;
        z = depth;

        Mat pixelCoordinatePixel = Mat.zeros(3, 1, CvType.CV_64FC1);
        pixelCoordinatePixel.put(0, 0, targetPixel.x * z);
        pixelCoordinatePixel.put(1, 0, targetPixel.y * z);
        pixelCoordinatePixel.put(2, 0, z);
        Log.d(TAG, "pixelCoordinatePixel" + pixelCoordinatePixel.dump());


        Mat intrinsicInverse = intrinsic.inv();
        Log.d(TAG, "inverse Intrinsic" + intrinsicInverse.dump());

//        Mat test = new Mat();
//        Core.gemm(intrinsicInverse, intrinsic, 1.0, new Mat(), 0.0, test);
//        Log.d(TAG, "test inverse: " + test.dump());

        Mat cameraCoordinatePoint = new Mat(3, 1, CvType.CV_64FC1);
        Core.gemm(intrinsicInverse, pixelCoordinatePixel, 1.0, new Mat(), 0.0, cameraCoordinatePoint);

        Mat extrinsic = Mat.eye(4, 4, CvType.CV_64FC1);
        Mat extrinsicInverse = extrinsic.inv();

        Mat worldCoordinatePoint = cameraCoordinatePoint.clone();

        Log.d(TAG, "worldCoordinatePoint: " + worldCoordinatePoint.dump());

        List<Double> worldPointList = new ArrayList<>();
        Converters.Mat_to_vector_double(worldCoordinatePoint, worldPointList);

        x = worldPointList.get(0);
        y = worldPointList.get(1);
        z = worldPointList.get(2);

        targetPoint = new Point3(x, y, z);

        /**
         *  test targetPoint
         */
        Mat RTest = Mat.eye(3, 3, CvType.CV_64FC1);
        Mat TTest = Mat.zeros(3, 1, CvType.CV_64FC1);
        Mat rvecTest = new Mat();
        Calib3d.Rodrigues(RTest, rvecTest);

        Log.d(TAG, "rvecTest: \n" + rvecTest.dump());
        Log.d(TAG, "TTest: \n" + TTest.dump());

        MatOfPoint3f objectPoint = new MatOfPoint3f(targetPoint);
        MatOfPoint2f imagePoint = new MatOfPoint2f();
        MatOfDouble disCoeffs = new MatOfDouble(distortion);
        Calib3d.projectPoints(objectPoint, rvecTest, TTest, intrinsic, disCoeffs, imagePoint);

        List<Point> imagePointList = imagePoint.toList();
        Point targetImagePoint = imagePointList.get(0);

        Imgproc.circle(this.triangulation1, targetImagePoint, radius, new Scalar(255, 0, 0, 255), thickness, lineType);
        this.inliers.add(this.triangulation1.clone());
        this.trackingPixelList.add(targetImagePoint);
        this.successCnt++;

        /**
         * test triangulate 2nd frame
         */

        Mat R = new Mat(3, 3, CvType.CV_64FC1);
        Mat T = new Mat(3, 1, CvType.CV_64FC1);
        Log.d(TAG, "test triangulate, transform: \n" + this.transformMatrix.dump());
        Transform.TransformConvertToRT(this.transformMatrix, R, T);
        Log.d(TAG, "test triangulate, transform: \n" + this.transformMatrix.dump());
        Log.d(TAG, "test triangulate, R: \n" + R.dump());
        Log.d(TAG, "test triangulate, T: \n" + T.dump());

        Calib3d.Rodrigues(R, rvecTest);
        Log.d(TAG, "rvecTest: \n" + rvecTest.dump());
        Calib3d.projectPoints(objectPoint, rvecTest, T, intrinsic, disCoeffs, imagePoint);
        imagePointList = imagePoint.toList();
        targetImagePoint = imagePointList.get(0);
        Imgproc.circle(this.triangulation2, targetImagePoint, radius, new Scalar(255, 0, 0, 255), thickness, lineType);
        this.inliers.add(this.triangulation2.clone());
        this.trackingPixelList.add(targetImagePoint);
        this.successCnt++;

        this.lastFrame = this.triangulation2.clone();
        Mat matchFrame = new Mat();
        Features2d.drawMatches(this.triangulation1, this.triangulationKeypoint1, this.triangulation2, this.triangulationKeypoint2, this.matchMat, matchFrame, Scalar.all(-1), Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
        this.matchFrameList.add(matchFrame);
    }

    public Double getDepth() {
        return depth;
    }

    public double getDepthVariance() {
        return depthVariance;
    }
}
