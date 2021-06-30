package com.nju.cs.zrh.objecttracking.utils.featurematch;

import androidx.annotation.NonNull;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;

public class SITFMatch implements FeatureMatch {
    @Override
    public void findKeyPoints(@NonNull Mat img, MatOfKeyPoint keyPoint) {

    }

    @Override
    public void findKeyPointsAndDescriptors(@NonNull Mat img, MatOfKeyPoint keyPoint, Mat descriptors) {

    }

    @Override
    public void findFeatureMatchesByDesc(@NonNull Mat descriptor1, @NonNull Mat descriptor2, MatOfDMatch goodMatch) {

    }

    @Override
    public void findFeatureMatches(@NonNull Mat img1, @NonNull Mat img2, MatOfDMatch goodMatch) {

    }

    @Override
    public void drawKeyPoints(@NonNull Mat img) {

    }

    @Override
    public void drawMatches(@NonNull Mat img1, @NonNull Mat img2, @NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull Mat descriptor1, @NonNull Mat descriptor2, @NonNull Mat result) {

    }
}
