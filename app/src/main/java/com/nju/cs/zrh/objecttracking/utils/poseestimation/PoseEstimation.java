package com.nju.cs.zrh.objecttracking.utils.poseestimation;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;

public abstract class PoseEstimation {

    public abstract boolean estimation(@NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull MatOfDMatch matches, Mat R, Mat T);

    public abstract boolean estimation(MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, Mat R, Mat T);
}
