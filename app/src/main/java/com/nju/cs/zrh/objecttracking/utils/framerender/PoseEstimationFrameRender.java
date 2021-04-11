package com.nju.cs.zrh.objecttracking.utils.framerender;

import com.nju.cs.zrh.objecttracking.PoseEstimationSolver;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class PoseEstimationFrameRender extends FrameRender {

    private PoseEstimationSolver mPoseEstimationSolver;

    public PoseEstimationFrameRender(PoseEstimationSolver mPoseEstimationSolver) {
        this.mPoseEstimationSolver = mPoseEstimationSolver;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();

        mPoseEstimationSolver.processFrame(rgbaFrame);
        return rgbaFrame;
    }
}
