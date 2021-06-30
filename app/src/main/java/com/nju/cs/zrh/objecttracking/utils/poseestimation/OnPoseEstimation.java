package com.nju.cs.zrh.objecttracking.utils.poseestimation;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;

public class OnPoseEstimation {

    private PoseEstimation mPoseEstimation;

    public OnPoseEstimation(PoseEstimation mPoseEstimation) {
        this.mPoseEstimation = mPoseEstimation;
    }

    public boolean estimation(@NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull MatOfDMatch matches, Mat R, Mat T) {
        return mPoseEstimation.estimation(keyPoint1, keyPoint2, matches, R, T);
    }

    

    public boolean estimation(MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, Mat R, Mat T) {
        return mPoseEstimation.estimation(objectPoints, imagePoints, R, T);
    }
}
