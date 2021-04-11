package com.nju.cs.zrh.objecttracking.utils.transform;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class Transform {

    // 归一化相机坐标系
    public static Point pixel2cam(final Point p, final Mat intrinsic) {
        double[] k = new double[9];
        intrinsic.get(0, 0, k);
        return new Point(
                (p.x - k[2]) / k[0],
                (p.y - k[5]) / k[4]
        );
    }
}
