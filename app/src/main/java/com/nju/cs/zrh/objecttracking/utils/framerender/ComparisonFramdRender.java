package com.nju.cs.zrh.objecttracking.utils.framerender;

import android.content.res.Resources;

import com.nju.cs.zrh.objecttracking.R;
import com.nju.cs.zrh.objecttracking.cameracalibration.CameraCalibrator;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ComparisonFramdRender extends FrameRender {

    private CameraCalibrator mCalibrator;
    private int mWidth;
    private int mHeight;
    private Resources mResources;

    public ComparisonFramdRender(CameraCalibrator mCalibrator, int mWidth, int mHeight, Resources mResources) {
        this.mCalibrator = mCalibrator;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mResources = mResources;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat undistortedFrame = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
        Calib3d.undistort(inputFrame.rgba(), undistortedFrame,
                mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());

        Mat comparisonFrame = inputFrame.rgba();
        undistortedFrame.colRange(new Range(0, mWidth / 2)).copyTo(comparisonFrame.colRange(new Range(mWidth / 2, mWidth)));
        List<MatOfPoint> border = new ArrayList<MatOfPoint>();
        final int shift = (int) (mWidth * 0.005);
        border.add(new MatOfPoint(new Point(mWidth / 2 - shift, 0), new Point(mWidth / 2 + shift, 0),
                new Point(mWidth / 2 + shift, mHeight), new Point(mWidth / 2 - shift, mHeight)));
        Imgproc.fillPoly(comparisonFrame, border, new Scalar(255, 255, 255));

        Imgproc.putText(comparisonFrame, mResources.getString(R.string.original), new Point(mWidth * 0.1, mHeight * 0.1),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 255, 0));
        Imgproc.putText(comparisonFrame, mResources.getString(R.string.undistorted), new Point(mWidth * 0.6, mHeight * 0.1),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 255, 0));

        return comparisonFrame;
    }
}
