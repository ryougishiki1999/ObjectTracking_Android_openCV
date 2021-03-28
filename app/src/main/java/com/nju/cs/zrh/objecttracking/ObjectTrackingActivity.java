package com.nju.cs.zrh.objecttracking;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nju.cs.zrh.objecttracking.cameracalibration.CalibrationResult;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class ObjectTrackingActivity extends AppCompatActivity {
    private final static String TAG = "ObjectTracking::";

    private Mat intrinsicMatrix = new Mat(3, 3, CvType.CV_64FC1);
    private Mat distortionCoefficients = new Mat(5, 1, CvType.CV_64FC1);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CalibrationResult.tryLoad(this, intrinsicMatrix, distortionCoefficients)) {
            Log.i(TAG, "successful in loading Intrinsic and distortionCoefficients");
        } else {

        }
    }
}
