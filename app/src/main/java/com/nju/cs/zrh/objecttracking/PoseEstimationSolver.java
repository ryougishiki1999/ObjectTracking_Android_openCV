package com.nju.cs.zrh.objecttracking;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.nju.cs.zrh.objecttracking.utils.Numerical;
import com.nju.cs.zrh.objecttracking.utils.featurematch.FeatureMatch;
import com.nju.cs.zrh.objecttracking.utils.featurematch.ORBFeatureMatch;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.OnPoseEstimation;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.PoseEstimation2D2D;
import com.nju.cs.zrh.objecttracking.utils.transform.Transform;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
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
    private FeatureMatch mFeatureMatch = new ORBFeatureMatch();
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
    private Mat distortion = new Mat(3, 3, CvType.CV_64FC1);
    private List<Double> distortionList = new ArrayList<>();
    private double k1;
    private double k2;
    private double k3;
    private double p1;
    private double p2;

    private Mat transformMatrix = new Mat(4, 4, CvType.CV_64FC1);
    private Mat transformMatrixBackup = new Mat(4, 4, CvType.CV_64FC1);

    private double lastError = 0.0;
    private static final int frameInterval = 10;
    private static final double filterProportion = 0.8;
    private int frameIntervalCnt = 0;
    private Point curPixel;
    //private Mat lastFrame = new Mat();
    private MatOfKeyPoint lastKeyPoint = null;
    private Mat lastDescriptor = null;
    private MatOfKeyPoint keyPointBackup = null;
    private Mat descriptorBackup = null;

    private static final int radius = 16;
    private static final int thickness = -1;
    private static final int lineType = Imgproc.LINE_8;

    private int totalCnt = 0;
    private List<Mat> inliers = new ArrayList<>();


    private Mat original = new Mat();
    private LinkedList<Mat> triangulationFrameList = new LinkedList<>();
    private Mat triangulation1;
    private Mat triangulation2;
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
        ;

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

        if (curProportion1 >= filterProportion && curProportion2 >= filterProportion) {
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

        if (filterByDescriptors(this.curMatchDescriptor, descriptor)) {
            this.mFeatureMatch.drawMatches(this.curMatchFrame, img, this.curMatchFrameKeyPoint, keyPoint, this.curMatchDescriptor, descriptor, result);
        } else {
            Mat m1 = new Mat(rgbaFrame.rows(), rgbaFrame.cols() / 2, rgbaFrame.type());
            Features2d.drawKeypoints(this.curMatchFrame, this.curMatchFrameKeyPoint, m1);
            Mat m2 = new Mat(rgbaFrame.rows(), rgbaFrame.cols() / 2, rgbaFrame.type());
            Features2d.drawKeypoints(img, keyPoint, m2);

            m1.copyTo(result.colRange(new Range(0, result.cols() / 2)));
            m2.copyTo(result.colRange(new Range(result.cols() / 2, result.cols())));
        }
    }

    public boolean renderMotionFrame(@NonNull Mat rgbaFrame) {

        this.frameIntervalCnt++;

        if (this.frameIntervalCnt < frameInterval) {
            return false;
        }

        this.frameIntervalCnt = 0;

        MatOfKeyPoint curKeyPoint = new MatOfKeyPoint();
        Mat curDescriptor = new Mat();

        this.mFeatureMatch.findKeyPointsAndDescriptors(rgbaFrame, curKeyPoint, curDescriptor);

        if (filterByDescriptors(this.lastDescriptor, curDescriptor)) {

            MatOfDMatch match = new MatOfDMatch();
            this.mFeatureMatch.findFeatureMatchesByDesc(this.lastDescriptor, curDescriptor, match);

            if (this.poseEstimation(this.lastKeyPoint, curKeyPoint, match)) {

                this.keyPointBackup = new MatOfKeyPoint(this.lastKeyPoint);
                this.descriptorBackup = this.lastDescriptor.clone();

                this.lastKeyPoint = new MatOfKeyPoint(curKeyPoint);
                this.lastDescriptor = curDescriptor.clone();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void renderTrackingFrame(@NonNull Mat rgbaFrame) {

        if (renderMotionFrame(rgbaFrame)) {

            Mat R = new Mat();
            Mat T = new Mat();

            Transform.TransformConvertToRT(this.transformMatrix, R, T);

            MatOfPoint2f curPixelMat = new MatOfPoint2f();

            MatOfPoint3f targetPointMat = new MatOfPoint3f(targetPoint);
            Mat rVec = new Mat();
            Mat tVec = T.clone();


            MatOfDouble distCoeffs = new MatOfDouble(distortion);

            Calib3d.Rodrigues(R, rVec);
            Log.d(TAG, "R: " + R.dump());
            Log.d(TAG, "rVec:" + rVec.dump());

            Calib3d.projectPoints(targetPointMat, rVec, tVec, intrinsic, distCoeffs, curPixelMat);

            Point[] curPixels = curPixelMat.toArray();
            curPixel = curPixels[0];

            Log.d(TAG, "curPixel " + curPixel.x + ", " + curPixel.y);
            Log.i(TAG, "curPixel " + curPixel.x + ", " + curPixel.y);

            totalCnt += 1;

            if ((curPixel.x >= 0 && curPixel.x <= this.width) && (curPixel.y >= 0 && curPixel.y <= this.height)) {
                Imgproc.circle(rgbaFrame, curPixel, radius, new Scalar(255, 0, 0, 255), thickness, lineType);
                Mat result = rgbaFrame.clone();
                this.inliers.add(result);
            } else {
                this.lastDescriptor = this.descriptorBackup;
                this.lastKeyPoint = this.keyPointBackup;
                this.transformMatrix = this.transformMatrixBackup;
            }
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

        Mat RDelta = new Mat();
        Mat TDelta = new Mat();

        if (this.mOnPoseEstimation.estimation(keyPoint1, keyPoint2, match, RDelta, TDelta)) {

            Mat transformDelta = Mat.zeros(4, 4, CvType.CV_64FC1);

            Log.d(TAG, "Rotation Matrix:" + RDelta.dump());
            Log.d(TAG, "Translation Vector" + TDelta.dump());

            Transform.convertRTToTransform(RDelta, TDelta, transformDelta);

            Log.d(TAG, "transformation delta " + transformDelta.dump());

            this.transformMatrixBackup = this.transformMatrix.clone();

            Log.d(TAG, "before, transformation matrix: " + transformMatrix.dump());
            Mat resultMatrix = new Mat();
            Core.gemm(transformDelta, transformMatrix, 1.0, new Mat(), 0.0, resultMatrix);
            transformMatrix = resultMatrix.clone();
            Log.d(TAG, "after, transformation matrix: " + transformMatrix.dump());

            return true;
        } else {
            Log.d(TAG, "pose estimation failed!");
            return false;
        }
    }


    private void initTriangulate() {
        this.initTransformMatrix();

        this.totalCnt = 0;
        this.inliers.clear();

        this.triangulation1 = this.triangulationFrameList.get(0).clone();
        this.triangulation2 = this.triangulationFrameList.get(1).clone();

        this.original = this.triangulation1.clone();
        Imgproc.circle(this.original, this.targetPixel, radius, new Scalar(0, 0, 255, 255), thickness, lineType);

        //lastFrame = triangulation2.clone();

        this.triangulationFrameList.clear();
    }

    private void initTransformMatrix() {
        Mat R = Mat.eye(3, 3, CvType.CV_64FC1);
        Mat T = Mat.zeros(3, 1, CvType.CV_64FC1);
        Transform.convertRTToTransform(R, T, this.transformMatrix);
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

        if (this.filterByDescriptors(descriptor1, descriptor2)) {

            this.mFeatureMatch.findFeatureMatchesByDesc(descriptor1, descriptor2, match);

            if (this.poseEstimation(keyPoint1, keyPoint2, match)) {

                this.lastKeyPoint = new MatOfKeyPoint(keyPoint2);
                this.lastDescriptor = descriptor2.clone();

                this.descriptorBackup = this.lastDescriptor;
                this.keyPointBackup = this.lastKeyPoint;

                Mat R = new Mat();
                Mat T = new Mat();

                Transform.TransformConvertToRT(this.transformMatrix, R, T);

                Triangulation.getInstance().triangulation(keyPoint1, keyPoint2, match, intrinsic, R, T, triangulationResult);

                return true;
            } else {
                return false;
            }
        } else {
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
        Mat pixelCoordinatePixel = Mat.ones(3, 1, CvType.CV_64FC1);
        pixelCoordinatePixel.put(0, 0, targetPixel.x);
        pixelCoordinatePixel.put(1, 0, targetPixel.y);
        Log.d(TAG, "pixelCoordinatePixel" + pixelCoordinatePixel.dump());

        Mat intrinsicInverse = intrinsic.inv();
        Log.d(TAG, "inverse Intrinsic" + intrinsicInverse.dump());

        Mat cameraCoordinatePoint = new Mat(3, 1, CvType.CV_64FC1);
        Core.gemm(intrinsicInverse, pixelCoordinatePixel, 1.0, new Mat(), 0.0, cameraCoordinatePoint);

        Mat extrinsic = Mat.eye(4, 4, CvType.CV_64FC1);
        Mat extrinsicInverse = extrinsic.inv();

        Mat worldCoordinatePoint = cameraCoordinatePoint.clone();

        Log.d(TAG, "worldCoordinatePoint: " + worldCoordinatePoint.dump());

        List<Double> worldPointList = new ArrayList<>();
        Converters.Mat_to_vector_double(worldCoordinatePoint, worldPointList);

        x = z * worldPointList.get(0);
        y = z * worldPointList.get(1);

        targetPoint = new Point3(x, y, z);
    }

    public Double getDepth() {
        return depth;
    }

    public double getDepthVariance() {
        return depthVariance;
    }
}
