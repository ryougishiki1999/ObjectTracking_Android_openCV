package com.nju.cs.zrh.objecttracking.utils.framerender;

import com.nju.cs.zrh.objecttracking.PoseEstimationSolver;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class FeatureMatchFrameRender extends FrameRender {

    private PoseEstimationSolver mPoseEstimationSolver;
    private Mat result;

    public FeatureMatchFrameRender(PoseEstimationSolver mPoseEstimationSolver) {
        this.mPoseEstimationSolver = mPoseEstimationSolver;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaFrame = inputFrame.rgba();

        result = new Mat(rgbaFrame.size(), rgbaFrame.type());
        mPoseEstimationSolver.renderFeatureMatchFrame(rgbaFrame.clone(), result);

        return result;
    }
}
