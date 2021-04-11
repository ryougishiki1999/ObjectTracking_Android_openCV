package com.nju.cs.zrh.objecttracking.utils.poseestimation;

import com.nju.cs.zrh.objecttracking.utils.framerender.PoseEstimationFrameRender;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;

public class OnPoseEstimation {

    private PoseEstimation mPoseEstimation;

    public OnPoseEstimation(PoseEstimation mPoseEstimation) {
        this.mPoseEstimation = mPoseEstimation;
    }

    public void estimation(MatOfKeyPoint keyPoint1, MatOfKeyPoint keyPoint2, MatOfDMatch matches, Mat R, Mat T) {
        mPoseEstimation.estimation(keyPoint1, keyPoint2, matches, R, T);
    }

    public void estimation(MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, Mat R, Mat T) {
        mPoseEstimation.estimation(objectPoints, imagePoints, R, T);
    }
}
