package com.nju.cs.zrh.objecttracking.utils;

import org.opencv.core.Mat;

/**
 * @author: Zhou RuoHeng
 * @date: 2021/3/8
 */
public class FrameAndRotationMat {

    private Mat frame;
    private Mat rotationMat;

    public FrameAndRotationMat() {

    }

    public FrameAndRotationMat(Mat frame_, Mat rotationMat_) {
        frame = frame_;
        rotationMat = rotationMat_;
    }

    public Mat getFrame() {
        return frame;
    }

    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    public Mat getRotationMat() {
        return rotationMat;
    }

    public void setRotationMat(Mat rotationMat) {
        this.rotationMat = rotationMat;
    }
}