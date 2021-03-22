package com.nju.cs.zrh.objecttracking;

import android.os.Bundle;
import android.view.Menu;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraActivity;

public class TestActivity extends CameraActivity {
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.calibration, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.preview_mode).setEnabled(true);
//        if (mCalibrator != null && !mCalibrator.isCalibrated()) {
//            menu.findItem(R.id.preview_mode).setEnabled(false);
//        }
        menu.findItem(R.id.preview_mode).setEnabled(false);
//        menu.findItem(R.id.preview_mode).setVisible(true);
        return true;
    }
}
