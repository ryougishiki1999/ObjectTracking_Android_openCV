package com.nju.cs.zrh.objecttracking.utils;

import org.opencv.core.Point;

import java.util.List;

public class Numerical {

    public static double getMeans(List<Double> list) {
        double sum = 0.0;
        int num = list.size();
        for (int i = 0; i < num; i++) {
            sum += list.get(i);
        }
        return sum / num;
    }

    public static double getVariance(List<Double> list) {
        double sum = 0.0;
        int num = list.size();
        double mean = getMeans(list);

        for (int i = 0; i < num; i++) {
            sum += Math.pow(list.get(i) - mean, 2);
        }

        return sum / num;
    }

    public static double get2DEuclideanDistance(Point p1, Point p2) {
        double res = 0.0;
        res = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
        return res;
    }

    public static double get2DEuclideanDistancePow(Point p1, Point p2) {
        double res = 0.0;
        res = (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y);
        return res;
    }
}
