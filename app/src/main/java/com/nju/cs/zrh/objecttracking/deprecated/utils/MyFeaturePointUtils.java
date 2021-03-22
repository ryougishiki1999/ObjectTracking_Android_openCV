package com.nju.cs.zrh.objecttracking.deprecated.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;

/**
 * @author: Zhou RuoHeng
 * @date: 2021/3/8
 */
public class MyFeaturePointUtils {

    private static ORB orbDetector = null;

    public static void resolve(Map<Long, FrameAndRotationMat> map) {

        Set<Long> KeySet = map.keySet();
        TreeSet<Long> timestampKeySet = new TreeSet<>(KeySet);
        Iterator<Long> iterator1 = timestampKeySet.iterator();
        Iterator<Long> iterator2 = timestampKeySet.iterator();
        iterator2.next();

        while (iterator2.hasNext()) {
            long timestamp1 = iterator1.next();
            long timestamp2 = iterator2.next();

            FrameAndRotationMat farm1 = map.get(timestamp1);
            FrameAndRotationMat farm2 = map.get(timestamp2);

            Mat src = farm1.getFrame();
            Mat dst = farm2.getFrame();

            orbFeatureProcess(src, dst);

        }
    }

    private static Mat orbFeatureProcess(Mat src, Mat dst) {
        Mat res = new Mat();

        int nfeatures = 500;
        float scaleFactor = 1.2f;
        int nlevels = 8;
        int edgeThreshold = 31;
        int firstLevel = 0;
        int WTA_k = 2;
        int scoreType = ORB.HARRIS_SCORE;
        int patchSize = 31;
        int fastThreshold = 20;

        orbDetector = ORB.create(nfeatures, scaleFactor, nlevels, edgeThreshold, firstLevel, WTA_k, scoreType, patchSize, fastThreshold);
        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint(), keyPoint2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat(), descriptors2 = new Mat();

        orbDetector.detectAndCompute(src, new Mat(), keyPoint1, descriptors1);
        orbDetector.detectAndCompute(dst, new Mat(), keyPoint2, descriptors2);

        MatOfDMatch matches = new MatOfDMatch();
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);
        matcher.match(descriptors1, descriptors2, matches);

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
            if (dMatch.distance <= (2.5 * minDist)) {
                goodMachList.addLast(dMatch);
            }
        }

        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(goodMachList);

        DMatch[] goodMatchesArray = goodMatches.toArray();
        KeyPoint[] keyPoints1Array = keyPoint1.toArray();
        KeyPoint[] keyPoints2Array = keyPoint2.toArray();

        for (int i = 0; i < goodMatchesArray.length; i++) {
            DMatch dMatch = goodMatchesArray[i];
            int queryIdx = dMatch.queryIdx;
            int trainIdx = dMatch.trainIdx;
            Point points1 = keyPoints1Array[queryIdx].pt;

        }

        return res;
    }
}