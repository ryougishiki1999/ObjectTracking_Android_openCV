package com.nju.cs.zrh.objecttracking.utils.featurematch;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;

public interface FeatureMatch {

    void featureDetectAndDraw(Mat img);

    void findFeatureMatches(Mat img1, Mat img2, MatOfKeyPoint keyPoint1, MatOfKeyPoint keyPoint2, MatOfDMatch match);
}
