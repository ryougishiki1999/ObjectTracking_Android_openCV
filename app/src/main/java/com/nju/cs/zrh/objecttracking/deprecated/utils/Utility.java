package com.nju.cs.zrh.objecttracking.deprecated.utils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * @author: Zhou RuoHeng
 * @date: 2021/3/8
 */
public class Utility {

    public static Mat vecFloat2Mat(float[] data, int rows, int cols) {
        Mat res = new Mat(rows, cols, CvType.CV_32FC1);
        res.put(0, 0, data);
        return res;
    }

    public static float[] mat2VecFloat(Mat data, int rows, int cols) {
        float[] res = new float[rows * cols];
        data.get(0, 0, res);
        return res;
    }
}
