package com.nju.cs.zrh.objecttracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nju.cs.zrh.objecttracking.utils.FrameAndRotationMat;
import com.nju.cs.zrh.objecttracking.utils.MyDateUtils;
import com.nju.cs.zrh.objecttracking.utils.MyFeaturePointUtils;
import com.nju.cs.zrh.objecttracking.utils.Utility;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CameraCalibrationActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private final static String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final static String TAG = "CameraCalibration";
    private final static int M_REQUEST_CODE = 203;

    private SensorManager mSensorManager;
    private static final float MS2S = 1.0f / 1000.0f;
    private final float[] deltaRotationVector = new float[4];
    private final float[] deltaRotationMatrix = new float[9];
    private float[] currentRotationMatrix = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    private long startTimestamp;
    private long sensorTimestamp;
    private static final long INTERVAL = 5000;


    private Mat Rgba;
    private CameraBridgeViewBase mCVCamera;
    private int cameraIdx;
    private final static int BACK_CAMERA_IDX = 0;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "openCV Camera loads successfully");
                    if (mCVCamera != null) {
                        cameraIdx = BACK_CAMERA_IDX;
                        mCVCamera.setCameraIndex(cameraIdx);
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

    private Button mQuitBtn;
    private Button mStartBtn;
    private TextView mRotationMatrixTxt;
    volatile private boolean startFlag = false;

    private Map<Long, Mat> timestampToFrame = new HashMap<>();
    private Map<Long, Mat> timestampToRotationMat = new HashMap<>();


    private Map<Long, FrameAndRotationMat> timestampToFaR = new TreeMap<>();

    private TextView testTxt;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_calibration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS, M_REQUEST_CODE);
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mCVCamera = findViewById(R.id.calibration_camera);
        mCVCamera.setCvCameraViewListener(this);


        mQuitBtn = findViewById(R.id.calibration_camera_quit_btn);
        mQuitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraCalibrationActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        mRotationMatrixTxt = findViewById(R.id.rotation_matrix_txt);
        mStartBtn = findViewById(R.id.calibration_camera_start_btn);
        mStartBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startFlag = true;
                startTimestamp = MyDateUtils.getCurTimestamp();
            }
        });

        testTxt = findViewById(R.id.calibration_camera_test_txt);


    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);

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
        long timestamp = MyDateUtils.getCurTimestamp();
        Rgba = inputFrame.rgba();

        if (cameraIdx == BACK_CAMERA_IDX) {
            Core.rotate(Rgba, Rgba, Core.ROTATE_90_CLOCKWISE);
        }

        Mat mRgba = Rgba.clone();


        if (startFlag) {
            if (MyDateUtils.getTimeDistance(startTimestamp, timestamp) <= INTERVAL) {
                timestampToFrame.put(timestamp, mRgba);
            } else {
                synchronized (CameraCalibrationActivity.class) {
                    if (startFlag) {
                        cameraCalibrationEntry();
                    }
                    startFlag = false;
                }
            }
        }

        return Rgba;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float[] values = event.values;
        int type = event.sensor.getType();
        StringBuilder sb;

        switch (type) {
            case Sensor.TYPE_GYROSCOPE:
                if (sensorTimestamp != 0) {
                    final float dT = (MyDateUtils.getCurTimestamp() - sensorTimestamp) * MS2S;

                    float axisX = values[0];
                    float axisY = values[1];
                    float axisZ = values[2];

                    float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                    // Normalize the rotation vector if it's big enough to get the axis
                    if (omegaMagnitude > Float.MIN_NORMAL) {
                        axisX /= omegaMagnitude;
                        axisY /= omegaMagnitude;
                        axisZ /= omegaMagnitude;
                    }

                    // Integrate around this axis with the angular speed by the time step
                    // in order to get a delta rotation from this sample over the time step
                    // We will convert this axis-angle representation of the delta rotation
                    // into a quaternion before turning it into the rotation matrix.
                    float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                    float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                    float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

                    deltaRotationVector[0] = sinThetaOverTwo * axisX;
                    deltaRotationVector[1] = sinThetaOverTwo * axisY;
                    deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                    deltaRotationVector[3] = cosThetaOverTwo;

                }

                sensorTimestamp = MyDateUtils.getCurTimestamp();
                SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
                // User code should concatenate the delta rotation we computed with the current
                // rotation in order to get the updated rotation.
                // rotationCurrent = rotationCurrent * deltaRotationMatrix;
                sb = new StringBuilder();
                sb.append("Delta Rotation Matrix:\n");
                for (int i = 0; i < 3; i++) {
                    for (int j = i * 3; j < i * 3 + 3; j++) {
                        @SuppressLint("DefaultLocale") String element = String.format("%.4f", deltaRotationMatrix[j]);
                        sb.append(element).append(", ");
                    }
                    sb.append("\n");
                }
                sb.append("time stamp: ").append(sensorTimestamp);
                mRotationMatrixTxt.setText(sb.toString());

                Mat deltaRotationMat = Utility.vecFloat2Mat(deltaRotationMatrix, 3, 3);
                Mat currentRotationMat = Utility.vecFloat2Mat(currentRotationMatrix, 3, 3);

                Core.multiply(currentRotationMat, deltaRotationMat, currentRotationMat);

                currentRotationMatrix = Utility.mat2VecFloat(currentRotationMat, 3, 3);

                if (startFlag) {
                    long timeDistance = MyDateUtils.getTimeDistance(startTimestamp, sensorTimestamp);
                    String stringBuilder = "time diff: \n" +
                            timeDistance +
                            " ms\n";
                    testTxt.setText(stringBuilder);

                    if (timeDistance <= INTERVAL) {
                        timestampToRotationMat.put(sensorTimestamp, currentRotationMat);
                    } else {
                        synchronized (CameraCalibrationActivity.class) {
                            if (startFlag) {
                                cameraCalibrationEntry();
                            }
                            startFlag = false;
                        }
                    }
                }

                break;
            default:
                Log.e(TAG, "type of sensor is not correct");
                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void cameraCalibrationEntry() {

        Set<Long> keySet1 = timestampToFrame.keySet();
        Set<Long> keySet2 = timestampToRotationMat.keySet();
        Set<Long> keySetRes = new HashSet<>(keySet1);
        keySetRes.retainAll(keySet2);

        Iterator<Long> iterator = keySetRes.iterator();

        while (iterator.hasNext()) {
            Long timestamp = iterator.next();

            Mat frame = timestampToFrame.get(timestamp);
            Mat rotationMat = timestampToRotationMat.get(timestamp);

            FrameAndRotationMat frameAndRotationMat = new FrameAndRotationMat(frame, rotationMat);

            timestampToFaR.put(timestamp, frameAndRotationMat);
        }

        MyFeaturePointUtils.resolve(timestampToFaR);
    }

    public Map<Long, Mat> getTimestampToFrame() {
        return timestampToFrame;
    }

    public Map<Long, Mat> getTimestampToRotationMat() {
        return timestampToRotationMat;
    }

    public Map<Long, FrameAndRotationMat> getTimestampToFaR() {
        return timestampToFaR;
    }
}

