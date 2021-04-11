package com.nju.cs.zrh.objecttracking.utils.poseestimation;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;

public abstract class PoseEstimation {

    public abstract void estimation(MatOfKeyPoint keyPoint1, MatOfKeyPoint keyPoint2, MatOfDMatch matches, Mat R, Mat T);

    public abstract void estimation(MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, Mat R, Mat T);
}
