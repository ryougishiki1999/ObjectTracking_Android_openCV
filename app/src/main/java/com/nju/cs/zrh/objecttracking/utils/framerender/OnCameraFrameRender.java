package com.nju.cs.zrh.objecttracking.utils.framerender;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class OnCameraFrameRender {
    private FrameRender mFrameRender;

    public OnCameraFrameRender(FrameRender mFrameRender) {
        this.mFrameRender = mFrameRender;
    }

    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return mFrameRender.render(inputFrame);
    }
}
