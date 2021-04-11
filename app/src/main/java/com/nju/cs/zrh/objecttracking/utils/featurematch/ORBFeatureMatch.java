package com.nju.cs.zrh.objecttracking.utils.featurematch;

import com.nju.cs.zrh.objecttracking.utils.featurematch.FeatureMatch;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;

import java.util.LinkedList;
import java.util.List;

public class ORBFeatureMatch implements FeatureMatch {

    private final int nfeatures = 500;
    private final float scaleFactor = 1.2f;
    private final int nlevels = 8;
    private final int edgeThreshold = 31;
    private final int firstLevel = 0;
    private final int WTA_k = 2;
    private final int scoreType = ORB.HARRIS_SCORE;
    private final int patchSize = 31;
    private final int fastThreshold = 20;

    private ORB orbDetector = ORB.create(nfeatures, scaleFactor, nlevels, edgeThreshold, firstLevel, WTA_k, scoreType, patchSize, fastThreshold);
    private DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

    @Override
    public void featureDetectAndDraw(Mat img) {
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();
        orbDetector.detect(img, keyPoint);
        Features2d.drawKeypoints(img, keyPoint, img);
    }

    @Override
    public void findFeatureMatches(Mat img1, Mat img2, MatOfKeyPoint keyPoint1, MatOfKeyPoint keyPoint2, MatOfDMatch goodMatches) {

        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();

        orbDetector.detectAndCompute(img1, new Mat(), keyPoint1, descriptor1);
        orbDetector.detectAndCompute(img2, new Mat(), keyPoint2, descriptor2);

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

        LinkedList<DMatch> goodMachList = new LinkedList<>();
        for (int i = 0; i < matchList.size(); i++) {
            DMatch dMatch = matchList.get(i);
            if (dMatch.distance <= Math.max(2.0 * minDist, 30.0)) {
                goodMachList.addLast(dMatch);
            }
        }

        goodMatches.fromList(goodMachList);
    }
}
