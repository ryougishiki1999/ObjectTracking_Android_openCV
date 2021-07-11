package com.nju.cs.zrh.objecttracking.utils.featurematch;

import androidx.annotation.NonNull;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.utils.Converters;
import org.opencv.xfeatures2d.SIFT;

import java.util.ArrayList;
import java.util.List;

public class SIFTMatch implements FeatureMatch {

    private static final int nfeatures = 0;
    private static final int nOctavesLayers = 3;
    private static final double contrastThreshold = 0.04;
    private static final double edgeThreshold = 10.0;
    private static final double sigma = 1.2;

    private static final SIFT siftDetector = SIFT.create();
    //private static final SIFT siftDetector = SIFT.create(nfeatures, nOctavesLayers, contrastThreshold, edgeThreshold, sigma);
    //private static final DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
    private static final BFMatcher matcher = BFMatcher.create();


    @Override
    public void findKeyPoints(@NonNull Mat img, MatOfKeyPoint keyPoint) {
        siftDetector.detect(img, keyPoint);
    }

    @Override
    public void findKeyPointsAndDescriptors(@NonNull Mat img, MatOfKeyPoint keyPoint, Mat descriptors) {
        siftDetector.detectAndCompute(img, new Mat(), keyPoint, descriptors);
    }

    @Override
    public void findMatchesByDesc(@NonNull Mat descriptor1, @NonNull Mat descriptor2, MatOfDMatch goodMatch) {
        MatOfDMatch matches = new MatOfDMatch();

        matcher.match(descriptor1, descriptor2, matches);

        List<DMatch> matchList = matches.toList();

        float minDist = matchList.get(0).distance;
        float maxDist = matchList.get(0).distance;

        for (int i = 1; i < matchList.size(); i++) {
            float dist = matchList.get(i).distance;

            if (dist < minDist) {
                minDist = dist;
            }

            if (dist > maxDist) {
                maxDist = dist;
            }
        }

        List<DMatch> goodMatchList = new ArrayList<>();
        for (int i = 0; i < matchList.size(); i++) {
            DMatch dMatch = matchList.get(i);
            if (dMatch.distance < 0.6 * maxDist) {
                goodMatchList.add(dMatch);
            }
        }

        goodMatch.fromList(goodMatchList);
    }

    /**
     * @param descriptor1
     * @param descriptor2
     * @param keyPoint1
     * @param keyPoint2
     * @param goodMatch
     * @return
     * @description： 会改变KeyPoint1和keyPoint2
     */
    @Override
    public boolean findMatchesByDesc(@NonNull Mat descriptor1, @NonNull Mat descriptor2, @NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, MatOfDMatch goodMatch) {
        MatOfDMatch matches = new MatOfDMatch();

        matcher.match(descriptor1, descriptor2, matches);

        List<DMatch> matchList = matches.toList();

        final int PtCount1 = (int) (matchList.size() * 0.5);
        final int PtCount2 = (int) (matchList.size() * 0.3);

        float minDist = matchList.get(0).distance;
        float maxDist = matchList.get(0).distance;

        for (int i = 1; i < matchList.size(); i++) {
            float dist = matchList.get(i).distance;

            if (dist < minDist) {
                minDist = dist;
            }

            if (dist > maxDist) {
                maxDist = dist;
            }
        }

        List<DMatch> goodMatchList = new ArrayList<>();
        for (int i = 0; i < matchList.size(); i++) {
            DMatch dMatch = matchList.get(i);
            if (dMatch.distance <= 0.5 * maxDist) {
                goodMatchList.add(dMatch);
            }
        }

        matches.fromList(goodMatchList);
        matchList = matches.toList();
        if (matchList.size() < PtCount1) {
            return false;
        }

        List<KeyPoint> keyPointList1 = keyPoint1.toList();
        List<KeyPoint> keyPointList2 = keyPoint2.toList();

        List<KeyPoint> ranKeyPointList1 = new ArrayList<>();
        List<KeyPoint> ranKeyPointList2 = new ArrayList<>();
        List<Point> ranPointList1 = new ArrayList<>();
        List<Point> ranPointList2 = new ArrayList<>();

        for (int i = 0; i < matchList.size(); i++) {
            ranKeyPointList1.add(keyPointList1.get(matchList.get(i).queryIdx));
            ranKeyPointList2.add(keyPointList2.get(matchList.get(i).trainIdx));
        }

        for (int i = 0; i < matchList.size(); i++) {
            ranPointList1.add(ranKeyPointList1.get(i).pt);
            ranPointList2.add(ranKeyPointList2.get(i).pt);
        }

        Mat RansacStatusMat = new Mat();
        MatOfPoint2f point1 = new MatOfPoint2f();
        point1.fromList(ranPointList1);
        MatOfPoint2f point2 = new MatOfPoint2f();
        point2.fromList(ranPointList2);

        Mat fundamentalMat = Calib3d.findFundamentalMat(point1, point2, Calib3d.FM_RANSAC, 1.0, 0.9995, RansacStatusMat);

        RansacStatusMat.convertTo(RansacStatusMat, CvType.CV_8SC1);
        List<Byte> RansacStatus = new ArrayList<>();
        Converters.Mat_to_vector_char(RansacStatusMat, RansacStatus);

        List<KeyPoint> RRKeyPointList1 = new ArrayList<>();
        List<KeyPoint> RRKeyPointList2 = new ArrayList<>();
        List<DMatch> RRMatches = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < matchList.size(); i++) {
            if (RansacStatus.get(i) != 0) {
                RRKeyPointList1.add(ranKeyPointList1.get(i));
                RRKeyPointList2.add(ranKeyPointList2.get(i));
                matchList.get(i).queryIdx = index;
                matchList.get(i).trainIdx = index;
                RRMatches.add(matchList.get(i));
                index = index + 1;
            }
        }

        if (RRMatches.size() < PtCount2) {
            return false;
        }

        keyPoint1.fromList(RRKeyPointList1);
        keyPoint2.fromList(RRKeyPointList2);
        goodMatch.fromList(RRMatches);

        return true;
    }

    @Override
    public void findMatches(@NonNull Mat img1, @NonNull Mat img2, MatOfDMatch goodMatch) {
        MatOfDMatch matches = new MatOfDMatch();

        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();
        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();

        this.findKeyPointsAndDescriptors(img1, keyPoint1, descriptor1);
        this.findKeyPointsAndDescriptors(img2, keyPoint2, descriptor2);

        this.findMatchesByDesc(descriptor1, descriptor2, matches);

        List<DMatch> matchList = matches.toList();
        List<KeyPoint> keyPointList1 = keyPoint1.toList();
        List<KeyPoint> keyPointList2 = keyPoint2.toList();

        List<KeyPoint> ranKeyPointList1 = new ArrayList<>();
        List<KeyPoint> ranKeyPointList2 = new ArrayList<>();
        List<Point> ranPointList1 = new ArrayList<>();
        List<Point> ranPointList2 = new ArrayList<>();

        for (int i = 0; i < matchList.size(); i++) {
            ranKeyPointList1.add(keyPointList1.get(matchList.get(i).queryIdx));
            ranKeyPointList2.add(keyPointList2.get(matchList.get(i).trainIdx));
        }

        for (int i = 0; i < matchList.size(); i++) {
            ranPointList1.add(ranKeyPointList1.get(i).pt);
            ranPointList2.add(ranKeyPointList2.get(i).pt);
        }

        Mat RansacStatusMat = new Mat();
        MatOfPoint2f point1 = new MatOfPoint2f();
        point1.fromList(ranPointList1);
        MatOfPoint2f point2 = new MatOfPoint2f();
        point2.fromList(ranPointList2);

        Mat fundamentalMat = Calib3d.findFundamentalMat(point1, point2, Calib3d.FM_RANSAC, 3.0, 0.99, RansacStatusMat);

        List<Byte> RansacStatus = new ArrayList<>();
        Converters.Mat_to_vector_char(RansacStatusMat, RansacStatus);

        List<KeyPoint> RRKeyPointList1 = new ArrayList<>();
        List<KeyPoint> RRKeyPointList2 = new ArrayList<>();
        List<DMatch> RRMatches = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < matchList.size(); i++) {
            if (RansacStatus.get(i) != 0) {
                RRKeyPointList1.add(ranKeyPointList1.get(i));
                RRKeyPointList2.add(ranKeyPointList2.get(i));
                matchList.get(i).queryIdx = index;
                matchList.get(i).trainIdx = index;
                RRMatches.add(matchList.get(i));
                index = index + 1;
            }
        }

        goodMatch.fromList(RRMatches);
    }

    @Override
    public void drawKeyPoints(@NonNull Mat img) {
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();

        this.findKeyPoints(img, keyPoint);
        Features2d.drawKeypoints(img, keyPoint, img);
    }

    @Override
    public void drawMatches(@NonNull Mat img1, @NonNull Mat img2, @NonNull MatOfKeyPoint keyPoint1, @NonNull MatOfKeyPoint keyPoint2, @NonNull Mat descriptor1, @NonNull Mat descriptor2, @NonNull Mat result) {

        MatOfDMatch match = new MatOfDMatch();
        //this.findMatchesByDesc(descriptor1, descriptor2, match);
        if (this.findMatchesByDesc(descriptor1, descriptor2, keyPoint1, keyPoint2, match)) {
            // Draw Matches
            Features2d.drawMatches(img1, keyPoint1, img2, keyPoint2, match, result, Scalar.all(-1), Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
        } else {

        }
    }
}
