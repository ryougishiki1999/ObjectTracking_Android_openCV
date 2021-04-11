package com.nju.cs.zrh.objecttracking;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.nju.cs.zrh.objecttracking.cameracalibration.CameraCalibrationActivity;

import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private Button cameraCalibrationBtn;
    private Button zzyCalibrationBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraCalibrationBtn = findViewById(R.id.camera_calibration_btn);
        cameraCalibrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraCalibrationActivity.class);
                startActivity(intent);
            }
        });

        zzyCalibrationBtn = findViewById(R.id.zzy_calibration_btn);
        zzyCalibrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ObjectTrackingActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "openCV library not found");
        } else {
            Log.d(TAG, "openCV library found inside package.");
        }
    }
}