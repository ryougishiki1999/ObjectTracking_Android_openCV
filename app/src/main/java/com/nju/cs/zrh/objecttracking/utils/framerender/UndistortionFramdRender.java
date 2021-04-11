package com.nju.cs.zrh.objecttracking.utils.framerender;

import com.nju.cs.zrh.objecttracking.cameracalibration.CameraCalibrator;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;

public class UndistortionFramdRender extends FrameRender {

    private CameraCalibrator mCalibrator;

    public UndistortionFramdRender(CameraCalibrator mCalibrator) {
        this.mCalibrator = mCalibrator;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat renderedFrame = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
        Calib3d.undistort(inputFrame.rgba(), renderedFrame,
                mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());

        return renderedFrame;
    }
}
