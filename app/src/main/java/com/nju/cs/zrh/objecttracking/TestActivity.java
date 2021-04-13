package com.nju.cs.zrh.objecttracking;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraActivity;

import java.util.Arrays;

public class TestActivity extends AppCompatActivity {
    private final static String TAG = "TestActivity";
    private Menu mMenu;

    private TextView testTouchTxtView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        testTouchTxtView = findViewById(R.id.test_touch_txtView);

        testTouchTxtView.setHintTextColor(getResources().getColor(R.color.black));
        int[] location = new int[2];
        testTouchTxtView.getLocationOnScreen(location);
        Log.d(TAG, "location:" + Arrays.toString(location));
        testTouchTxtView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float xRaw = event.getRawX();
                float y = event.getY();
                float yRaw = event.getRawY();
                return false;
            }

        });
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
