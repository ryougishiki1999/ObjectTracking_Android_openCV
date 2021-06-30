package com.nju.cs.zrh.objecttracking.utils.framerender;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class PreviewFrameRender extends FrameRender {
    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        //Imgproc.circle(rgba, new Point(300, 400), 16, new Scalar(255, 0, 0, 255), -1, 8);

        return rgba;
    }
}
