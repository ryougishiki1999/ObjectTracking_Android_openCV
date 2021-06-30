package com.nju.cs.zrh.objecttracking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestActivity extends AppCompatActivity {
    private final static String TAG = "TestActivity";
    private Menu mMenu;

    private final static String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static final String galleryPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator + "Camera" + File.separator + "objectTracking" + File.separator;

    //private TextView testTouchTxtView;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_EXTERNAL_STORAGE);
        }


        mImageView = findViewById(R.id.test_imgView);
        //mImageView.setImageResource(R.drawable.lena);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lena);
                Mat img = new Mat();
                Utils.bitmapToMat(bitmap, img);
                //Mat img = new Mat(600, 800, CvType.CV_8UC4, new Scalar(0, 0, 255,255));

                int thickness = -1;
                int lineType = Imgproc.LINE_8;

                //Imgproc.circle(img, new Point(200, 500), 16, new Scalar(0, 0, 0), -1, lineType);
                Imgproc.circle(img, new Point(200, 500), 16, new Scalar(255, 0, 0, 255), -1, lineType);
                Bitmap result = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img, result);
                mImageView.setImageBitmap(result);

                Mat m1 = new Mat(2, 2, CvType.CV_64FC1);
                double[] arr = {2.0, 3.0, 4.0, 1.0};
                m1.put(0, 0, arr);

                Log.d(TAG, "m1 " + m1.dump());

                Mat m2 = m1.inv();
                Log.d(TAG, "m2 " + m2.dump());

                Mat m3 = new Mat();
                Core.gemm(m1, m2, 1.0, new Mat(), 0.0, m3);
                Log.d(TAG, "m3" + m3.dump());

                saveBMP2Gallery(result, "result");

            }
        });
//        testTouchTxtView = findViewById(R.id.test_touch_txtView);
//
//        testTouchTxtView.setHintTextColor(getResources().getColor(R.color.black));
//        int[] location = new int[2];
//        testTouchTxtView.getLocationOnScreen(location);
//        Log.d(TAG, "location:" + Arrays.toString(location));
//        testTouchTxtView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
////                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lena);
////                Mat img = new Mat();
////                Utils.bitmapToMat(bitmap, img);
////                int thickness = -1;
////                int lineType = Imgproc.LINE_8;
////
////                //Imgproc.circle(img, new Point(200, 500), 16, new Scalar(0, 0, 0), -1, lineType);
////                Imgproc.circle(img, new Point(200, 500), 10, new Scalar(1, 1, 1), 5);
////                Utils.matToBitmap(img, bitmap);
////                mImageView.setImageBitmap(bitmap);
////                float x = event.getX();
////                float xRaw = event.getRawX();
////                float y = event.getY();
////                float yRaw = event.getRawY();
////                double[] rArray = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0};
////                Mat R = new Mat(3, 3, CvType.CV_64FC1);
////                R.put(0, 0, rArray);
////
////                double[] tArray = {10.0, 11.0, 12.0};
////                Mat T = new Mat(3, 1, CvType.CV_64FC1);
////                T.put(0, 0, tArray);
////
////                Mat transform = new Mat();
////                Transform.convertRTToTransform(R, T, transform);
////
////                Mat m1 = new Mat(2, 2, CvType.CV_64FC1);
////                double[] arr = {2.0, 3.0, 4.0, 1.0};
////                m1.put(0, 0, arr);
////
////
////                Mat m2 = new Mat(2, 1, CvType.CV_64FC1);
////                m2.put(0, 0, 2);
////                m2.put(1, 0, 3);
////
////
////                Log.i(TAG, "m1: " + m1.dump());
////                Log.i(TAG, "m2: " + m2.dump());
////
////                Mat m3 = new Mat();
////                Core.gemm(m1, m2, 1.0, new Mat(), 0.0, m3);
////
////                Log.i(TAG, "m3: " + m3.dump());
////
////                Mat m4 = new Mat(2, 1, CvType.CV_64FC1);
////                m4.put(0, 0, 1.0);
////                m4.put(1, 0, 2.0);
////
////                Mat m5 = new Mat(2, 1, CvType.CV_64FC1);
////                m5.put(0, 0, 1.0);
////                m5.put(1, 0, 3.0);
////
////                Log.i(TAG, "m4: " + m4.dump());
////                Log.i(TAG, "m5: " + m5.dump());
////
////                Mat m6 = new Mat(2, 1, CvType.CV_64FC1);
////                Core.add(m4, m5, m6);
////                Log.i(TAG, "m6: " + m6.dump());
////
////                Mat I1 = Mat.eye(2, 2, CvType.CV_64FC1);
////                Mat m7 = new Mat(2, 1, CvType.CV_64FC1);
////                //Core.gemm(m6, I1, 1.0, new Mat(), 0.0, m7);
////
////                Log.i(TAG, "m6: " + m7.dump());
//
//                return false;
//            }
//
//        });
    }

    private void saveBMP2Gallery(Bitmap bmp, String picName) {

        File file = null;
        FileOutputStream outputStream = null;

        try {
            file = new File(galleryPath);
            if (!file.exists()) {
                file.mkdir();
            }

        } catch (Exception e) {
            e.getStackTrace();
        }

        try {
            file = new File(galleryPath, picName + ".jpg");

            outputStream = new FileOutputStream(file);

            if (outputStream != null) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            }

        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found.");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
        }
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
