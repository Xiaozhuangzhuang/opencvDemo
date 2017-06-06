package com.example.zhang.opencvdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ImageActivity extends AppCompatActivity {
    public static ImageActivity instance = null;
    private final String TAG ="Mainactivity";
    private Button btn_save, btn_resume;
    private ImageView iv_canvas;
    private Bitmap baseBitmap;
    Bitmap mutableBitmap;
    private Paint paint;
    private int width;
    int[] Points;
    String filename;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
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
        setContentView(R.layout.activity_image);
        Intent intent=getIntent();
        filename = intent.getStringExtra("filename");
        double[] ContourBArray = intent.getDoubleArrayExtra("contourBArray");
        width = intent.getIntExtra("width",1024);
        //Log.d(TAG,"CountourB"+ContourBArray[0]+ContourBArray[1]+ContourBArray[2]+ContourBArray[3]+ContourBArray[4]+ContourBArray[5]+ContourBArray[6]+ContourBArray[7]);
        //Log.d(TAG,filename);
        baseBitmap = getLoacalBitmap(filename); //从本地取图片(在cdcard中获取)
        // 初始化一个画笔，笔触宽度为5，颜色为红色
        paint = new Paint();
        paint.setStrokeWidth(5);
        paint.setColor(Color.RED);
        iv_canvas = (ImageView) findViewById(R.id.iv_canvas);
        btn_save = (Button) findViewById(R.id.btn_save);
        btn_resume = (Button) findViewById(R.id.btn_resume);
        iv_canvas .setImageBitmap(baseBitmap);
        Points = axistansform(ContourBArray);
        //Log.d(TAG,"Points"+Points[0]+Points[1]+Points[2]+Points[3]+Points[4]+Points[5]+Points[6]+Points[7]);
        drawfourPoint(Points);
        btn_save.setOnClickListener(click);
        btn_resume.setOnClickListener(click);
        iv_canvas.setOnTouchListener(touch);
    }
    private int[] axistansform(double[] ContourBArray){
        int[] Points= new int[8];
        for(int i = 0;i<4;i++){
            Points[i*2+1] = (int)ContourBArray[i*2+0];
            Points[i*2+0] = width - (int)ContourBArray[i*2+1];
        }
        return Points;
    }
    private void drawfourPoint(int[] Points){
        Canvas canvas;
        mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(mutableBitmap);
        float[] opts= {(float) Points[0],(float) Points[1],(float) Points[2],(float) Points[3],(float) Points[2],(float) Points[3],(float) Points[4],(float) Points[5],
                (float) Points[4],(float) Points[5],(float) Points[6],(float) Points[7],(float) Points[6],(float) Points[7],(float) Points[0],(float) Points[1]};
        for(int i = 0;i<4;i++){
            int x = Points[i*2+0];
            int y = Points[i*2+1];
            int radius = 15;
            canvas.drawCircle(x, y, radius, paint);
        }
        canvas.drawLines(opts,paint);
        iv_canvas.setImageBitmap(mutableBitmap);
    }
    private View.OnTouchListener touch = new View.OnTouchListener() {
        int i;
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                // 用户按下动作
                case MotionEvent.ACTION_DOWN:
                    // 记录开始触摸的点的坐标
                    float startX = event.getX();
                    float startY = event.getY();
                    i = findtheNearestPoint(startX,startY);
                    Log.d(TAG,"startX"+startX+"startY"+startY);
                    Log.d(TAG,"Points"+Points[0]+" "+Points[1]+","+Points[2]+" "+Points[3]+","+Points[4]+" "+Points[5]+","+Points[6]+" "+Points[7]);
                    Log.d(TAG,"i"+i);
                    break;
                // 用户手指在屏幕上移动的动作
                case MotionEvent.ACTION_MOVE:
                 // 记录移动位置的点的坐标
                     float stopX = event.getX();
                     float stopY = event.getY();
                     //根据两点坐标，绘制连线
                     Points[i*2+0]=(int)stopX;
                     Points[i*2+1]=(int)stopY;
                     Log.d(TAG,"PointsAfter"+Points[0]+" "+Points[1]+","+Points[2]+" "+Points[3]+","+Points[4]+" "+Points[5]+","+Points[6]+" "+Points[7]);
                     drawfourPoint(Points);
                     break;
                case MotionEvent.ACTION_UP:
                     break;
                default:
                     break;
            }
            return true;
        }
    };
    private int findtheNearestPoint(float startX,float startY){
        int index = 0;
        double dis = 100000;
        for(int i =0;i<4;i++){
            double cdis = Math.pow(((int)startX-Points[i*2+0])+((int)startY-Points[i*2+1]),2);
            if(cdis<dis){
                dis = cdis;
                index = i;
            }
        }
        return index;
    }
    private View.OnClickListener click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_save:
                    Intent intent=new Intent(ImageActivity.this,FinalActivity.class);
                    intent.putExtra("filename",filename);
                    intent.putExtra("Points",Points);
                    startActivity(intent);
                    break;
                case R.id.btn_resume:
                    //Toast.makeText(ImageActivity.this,"resume is clicked",Toast.LENGTH_SHORT).show();
                    Intent intent2=new Intent(ImageActivity.this,MainActivity.class);
                    startActivity(intent2);
                    ImageActivity.this.finish();
                    break;
                default:
                    break;
            }
        }
    };
    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);  ///把流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
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
