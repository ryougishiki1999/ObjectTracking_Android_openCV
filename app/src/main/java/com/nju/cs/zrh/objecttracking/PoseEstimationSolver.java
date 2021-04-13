package com.nju.cs.zrh.objecttracking;

import android.util.Log;

import com.nju.cs.zrh.objecttracking.utils.featurematch.FeatureMatch;
import com.nju.cs.zrh.objecttracking.utils.featurematch.ORBFeatureMatch;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.OnPoseEstimation;
import com.nju.cs.zrh.objecttracking.utils.poseestimation.PoseEstimation2D2D;

import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.utils.Converters;

import java.security.Key;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class PoseEstimationSolver {

    private static final String TAG = "PoseEstimationSolver";

    private LinkedList<Mat> rgbaFrameList = new LinkedList<>();
    private FeatureMatch mFeatureMatch = new ORBFeatureMatch();
    private OnPoseEstimation mOnPoseEstimation;
    private Map<Mat, Point> touchMap = new HashMap<>();

    private Mat intrinsic = new Mat(3, 3, CvType.CV_64FC1);

    public PoseEstimationSolver(Mat intrinsic) {
        this.intrinsic = intrinsic;
        mOnPoseEstimation = new OnPoseEstimation(new PoseEstimation2D2D(intrinsic));
    }

    public void processFrame(Mat rgbaFrame) {

        mFeatureMatch.featureDetectAndDraw(rgbaFrame);
    }

    public void addRgbaFrame(Mat rgbaFrame, Point point) {
        rgbaFrameList.add(rgbaFrame);
        touchMap.put(rgbaFrame, point);

        if (rgbaFrameList.size() >= 2) {
            poseEstimation();
        }
    }

    private void poseEstimation() {
        ListIterator<Mat> iterator1 = rgbaFrameList.listIterator(rgbaFrameList.size() - 1);
        ListIterator<Mat> iterator2 = rgbaFrameList.listIterator(rgbaFrameList.size() - 1);
        Mat img1 = iterator1.previous();
        Mat img2 = iterator2.next();

        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();
        MatOfDMatch match = new MatOfDMatch();
        mFeatureMatch.findFeatureMatches(img1, img2, keyPoint1, keyPoint2, match);

        Mat R = new Mat(3, 3, CvType.CV_64FC1);
        Mat T = new Mat(3, 1, CvType.CV_64FC1);
        mOnPoseEstimation.estimation(keyPoint1, keyPoint2, match, R, T);

        //debug
        Log.d(TAG, "Rotation Matrix:" + R.dump());
        Log.d(TAG, "Translation Vector" + T.dump());

        Point p1 = touchMap.get(img1);
        Point p2 = touchMap.get(img2);
        List<KeyPoint> keyPointList1 = new ArrayList<>(keyPoint1.toList());
        keyPointList1.add(new KeyPoint((float) p1.x, (float) p1.y, 1.0f));
        List<KeyPoint> keyPointList2 = new ArrayList<>(keyPoint2.toList());
        keyPointList2.add(new KeyPoint((float) p2.x, (float) p2.y, 1.0f));

        keyPoint1.fromList(keyPointList1);
        keyPoint2.fromList(keyPointList2);

        List<Point3> world_coordinate_pts = new ArrayList<>();
        Triangulation.getInstance().triangulation(keyPoint1, keyPoint2, match, intrinsic, R, T, world_coordinate_pts);
    }
}
