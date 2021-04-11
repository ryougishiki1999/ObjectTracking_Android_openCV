package com.nju.cs.zrh.objecttracking.utils.poseestimation;

import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;

public class PoseEstimation3D2D extends PoseEstimation {

    private final static String TAG = "PoseEstimation3D2D::";

    private Mat intrinsic = new Mat(3, 3, CvType.CV_64FC1);
    private MatOfDouble distCoeffs;

    public PoseEstimation3D2D(Mat intrinsic, Mat distcoeffs) {
        this.intrinsic = intrinsic;
        this.distCoeffs = new MatOfDouble(distcoeffs);
    }

    @Override
    public void estimation(MatOfKeyPoint keyPoint1, MatOfKeyPoint keyPoint2, MatOfDMatch matches, Mat R, Mat T) {
        Log.i(TAG, "wrong way in 3D3D");
    }


    @Override
    public void estimation(MatOfPoint3f objectPoints, MatOfPoint2f imagePoints, Mat R, Mat T) {
        Mat r = new Mat(3, 1, CvType.CV_64FC1);
        Mat t = new Mat(3, 1, CvType.CV_64FC1);

        Calib3d.solvePnP(objectPoints, imagePoints, intrinsic, distCoeffs, r, t, false, Calib3d.SOLVEPNP_EPNP);
        Calib3d.Rodrigues(r, R);
        T = t.clone();
    }
}
