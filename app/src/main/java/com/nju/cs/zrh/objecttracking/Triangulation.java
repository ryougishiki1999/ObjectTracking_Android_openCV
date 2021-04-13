package com.nju.cs.zrh.objecttracking;

import android.util.Log;

import com.nju.cs.zrh.objecttracking.utils.transform.Transform;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Triangulation {

    private static final String TAG = "Triangulation";

    private Triangulation() {

    }

    private static class HolderClass {
        private final static Triangulation instance = new Triangulation();
    }

    public static Triangulation getInstance() {
        return HolderClass.instance;
    }

    public void triangulation(MatOfKeyPoint keyPoint1, MatOfKeyPoint keyPoint2, MatOfDMatch match, Mat intrinsic, Mat R, Mat T, List<Point3> points) {
        List<DMatch> dMatchList = match.toList();
        List<KeyPoint> keyPointList1 = keyPoint1.toList();
        List<KeyPoint> keyPointList2 = keyPoint2.toList();

        List<Point> pts1 = new ArrayList<>();
        List<Point> pts2 = new ArrayList<>();

        Mat T1 = new Mat(3, 4, CvType.CV_64FC1);
        Mat T2 = new Mat(3, 4, CvType.CV_64FC1);

        T1.put(0, 0, 1.0);
        T1.put(1, 1, 1.0);
        T1.put(2, 2, 1.0);

        double[] rArray = new double[9];
        double[] tArray = new double[3];
        R.get(0, 0, rArray);
        T.get(0, 0, tArray);

        T2.put(0, 0, rArray[0]);
        T2.put(0, 1, rArray[1]);
        T2.put(0, 2, rArray[2]);
        T2.put(0, 3, tArray[0]);

        T2.put(1, 0, rArray[3]);
        T2.put(1, 1, rArray[4]);
        T2.put(1, 2, rArray[5]);
        T2.put(1, 3, tArray[1]);

        T2.put(1, 0, rArray[6]);
        T2.put(1, 1, rArray[7]);
        T2.put(1, 2, rArray[8]);
        T2.put(1, 3, tArray[2]);

        Log.d(TAG, "Rotation matrix:" + R.dump());
        Log.d(TAG, "Translation matrix:" + T.dump());
        Log.d(TAG, "T1 posture:" + T1.dump());
        Log.d(TAG, "T2 posture:" + T2.dump());

        for (DMatch dMatch : dMatchList) {
            pts1.add(Transform.pixel2cam(keyPointList1.get(dMatch.queryIdx).pt, intrinsic));
            pts2.add(Transform.pixel2cam(keyPointList2.get(dMatch.trainIdx).pt, intrinsic));
        }

        // 加入观测点
        Point p1 = keyPointList1.get(keyPointList1.size() - 1).pt;
        Point p2 = keyPointList2.get(keyPointList2.size() - 1).pt;
        pts1.add(Transform.pixel2cam(p1, intrinsic));
        pts2.add(Transform.pixel2cam(p2, intrinsic));

        if (pts1.size() == pts2.size()) {

            final int N = pts1.size();
            Mat projectionPoints1 = new Mat(2, pts1.size(), CvType.CV_64FC1);
            Mat projectionPoints2 = new Mat(2, pts2.size(), CvType.CV_64FC1);

            for (int i = 0; i < N; i++) {
                double x = pts1.get(i).x;
                double y = pts1.get(i).y;
                projectionPoints1.put(0, i, x);
                projectionPoints1.put(1, i, y);
                // debug
                Log.d(TAG, "projectionPoints1 ith col:" + projectionPoints1.col(i).dump());
            }


            for (int i = 0; i < N; i++) {
                double x = pts2.get(i).x;
                double y = pts2.get(i).y;
                projectionPoints2.put(0, i, x);
                projectionPoints2.put(1, i, y);
                // debug
                Log.d(TAG, "projectionPoints2 ith col:" + projectionPoints2.col(i).dump());
            }

            Mat pts4D = new Mat(4, N, CvType.CV_64FC1);

            Calib3d.triangulatePoints(T1, T2, projectionPoints1, projectionPoints2, pts4D);

            for (int i = 0; i < pts4D.cols(); i++) {
                Mat colVec = pts4D.col(i);

                List<Double> colVecList = new ArrayList<>();
                Converters.Mat_to_vector_double(colVec, colVecList);
                double normalize = colVecList.get(3);

                double x = colVecList.get(0) / normalize;
                double y = colVecList.get(1) / normalize;
                double z = colVecList.get(2) / normalize;

                Point3 p = new Point3(x, y, z);

                points.add(p);
            }

        } else {
            Log.e("TAG", "feature points match obtaining error");
        }
    }
}
