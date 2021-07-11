package com.nju.cs.zrh.objecttracking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.nju.cs.zrh.objecttracking.cameracalibration.CalibrationResult;
import com.nju.cs.zrh.objecttracking.utils.framerender.FeatureFrameRender;
import com.nju.cs.zrh.objecttracking.utils.framerender.FeatureMatchFrameRender;
import com.nju.cs.zrh.objecttracking.utils.framerender.MotionFrameRender;
import com.nju.cs.zrh.objecttracking.utils.framerender.OnCameraFrameRender;
import com.nju.cs.zrh.objecttracking.utils.framerender.PreviewFrameRender;
import com.nju.cs.zrh.objecttracking.utils.framerender.TrackingFrameRender;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

public class ObjectTrackingActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private final static String TAG = "ObjectTrackingActivity";
    private final static String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final static int M_REQUEST_CODE = 203;

    private final static String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private Mat mRgba;
    private Mat intrinsicMatrix;
    private Mat distortionCoefficients;

    private CameraBridgeViewBase mOpenCvCameraView;

    private OnCameraFrameRender mOnCameraFrameRender;
    private PoseEstimationSolver mPoseEstimationSolver;

    private Button mTrakingEndBtn;
    private DepthEstimationState mDepthEstimationState = DepthEstimationState.CLOSE;

    private enum DepthEstimationState {
        CLOSE(0, ""),
        SYN(1, ""),
        ESTABLISHED(2, ""),
        FIN(3, "");

        private int state;
        private String stateInfo;

        DepthEstimationState(int state, String stateInfo) {
            this.state = state;
            this.stateInfo = stateInfo;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public String getStateInfo() {
            return stateInfo;
        }

        public void setStateInfo(String stateInfo) {
            this.stateInfo = stateInfo;
        }
    }


    private Button mFeatureMatchBtn;
    private Mat MatchFrame = null;
    private boolean isMatchEnable = false;


    private Menu mMenu;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    if (mOpenCvCameraView != null) {
                        mOpenCvCameraView.setCameraPermissionGranted();
                        mOpenCvCameraView.setMaxFrameSize(800, 600);
                        mOpenCvCameraView.enableView();
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

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }

        intrinsicMatrix = new Mat(3, 3, CvType.CV_64FC1);
        distortionCoefficients = new Mat(5, 1, CvType.CV_64FC1);

        if (CalibrationResult.tryLoad(this, intrinsicMatrix, distortionCoefficients)) {
            Log.i(TAG, "successful in loading Intrinsic and distortionCoefficients");
            Log.d(TAG, "intrinsic: " + intrinsicMatrix.dump());
            Log.d(TAG, "distortion: " + distortionCoefficients.dump());
        } else {
            Log.d(TAG, "please calibrate the camera first");
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.object_tracking_camera);

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);

            mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {
                final Resources res = getResources();

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getPointerCount() <= 1) {
                        float x = event.getX();
                        float y = event.getY();
                        //float xRaw= event.getRawX();
                        //float yRaw = event.getRawY();
                        if (mPoseEstimationSolver.getTriangulationFrameListSize() == 0) {
                            (Toast.makeText(ObjectTrackingActivity.this, res.getString(R.string.first_sample), Toast.LENGTH_SHORT)).show();
                            mPoseEstimationSolver.addTriangulationFrame(mRgba.clone());
                            mPoseEstimationSolver.setTargetPixel(new Point(x, y));
                            mDepthEstimationState = DepthEstimationState.SYN;
                            return false;
                        } else if (mDepthEstimationState == DepthEstimationState.SYN && mPoseEstimationSolver.getTriangulationFrameListSize() == 1) {
                            (Toast.makeText(ObjectTrackingActivity.this, res.getString(R.string.second_sample), Toast.LENGTH_SHORT)).show();
                            mPoseEstimationSolver.addTriangulationFrame(mRgba.clone());
                            return false;
                        } else if (mPoseEstimationSolver.getTriangulationFrameListSize() >= 2) {
                            (Toast.makeText(ObjectTrackingActivity.this, res.getString(R.string.restart), Toast.LENGTH_SHORT)).show();
                            mPoseEstimationSolver.getTriangulationFrameList().clear();

                            mPoseEstimationSolver.addTriangulationFrame(mRgba.clone());
                            mPoseEstimationSolver.setTargetPixel(new Point(x, y));
                            mDepthEstimationState = DepthEstimationState.SYN;
                            return false;
                        }
                    }
                    return false;
                }
            });
        }

        mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());

        mPoseEstimationSolver = new PoseEstimationSolver(intrinsicMatrix, distortionCoefficients);


        mTrakingEndBtn = findViewById(R.id.tracking_end_btn);

        mTrakingEndBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Resources res = getResources();
                mPoseEstimationSolver.saveTrackingResult();
                mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
                mMenu.findItem(R.id.preview_mode).setChecked(true);
                mMenu.findItem(R.id.object_tracking_mode).setChecked(false);

                (Toast.makeText(ObjectTrackingActivity.this, "saved tracking result", Toast.LENGTH_SHORT)).show();
            }
        });

        mFeatureMatchBtn = findViewById(R.id.feature_match_btn);
        mFeatureMatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Resources res = getResources();
                if (!isMatchEnable) {
                    (Toast.makeText(ObjectTrackingActivity.this, res.getString(R.string.match_sample), Toast.LENGTH_SHORT)).show();
                    MatchFrame = mRgba.clone();
                    isMatchEnable = true;
                } else {
                    (Toast.makeText(ObjectTrackingActivity.this, res.getString(R.string.end_match), Toast.LENGTH_SHORT)).show();
                    isMatchEnable = false;
                    mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
                    mMenu.findItem(R.id.preview_mode).setChecked(true);
                    mMenu.findItem(R.id.feature_match).setChecked(false);
                    mMenu.findItem(R.id.feature_mode).setChecked(false);
                }
                return;
            }
        });
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
            mPoseEstimationSolver.setWidth(width);
            mPoseEstimationSolver.setHeight(height);

            mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba().clone();
        return mOnCameraFrameRender.render(inputFrame);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.objecttracking, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.preview_mode).setEnabled(true);
        menu.findItem(R.id.preview_mode).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final Resources res = getResources();
        switch (item.getItemId()) {
            case R.id.preview_mode:
                mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
                item.setChecked(true);
                return true;
            case R.id.feature_mode:
                item.setChecked(true);
                return true;
            case R.id.feature_extraction:
                mOnCameraFrameRender = new OnCameraFrameRender(new FeatureFrameRender(mPoseEstimationSolver));
                item.setChecked(true);
                return true;
            case R.id.feature_match:
                if (MatchFrame == null || !isMatchEnable) {
                    (Toast.makeText(this, res.getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
                    return true;
                }
                mPoseEstimationSolver.setCurMatchFrame(MatchFrame);
                item.setChecked(true);
                mOnCameraFrameRender = new OnCameraFrameRender(new FeatureMatchFrameRender(mPoseEstimationSolver));
                return true;
            case R.id.triangulation:
                if (mPoseEstimationSolver.getTriangulationFrameListSize() != 2 || mDepthEstimationState != DepthEstimationState.SYN) {
                    (Toast.makeText(this, res.getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
                    return true;
                }
                if (mPoseEstimationSolver.triangulateEntry()) {
                    (Toast.makeText(this, res.getString(R.string.fulfill_triangulate), Toast.LENGTH_SHORT)).show();
                    mDepthEstimationState = DepthEstimationState.ESTABLISHED;
                    mOnCameraFrameRender = new OnCameraFrameRender(new TrackingFrameRender(mPoseEstimationSolver));

                    (Toast.makeText(this, "start object tracking...", Toast.LENGTH_SHORT)).show();
                    mMenu.findItem(R.id.object_tracking_mode).setChecked(true);
                } else {
                    (Toast.makeText(this, res.getString(R.string.restart_triangulate), Toast.LENGTH_SHORT)).show();
                    mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
                }
                return true;
            case R.id.object_tracking_mode:
//                if (mDepthEstimationState != DepthEstimationState.ESTABLISHED) {
//                    (Toast.makeText(this, res.getString(R.string.wait_triangulate), Toast.LENGTH_SHORT)).show();
//                    return true;
//                }
//                mOnCameraFrameRender = new OnCameraFrameRender(new TrackingFrameRender(mPoseEstimationSolver));
                (Toast.makeText(this, "Entry Deprecated now...", Toast.LENGTH_SHORT)).show();
                item.setChecked(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
