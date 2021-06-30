package com.nju.cs.zrh.objecttracking.utils.transform;

import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.Arrays;

public class Transform {
    private static final String TAG = "Transform";

    // 归一化相机坐标系
    public static Point pixel2cam(Point pixel, double[] intrinsic) {

        Log.d(TAG, "intrinsic arr:" + Arrays.toString(intrinsic));
        return new Point(
                (pixel.x - intrinsic[2]) / intrinsic[0],
                (pixel.y - intrinsic[5]) / intrinsic[4]
        );
    }

    public static void convertRTToTransform(@NonNull Mat R, @NonNull Mat T, Mat transform) {
        Log.d(TAG, "R: " + R.dump());
        Log.d(TAG, "T: " + T.dump());
        Mat.zeros(4, 4, CvType.CV_64FC1).copyTo(transform);
        R.copyTo(transform.colRange(0, 3).rowRange(0, 3));
        T.copyTo(transform.colRange(3, 4).rowRange(0, 3));
        transform.put(3, 3, 1.0);
        Log.d(TAG, "transform: " + transform.dump());
    }

    public static void TransformConvertToRT(@NonNull Mat transform, @NonNull Mat R, Mat T) {
        Log.d(TAG, "transform: " + transform.dump());
        Mat.zeros(3, 3, CvType.CV_64FC1).copyTo(R);
        Mat.zeros(3, 1, CvType.CV_64FC1).copyTo(T);
        transform.colRange(0, 3).rowRange(0, 3).copyTo(R);
        transform.colRange(3, 4).rowRange(0, 3).copyTo(T);
        Log.d(TAG, "R: " + R.dump());
        Log.d(TAG, "T: " + T.dump());
    }
}
