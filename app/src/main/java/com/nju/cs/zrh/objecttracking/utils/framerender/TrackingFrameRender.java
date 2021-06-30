package com.nju.cs.zrh.objecttracking.utils.framerender;

import com.nju.cs.zrh.objecttracking.PoseEstimationSolver;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class TrackingFrameRender extends FrameRender {

    private PoseEstimationSolver mPoseEstimationSolver;

    public TrackingFrameRender(PoseEstimationSolver mPoseEstimationSolver) {
        this.mPoseEstimationSolver = mPoseEstimationSolver;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();
        mPoseEstimationSolver.renderTrackingFrame(rgbaFrame);
        return rgbaFrame;
    }
}
