package com.example.zhang.opencvdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static android.R.attr.path;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private final String TAG ="Mainactivity";
    private JavaCameraView javaCameraView;
    private boolean iftakephoto = false;
    private MatOfPoint ContourB;
    Mat mRgba,hierarchy;
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
        final Window window = this.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        window.setAttributes(params);
        window.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                setHideVirtualKey(window);
            }
        });
        setContentView(R.layout.activity_main);
        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        Button takephoto = (Button)findViewById((R.id.opencv_takephoto_button));
        takephoto.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                iftakephoto = true;
                Point[] points = ContourB.toArray();
                double[] ContourBArray = {points[0].x,points[0].y,points[1].x,points[1].y,points[2].x,points[2].y,points[3].x,points[3].y};
                try {
                    Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mRgba, bmp);
                    Bitmap tmp = rotateBitmap(bmp,90);
                    String filename = saveImageToGallery(getApplicationContext(),tmp);
                    Intent intent=new Intent(MainActivity.this,ImageActivity.class);
                    intent.putExtra("filename",filename);
                    intent.putExtra("contourBArray",ContourBArray);
                    intent.putExtra("width",mRgba.rows());
                    startActivity(intent);
                    MainActivity.this.finish();
                }catch(Exception ex){
                    System.out.println(ex.getMessage());
                }
            }
        });
    }
    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }
    @Override
    protected void onPause() {
        iftakephoto = false;
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
        iftakephoto = false;
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
        if (iftakephoto == true){
            List hullmop = new ArrayList<MatOfPoint>();
            hullmop.add(ContourB);
            Imgproc.drawContours(mRgba,hullmop,0, new Scalar(0,255,0),3);
            return mRgba;
        }
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
        List<MatOfPoint> res = findContour(mGrayB);
        if(res.size()!=0){
            ContourB = res.get(0);
            Rect rect = Imgproc.boundingRect(ContourB);
            Point x1 = new Point(rect.x, rect.y);
            Point x2 = new Point(rect.x + rect.width, rect.y );
            Point x3 = new Point(rect.x + rect.width, rect.y + rect.height);
            Point x4 = new Point(rect.x,rect.y + rect.height);
            Point[] points = ContourB.toArray();
            //Imgproc.circle(mRgba, new Point(x1.x, x1.y), 20, new Scalar(0, 0, 255), 5); //p3 is colored blue
            //Imgproc.line(mRgba,x1,x2,new Scalar(255,0,0),3);
            //Imgproc.line(mRgba,x2,x3,new Scalar(255,0,0),3);
            //Imgproc.line(mRgba,x3,x4,new Scalar(255,0,0),3);
            //Imgproc.line(mRgba,x4,x1,new Scalar(255,0,0),3);
            /*
            MatOfPoint2f  NewMtx = new MatOfPoint2f( ContourB.toArray());
            RotatedRect rect = Imgproc.minAreaRect(NewMtx);
            Point[] vertices = new Point[4];
            rect.points(vertices);
            MatOfPoint Rec = new MatOfPoint(vertices);
            List<MatOfPoint> boxContours = new ArrayList<>();
            boxContours.add(Rec);
            Imgproc.drawContours(mRgba, boxContours, 0, new Scalar(128, 128, 128), 3);*/
            //List<MatOfPoint> boxContours = new ArrayList<>();
            //boxContours.add(new MatOfPoint(vertices));
            //MatOfPoint ContourC = findContour(mGrayC);
            List hullmop = new ArrayList<MatOfPoint>();
            hullmop.add(ContourB);
            //hullmop.add(ContourC);
            Imgproc.drawContours(mRgba,hullmop,0, new Scalar(0,255,0),3);
            //Imgproc.drawContours(mRgba,hullmop,1, new Scalar(255,0,0),3);
        }

        mGray.release();
        mGrayC.release();
        mGrayB.release();
        return mRgba;
    }
    private List<MatOfPoint> findContour(Mat mGray){
        List<MatOfPoint> ans = new ArrayList<MatOfPoint>();
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
            //Log.d(TAG,Integer.toString(contour.toArray().length));
            //Log.d(TAG,contour.size().toString());
            if ((contourarea > maxArea) && (contour.toArray().length==4)) {
                maxArea = contourarea;
                maxAreaIdx = idx;
            }
        }
        hierarchy.release();
        if(maxAreaIdx!=0){
            ans.add(hullmop.get(maxAreaIdx));
        }
        return ans;
    }
    public String saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片

        File appDir = new File(Environment.getExternalStorageDirectory()+"/Recipio");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);

            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
        return file.toString();
    }
    public void setHideVirtualKey(Window window){
        //保持布局状态
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                //布局位于状态栏下方
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                //全屏
                View.SYSTEM_UI_FLAG_FULLSCREEN|
                //隐藏导航栏
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (Build.VERSION.SDK_INT>=19){
            uiOptions |= 0x00001000;
        }else{
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        window.getDecorView().setSystemUiVisibility(uiOptions);
    }

}
