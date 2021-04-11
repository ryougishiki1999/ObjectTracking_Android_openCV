package com.nju.cs.zrh.objecttracking.utils.framerender;

import com.nju.cs.zrh.objecttracking.cameracalibration.CameraCalibrator;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class CalibrationFrameRender extends FrameRender {

    private CameraCalibrator mCalibrator;

    public CalibrationFrameRender(CameraCalibrator mCalibrator) {
        this.mCalibrator = mCalibrator;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();
        mCalibrator.processFrame(grayFrame, rgbaFrame);

        return rgbaFrame;
    }
}
