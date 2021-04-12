package com.nju.cs.zrh.objecttracking;

import android.util.Log;

import com.nju.cs.zrh.objecttracking.utils.featurematch.FeatureMatch;
import com.nju.cs.zrh.objecttracking.utils.featurematch.ORBFeatureMatch;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.OnPoseEstimation;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.PoseEstimation2D2D;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class PoseEstimationSolver {

    private static final String TAG = "PoseEstimationSolver";

    private LinkedList<Mat> rgbaFrameList = new LinkedList<>();
    private FeatureMatch mFeatureMatch = new ORBFeatureMatch();
    private OnPoseEstimation mOnPoseEstimation;

    private Mat intrinsic = new Mat(3, 3, CvType.CV_64FC1);

    public PoseEstimationSolver(Mat intrinsic) {
        this.intrinsic = intrinsic;
        mOnPoseEstimation = new OnPoseEstimation(new PoseEstimation2D2D(intrinsic));
    }

    public void processFrame(Mat rgbaFrame) {
        mFeatureMatch.featureDetectAndDraw(rgbaFrame);
    }

    public void addRgbaFrame(Mat rgbaFrame) {
        rgbaFrameList.add(rgbaFrame.clone());

        if (rgbaFrameList.size() >= 2) {
            poseEstimation();
        }
    }

    private void poseEstimation() {
        ListIterator<Mat> iterator = rgbaFrameList.listIterator(rgbaFrameList.size() - 1);
        Mat img1 = iterator.previous().clone();
        Mat img2 = iterator.next().clone();

        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();
        MatOfDMatch match = new MatOfDMatch();
        mFeatureMatch.findFeatureMatches(img1, img2, keyPoint1, keyPoint2, match);

        Mat R = new Mat(3, 3, CvType.CV_64FC1);
        Mat T = new Mat(3, 1, CvType.CV_64FC1);
        mOnPoseEstimation.estimation(keyPoint1, keyPoint2, match, R, T);

        Log.i(TAG, "Rotation Matrix:" + R.dump());
        Log.i(TAG, "Translation Vector" + T.dump());

        List<Point3> world_coordinate_pts = new ArrayList<>();
        Triangulation.getInstance().triangulation(keyPoint1, keyPoint2, match, intrinsic, R, T, world_coordinate_pts);
    }
}
