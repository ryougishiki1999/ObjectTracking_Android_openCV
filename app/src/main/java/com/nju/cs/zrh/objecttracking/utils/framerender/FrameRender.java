package com.nju.cs.zrh.objecttracking.utils.framerender;

import com.nju.cs.zrh.objecttracking.cameracalibration.CameraCalibrator;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public abstract class FrameRender {
    public abstract Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame);
}
