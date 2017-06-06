package com.example.zhang.opencvdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class FinalActivity extends AppCompatActivity {
    Button save,rotate,cancel;
    ImageView image;
    Bitmap dst;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final);
        Intent intent=getIntent();
        String filename = intent.getStringExtra("filename");
        int[] Points = intent.getIntArrayExtra("Points");
        Bitmap baseBitmap = getLoacalBitmap(filename); //从本地取图片(在cdcard中获取)
        save = (Button)findViewById(R.id.btn_finalsave);
        rotate = (Button)findViewById(R.id.btn_rotate);
        image = (ImageView)findViewById(R.id.final_image);
        cancel = (Button)findViewById(R.id.btn_cancel);
        Mat imgToProcess = new Mat();
        Utils.bitmapToMat(baseBitmap,imgToProcess);
        dst = transformation(imgToProcess,Points);
        image.setImageBitmap(dst);
        save.setOnClickListener(click);
        rotate.setOnClickListener(click);
        cancel.setOnClickListener(click);
    }
    private Bitmap transformation(Mat imgToProcess,int[] points){
        Point[] point = new Point[4];
        for(int i = 0;i<4;i++){
            point[i] = new Point(points[i*2],points[i*2+1]);
        }
        MatOfPoint ContourB = new MatOfPoint();
        ContourB.fromArray(point);

        Rect rect = Imgproc.boundingRect(ContourB);
        Point x1 = new Point(rect.x, rect.y);
        Point x2 = new Point(rect.x + rect.width, rect.y );
        Point x3 = new Point(rect.x + rect.width, rect.y + rect.height);
        Point x4 = new Point(rect.x,rect.y + rect.height);
        Mat src_mat=new Mat(4,1, CvType.CV_32FC2);
        Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);
        int index = 0;
        int value = 10000;
        for(int i = 0;i<4;i++){
            double x = point[i].x;
            double y = point[i].y;
            int ans = (int)(x+y);
            if (ans<value){
                index = i;
                value = ans;
            }
        }
        src_mat.put(0,0,point[index%4].x,point[index%4].y,point[(index+1)%4].x,point[(index+1)%4].y,point[(index+2)%4].x,point[(index+2)%4].y,point[(index+3)%4].x,point[(index+3)%4].y);
        dst_mat.put(0,0,x1.x,x1.y,x2.x,x2.y,x3.x,x3.y,x4.x,x4.y);
        Mat perspectiveTransform=Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Mat dst=imgToProcess.clone();
        Imgproc.warpPerspective(imgToProcess, dst, perspectiveTransform, dst.size());
        Mat subMat=dst.submat(rect);
        Bitmap bmp = Bitmap.createBitmap(subMat.cols(), subMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(subMat, bmp);
        return bmp;
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
    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);  ///把流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    private View.OnClickListener click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_finalsave:
                    if(ImageActivity.instance!=null){
                        ImageActivity.instance.finish();
                    }
                    Toast.makeText(FinalActivity.this,"save is clicked",Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btn_rotate:
                    dst = rotateBitmap(dst,90);
                    image.setImageBitmap(dst);
                    break;
                case R.id.btn_cancel:
                    FinalActivity.this.finish();
                default:
                    break;
            }
        }
    };
}
