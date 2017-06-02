package com.example.zhang.opencvdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private final String TAG ="Mainactivity";
    private JavaCameraView javaCameraView;
    Mat mRgba,mBinary,mIntermediateMat,hierarchy,mRgbaT;
    BaseLoaderCallback mloaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if(status==BaseLoaderCallback.SUCCESS){
                javaCameraView.enableView();
            }
            else{
                super.onManagerConnected(status);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,      WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
            mloaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0,this,mloaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mGrayB,mGrayC,mGray;
        mGray = new Mat();
        mGrayB = new Mat();
        mGrayC = new Mat();
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_BGR2GRAY);

        //运行Canny算子
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, (new Size(5,5)));
        //Imgproc.erode(mGray, mGrayC, element);//腐蚀
        //Imgproc.dilate(mGrayC, mGrayC, element);//膨胀
        Imgproc.Canny(mGray, mGrayC, 20, 100);
        Imgproc.dilate(mGrayC, mGrayC, element);//膨胀
        org.opencv.core.Size s = new Size(9,9);
        Imgproc.GaussianBlur(mGrayC, mGrayC, s, 2);
        Imgproc.threshold(mGrayC, mGrayC, 30, 255,Imgproc.THRESH_BINARY);
        Imgproc.threshold(mGray, mGrayB, 200, 255,Imgproc.THRESH_BINARY);
        MatOfPoint ContourB = findContour(mGrayB);
        MatOfPoint ContourC = findContour(mGrayC);

        List hullmop = new ArrayList<MatOfPoint>();
        hullmop.add(ContourB);
        hullmop.add(ContourC);
        Imgproc.drawContours(mRgba,hullmop,0, new Scalar(0,255,0),3);
        Imgproc.drawContours(mRgba,hullmop,1, new Scalar(255,0,0),3);
        //Log.d(TAG,Double.toString(maxArea));
        //Log.d(TAG,Integer.toString(maxAreaIdx));
        mGray.release();
        mGrayC.release();
        mGrayB.release();
        return mRgba;
    }
    private MatOfPoint findContour(Mat mGray){
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        hierarchy = new Mat();
        Imgproc.findContours(mGray,contours,hierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        // Find the convex hull
        List<MatOfInt> hull = new ArrayList<MatOfInt>();
        for(int i=0; i < contours.size(); i++){
            MatOfPoint contour = contours.get(i);
            double contourarea = Imgproc.contourArea(contour);
            hull.add(new MatOfInt());
            Imgproc.convexHull(contours.get(i), hull.get(i));
        }
        // Convert MatOfInt to MatOfPoint for drawing convex hull
        // Loop over all contours
        List<Point[]> hullpoints = new ArrayList<Point[]>();
        for(int i=0; hull.size()>0 && i < hull.size(); i++){
            Point[] points = new Point[hull.get(i).rows()];

            // Loop over all points that need to be hulled in current contour
            for(int j=0; j < hull.get(i).rows(); j++){
                int index = (int)hull.get(i).get(j, 0)[0];
                points[j] = new Point(contours.get(i).get(index, 0)[0], contours.get(i).get(index, 0)[1]);
            }
            hullpoints.add(points);
        }
        // Convert Point arrays into MatOfPoint
        List<MatOfPoint> hullmop = new ArrayList<MatOfPoint>();
        for(int i=0; hullpoints.size()>0 && i < hullpoints.size(); i++){
            MatOfPoint mop = new MatOfPoint();
            mop.fromArray(hullpoints.get(i));
            hullmop.add(mop);
        }
        for(int i=0; hullmop.size()>0 && i < hullmop.size(); i++){
            MatOfPoint2f  NewMtx = new MatOfPoint2f( hullmop.get(i).toArray());
            Imgproc.approxPolyDP(NewMtx,NewMtx,0.1*Imgproc.arcLength(NewMtx,true),true);
            NewMtx.convertTo(hullmop.get(i), CvType.CV_32S);
        }

        double maxArea =10000;
        int maxAreaIdx = 0;
        for (int idx = 0; hullmop.size()>0 && idx < hullmop.size(); idx++) {
            MatOfPoint contour = hullmop.get(idx);
            double contourarea = Imgproc.contourArea(contour);
            Log.d(TAG,Integer.toString(contour.toArray().length));
            Log.d(TAG,contour.size().toString());
            if ((contourarea > maxArea) && (contour.toArray().length==4)) {
                maxArea = contourarea;
                maxAreaIdx = idx;
            }
        }
        hierarchy.release();
        if(maxAreaIdx!=0){
            return hullmop.get(maxAreaIdx);
        }
        else {
            return new MatOfPoint();
        }
    }
}
