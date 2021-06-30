package com.nju.cs.zrh.objecttracking.utils.poseestimation;

import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class PoseEstimation2D2D extends PoseEstimation {

    private final static String TAG = "PoseEstimation2D2D";

    private Mat intrinsic = new Mat(3, 3, CvType.CV_64FC1);

    private final static int NUM_THRESHOLD = 8;
    private static final double prob = 0.99;
    private static final double threshold = 1.0;

    public PoseEstimation2D2D(Mat intrinsic) {
        this.intrinsic = intrinsic;
    }

    public boolean estimation(@NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull MatOfDMatch matches, Mat R, Mat T) {

        List<Point> pointList1 = new ArrayList<>();
        List<Point> pointList2 = new ArrayList<>();

        List<DMatch> matchList = matches.toList();

        if (matchList.size() < NUM_THRESHOLD) {
            Log.d(TAG, "num of match lesser than NUM THRESHOLD");
            return false;
        }

        List<KeyPoint> keyPointList1 = keyPoint1.toList();
        List<KeyPoint> keyPointList2 = keyPoint2.toList();

        for (int i = 0; i < matchList.size(); i++) {
            pointList1.add(keyPointList1.get(matchList.get(i).queryIdx).pt);
            pointList2.add(keyPointList2.get(matchList.get(i).trainIdx).pt);
        }

        MatOfPoint2f points1 = new MatOfPoint2f();
        points1.fromList(pointList1);
        MatOfPoint2f points2 = new MatOfPoint2f();
        points2.fromList(pointList2);

//        Mat fundamentalMatrix = new Mat();
//        fundamentalMatrix = Calib3d.findFundamentalMat(points1, points2, Calib3d.RANSAC);

        Mat essentialMatrix = new Mat(3, 3, CvType.CV_64FC1);
        essentialMatrix = Calib3d.findEssentialMat(points1, points2, intrinsic, Calib3d.RANSAC, prob, threshold);

        Calib3d.recoverPose(essentialMatrix, points1, points2, intrinsic, R, T);

        Log.d(TAG, "Rotation Matrix:" + R.dump());
        Log.d(TAG, "Translation Vector:" + T.dump());

        return true;
    }

    @Override
    public boolean estimation(MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, Mat R, Mat T) {
        Log.d(TAG, "wrong way in 2D2D");
        return false;
    }
}
