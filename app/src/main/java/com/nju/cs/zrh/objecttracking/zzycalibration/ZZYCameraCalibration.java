package com.nju.cs.zrh.objecttracking.zzycalibration;

import android.media.Image;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Zhou RuoHeng
 * @date: 2021/3/8
 */
public class ZZYCameraCalibration {

    private final List<Mat> frameList = new ArrayList<>();

    private final static int BOARD_WIDTH = 9;
    private final static int BOARD_HEIGHT = 6;
    private final static int SQUARE_SIZE = 20;
    private final static int NUM_HOR_CORNERS = 9;
    private final static int NUM_VERT_CORNERS = 6;
    private final static float SCALE = 0.25f;
    private Size boardSize;
    private Size squareSize = new Size(SQUARE_SIZE, SQUARE_SIZE);

    private Mat savedImage = new Mat(); // the saved chessboard image
    private Image undistoredImage = null, CamStream;  // the calibrated camera frame

    private List<Mat> imagePoints = new ArrayList<>();
    private List<Mat> objectPoints = new ArrayList<>();
    private MatOfPoint2f imageCorners = new MatOfPoint2f();
    private MatOfPoint3f obj = new MatOfPoint3f();
    private Mat intrinsic = new Mat(3, 3, CvType.CV_32FC1);
    private int boardsNumber;
    private int numCornersHor;
    private int numCornersVer;
    private int successes = 0;
    private Mat distCoeffs = new Mat();
    private boolean isCalibrated = false;

    private ZZYCameraCalibration() {

    }

    private static class HolderClass {
        private final static ZZYCameraCalibration instance = new ZZYCameraCalibration();
    }

    public static ZZYCameraCalibration getInstance() {
        return HolderClass.instance;
    }


    public void resolve(Map<Long, Mat> timestampToFrame, float[] intrinsicMatrix) {
        Set<Long> timestampSet = timestampToFrame.keySet();
        Iterator<Long> iterator = timestampSet.iterator();
        while (iterator.hasNext()) {
            long timestamp = iterator.next();
            Mat frame = timestampToFrame.get(timestamp);
            frameList.add(frame);
        }

        calibrationProcess();
    }

    private void calibrationProcess() {
        this.boardsNumber = frameList.size();
        this.numCornersHor = 9;
        this.numCornersVer = 6;
        int numSquares = this.numCornersHor * this.numCornersVer;
        for (int i = 0; i < numSquares; i++) {
            obj.push_back(new MatOfPoint3f(new Point3(i / this.numCornersHor, i % this.numCornersVer, 0.0f)));
        }

        for (int i = 0; i < this.boardsNumber; i++) {
            if (!frameList.get(i).empty()) {
                this.findChessCorners(frameList.get(i));
            }
        }

        float[] intrinsicMatrix = new float[intrinsic.rows() * intrinsic.cols()];
        if (this.isCalibrated) {

            intrinsic.convertTo(intrinsic, CvType.CV_32FC1);
            System.out.println("intrinsic Size: " + intrinsic.size());
            intrinsic.get(0, 0, intrinsicMatrix);
        }

    }

    private void findChessCorners(Mat frame) {
        Mat grayImage = new Mat();

        if (this.successes < this.boardsNumber) {

            Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_RGBA2GRAY);

            Size boardSize = new Size(this.numCornersHor, this.numCornersVer);

            boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);

            if (found) {
                TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
                Imgproc.cornerSubPix(grayImage, imageCorners, new Size(11, 11), new Size(-1, -1), term);
                this.imagePoints.add(imageCorners);
                imageCorners = new MatOfPoint2f();
                this.objectPoints.add(obj);
                grayImage.copyTo(this.savedImage);
            }
            this.successes++;
        }

        if (this.successes == this.boardsNumber) {
            this.myCalibrateCamera();
        }
    }

    private void myCalibrateCamera() {
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();
        intrinsic.put(0, 0, 1.0f);
        intrinsic.put(1, 1, 1.0f);
        // calibrate!
        Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs);
        this.isCalibrated = true;
    }


}
