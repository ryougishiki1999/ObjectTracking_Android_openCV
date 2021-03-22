package com.nju.cs.zrh.objecttracking.deprecated.zzycalibration;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nju.cs.zrh.objecttracking.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author: Zhou RuoHeng
 * @date: 2021/3/8
 */
public class ZZYCalibrationActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final static String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final static String TAG = "ZZYCalibration";
    private final static int M_REQUEST_CODE = 203;

    private Mat Rgba;
    private CameraBridgeViewBase mCVCamera;
    private static int successFrameCount = 0;
    private final static int FRAME_NUM_THRESHOLD = 8;
    private Mat intrinsic = new Mat(3, 3, CvType.CV_32FC1);
    private final float[] intrinsicFloat = new float[9];

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "openCV Camera loads successfully");
                    if (mCVCamera != null) {
                        mCVCamera.setCameraPermissionGranted();
                        mCVCamera.enableView();
                    }
                    break;
                default:
                    Log.e(TAG, "openCV Camera loads unsuccessfully");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private Map<Long, Mat> timestampToFrame = new TreeMap<>();
    private ZZYCameraCalibration processor;

    private Button takePhotoBtn;
    private TextView successFrameCountTxt;
    private TextView intrinsicTxt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zzycalibration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS, M_REQUEST_CODE);
        }

        mCVCamera = findViewById(R.id.zzy_camera);
        mCVCamera.setCvCameraViewListener(this);

        takePhotoBtn = findViewById(R.id.zzy_activity_take_photo_btn);
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (successFrameCount < FRAME_NUM_THRESHOLD) {
                    boolean flag = ZZYCameraCalibration.getInstance().resolve(Rgba);
                    if (flag) {
                        successFrameCount++;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("success frames: \n");
                    sb.append(successFrameCount);
                    successFrameCountTxt.setText(sb.toString());
                } else {
                    String txt = "the num of frames is enough";
                    successFrameCountTxt.setTextColor(Color.RED);
                    successFrameCountTxt.setText(txt);
                }

                if (ZZYCameraCalibration.getInstance().isCalibrated()) {
                    intrinsic = ZZYCameraCalibration.getInstance().getIntrinsic();
                    intrinsic.get(0, 0, intrinsicFloat);

                    StringBuilder sb = new StringBuilder();
                    sb.append("intrinsic matrix: \n");
                    for (int i = 0; i < 3; i++) {
                        for (int j = i * 3; j < i * 3 + 3; j++) {
                            sb.append(intrinsicFloat[j]).append(", ");
                        }
                        sb.append("\n");
                    }

                    intrinsicTxt.setText(sb.toString());
                }
            }
        });

        successFrameCountTxt = findViewById(R.id.success_frame_count_txt);
        intrinsicTxt = findViewById(R.id.zzy_activity_intrinsic_txt);

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "openCV library not found");
        } else {
            Log.d(TAG, "openCV library found inside package.");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (mCVCamera != null) {
            mCVCamera.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Rgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        Rgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Rgba = inputFrame.rgba();
        Core.rotate(Rgba, Rgba, Core.ROTATE_90_CLOCKWISE);
        return Rgba;
    }
}
