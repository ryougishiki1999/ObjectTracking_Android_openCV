package com.nju.cs.zrh.objecttracking;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nju.cs.zrh.objecttracking.cameracalibration.CalibrationResult;
import com.nju.cs.zrh.objecttracking.utils.framerender.OnCameraFrameRender;
import com.nju.cs.zrh.objecttracking.utils.framerender.PoseEstimationFrameRender;
import com.nju.cs.zrh.objecttracking.utils.framerender.PreviewFrameRender;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class ObjectTrackingActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private final static String TAG = "ObjectTrackingActivity";
    private final static String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final static int M_REQUEST_CODE = 203;

    private Mat mRgba;
    private Mat intrinsicMatrix = new Mat(3, 3, CvType.CV_64FC1);
    private Mat distortionCoefficients = new Mat(5, 1, CvType.CV_64FC1);

    private CameraBridgeViewBase mOpenCvCameraView;

    private OnCameraFrameRender mOnCameraFrameRender;
    private PoseEstimationSolver mPoseEstimationSolver;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    if (mOpenCvCameraView != null) {
                        mOpenCvCameraView.setCameraPermissionGranted();
                        mOpenCvCameraView.setMaxFrameSize(800,600);
                        mOpenCvCameraView.enableView();
                        mOpenCvCameraView.setOnTouchListener(ObjectTrackingActivity.this);
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private int mWidth;
    private int mHeight;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_object_tracking);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS, M_REQUEST_CODE);
        }

        if (CalibrationResult.tryLoad(this, intrinsicMatrix, distortionCoefficients)) {
            Log.i(TAG, "successful in loading Intrinsic and distortionCoefficients");
        } else {
            Log.d(TAG, "please calibrate the camera first");
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.object_tacking_camera);
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
        }

        mPoseEstimationSolver = new PoseEstimationSolver(intrinsicMatrix.clone());
        mOnCameraFrameRender = new OnCameraFrameRender(new PoseEstimationFrameRender(mPoseEstimationSolver));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found.");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView = null;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;

            mRgba = new Mat(mHeight, mWidth, CvType.CV_8UC4);
        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        return mOnCameraFrameRender.render(inputFrame);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mPoseEstimationSolver.addRgbaFrame(mRgba.clone());
        return false;
    }
}
