package com.nju.cs.zrh.objecttracking.utils.featurematch;

import androidx.annotation.NonNull;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;

import java.util.LinkedList;
import java.util.List;

public class ORBFeatureMatch implements FeatureMatch {

    private static final int nfeatures = 500;
    private static final float scaleFactor = 1.2f;
    private static final int nlevels = 8;
    private static final int edgeThreshold = 31;
    private static final int firstLevel = 0;
    private static final int WTA_k = 2;
    private static final int scoreType = ORB.FAST_SCORE;
    private static final int patchSize = 31;
    private static final int fastThreshold = 20;

    private static final double lowerDistance = 20.0;

    private ORB orbDetector = ORB.create(nfeatures, scaleFactor, nlevels, edgeThreshold, firstLevel, WTA_k, scoreType, patchSize, fastThreshold);
    private DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

    @Override
    public void findKeyPoints(@NonNull Mat img, MatOfKeyPoint keyPoint) {
        orbDetector.detect(img, keyPoint);
    }

    @Override
    public void findKeyPointsAndDescriptors(@NonNull Mat img, MatOfKeyPoint keyPoint, Mat descriptors) {
        orbDetector.detectAndCompute(img, new Mat(), keyPoint, descriptors);
    }

    @Override
    public void findMatchesByDesc(@NonNull Mat descriptor1, @NonNull Mat descriptor2, MatOfDMatch goodMatch) {

        MatOfDMatch matches = new MatOfDMatch();

        matcher.match(descriptor1, descriptor2, matches);

        double minDist = Double.MAX_VALUE;
        double maxDist = Double.MIN_VALUE;
        List<DMatch> matchList = matches.toList();
        for (DMatch dMatch : matchList) {
            double dist = dMatch.distance;
            if (dist < minDist) minDist = dist;
            if (dist > maxDist) maxDist = dist;
        }

        // 筛选合适匹配
        LinkedList<DMatch> goodMachList = new LinkedList<>();
        for (int i = 0; i < matchList.size(); i++) {
            DMatch dMatch = matchList.get(i);
            if (dMatch.distance <= Math.max(minDist * 1.5, lowerDistance)) {
                goodMachList.addLast(dMatch);
            }
        }

        goodMatch.fromList(goodMachList);
    }

    @Override
    public boolean findMatchesByDesc(@NonNull Mat descriptor1, @NonNull Mat descriptor2, @NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, MatOfDMatch goodMatch) {
        return false;
    }

    @Override
    public void findMatches(@NonNull Mat img1, @NonNull Mat img2, MatOfDMatch goodMatch) {

        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();
        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();

        this.findKeyPointsAndDescriptors(img1, keyPoint1, descriptor1);
        this.findKeyPointsAndDescriptors(img2, keyPoint2, descriptor2);

        this.findMatchesByDesc(descriptor1, descriptor2, goodMatch);
    }


    @Override
    public void drawKeyPoints(@NonNull Mat img) {
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();
        this.findKeyPoints(img, keyPoint);
        Features2d.drawKeypoints(img, keyPoint, img);
    }

    @Override
    public void drawMatches(@NonNull Mat img1, @NonNull Mat img2, @NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull Mat descriptor1, @NonNull Mat descriptor2, Mat result) {

        MatOfDMatch match = new MatOfDMatch();
        this.findMatchesByDesc(descriptor1, descriptor2, match);

        // Draw Matches
        Features2d.drawMatches(img1, keyPoint1, img2, keyPoint2, match, result, Scalar.all(-1), Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
    }
}
