package com.nju.cs.zrh.objecttracking.utils.featurematch;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;

public interface FeatureMatch {
    void findKeyPoints(@NonNull Mat img, MatOfKeyPoint keyPoint);

    void findKeyPointsAndDescriptors(@NonNull Mat img, MatOfKeyPoint keyPoint, Mat descriptors);

    void findMatchesByDesc(@NonNull Mat descriptor1, @NonNull Mat descriptor2, MatOfDMatch goodMatch);

    boolean findMatchesByDesc(@NonNull Mat descriptor1, @NonNull Mat descriptor2, @NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, MatOfDMatch goodMatch);

    void findMatches(@NonNull Mat img1, @NonNull Mat img2, MatOfDMatch goodMatch);

    void drawKeyPoints(@NonNull Mat img);

    void drawMatches(@NonNull Mat img1, @NonNull Mat img2, @NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull Mat descriptor1, @NonNull Mat descriptor2, @NonNull Mat result);

}
