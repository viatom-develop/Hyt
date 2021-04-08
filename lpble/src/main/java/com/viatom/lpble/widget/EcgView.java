package com.viatom.lpble.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;

import com.viatom.lpble.R;
import com.viatom.lpble.ble.DataController;
import com.viatom.lpble.constants.Constant;

/**
 * TODO: document your custom view class.
 */
public class EcgView extends View{

    private TextPaint mTextPaint;
    private Paint bPaint;
    private Paint linePaint;
    private Paint wPaint;
    private Paint redPaint;
    private Paint redPaint2;
    private Paint redPaint3;
    private float mTextWidth;
    private float mTextHeight;

    public int mWidth;
    public int mHeight;


    private int maxIndex;


    private GestureDetector detector;

    //设备状态
    private int runState;

    //单元高度
    private int cellHeight;

    private int cellSize = 4;

    public float[] mTop = new float[cellSize];
    public float[] mBottom = new float[cellSize];
    public int[] mBase = new int[cellSize];

    public EcgView(Context context) {
        super(context);
        init(null, 0);
    }

    public EcgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public EcgView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.EcgView, defStyle, 0);

        a.recycle();

        // Set up a default TextPaint object
        iniPaint();
    }

    private void iniPaint() {
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
		mTextPaint.setTextSize(24);
		mTextPaint.setStyle(Paint.Style.FILL);
		mTextPaint.setStrokeWidth((float) 1.5);
		mTextPaint.setColor(getColor(R.color.color_dashboard_ruler));

        redPaint = new Paint();
        redPaint.setColor(getColor(R.color.red_m));
        redPaint.setStyle(Paint.Style.STROKE);
        redPaint.setStrokeWidth(4.0f);

        redPaint2 = new Paint();
        redPaint2.setColor(getColor(R.color.red_b));
        redPaint2.setStyle(Paint.Style.STROKE);
        redPaint2.setStrokeWidth(2.0f);

        redPaint3 = new Paint();
        redPaint3.setColor(getColor(R.color.red_b));
        redPaint3.setStyle(Paint.Style.STROKE);
        redPaint3.setStrokeWidth(1.0f);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setTextSize(15);
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setStrokeWidth((float) 4.0f);
        linePaint.setColor(getColor(R.color.color_dashboard_ruler));

        wPaint = new Paint();
        wPaint.setColor(getColor(R.color.color_dashboard_ecg_wave/*colorWhite*/));
        wPaint.setStyle(Paint.Style.STROKE);
        wPaint.setStrokeWidth(4.0f);
        wPaint.setTextAlign(Paint.Align.LEFT);
        wPaint.setTextSize(32);

        bPaint = new Paint();
        bPaint.setTextAlign(Paint.Align.LEFT);
        bPaint.setTextSize(32);
        bPaint.setColor(getColor(R.color.black));
        bPaint.setStyle(Paint.Style.STROKE);
        bPaint.setStrokeWidth(4.0f);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        iniParam();

        Log.d("EcgView", "runState: " + runState );
        Log.d("EcgView", "maxIndex: " + maxIndex);
        Log.d("EcgView", "dataSrc.length: " + DataController.dataSrc.length);
        if((runState == Constant.BluetoothConfig.RunState.PREPARING_TEST || runState == Constant.BluetoothConfig.RunState.RECORDING) && DataController.dataSrc.length > 0) {
            drawRuler(canvas);

            drawWave(canvas);
        }
    }

    private void iniParam() {
//        SPEED = DataController.SPEED;

        maxIndex = DataController.maxIndex; // 画满需要的点数


        if (DataController.dataSrc == null) {
            DataController.dataSrc = new float[maxIndex];
        }


        mWidth = getWidth();
        mHeight = getHeight();

        cellHeight = mHeight / cellSize;

        float thickGridCount = mHeight/(5/DataController.mm2px);// 纵向格子数


//        float baseIndex = thickGridCount / 2 ; // 波形基线

        for (int i = 0; i < cellSize; i++){
            float baseIndex =  thickGridCount / (cellSize* 2) * (i*2 + 1);
            if( baseIndex % 1 >= 0.5)
                baseIndex ++;
            baseIndex -= baseIndex%1;

            mBase[i] = (int) (baseIndex * (5/DataController.mm2px)); // 波形基线
            mTop[i] = mBase[i] - 20/ DataController.mm2px; //  波形顶线
            mBottom[i] = mBase[i] + 20/ DataController.mm2px; // 波行底线
        }


    }

    private void drawRuler(Canvas canvas) {
//        float chartStartX = (float) (1.0 / (5.0 *  DataController.mm2px));
//        float standardYTop = mBase - (DataController.amp[DataController.ampKey] * 0.5f / DataController.mm2px);
//        float standardTBottom = mBase + (DataController.amp[DataController.ampKey] * 0.5f / DataController.mm2px);
//
//        canvas.drawLine(chartStartX + 20, standardYTop, chartStartX+20, standardTBottom, linePaint);
//
//        String rulerStr =  "1mV";
//        canvas.drawText(rulerStr, chartStartX+25, standardTBottom + 20, mTextPaint);
    }

    private void drawWave(Canvas canvas) {
        Path p = new Path();
        for (int c = 0; c < cellSize; c++){
            p.moveTo(0, mBase[c]);

            int cellStartIndex =  maxIndex / cellSize * c;
            int cellEndIndex = cellStartIndex +  maxIndex / cellSize;

            for (int i = cellStartIndex; i < cellEndIndex; i++) {

                if (i == DataController.index  && i < cellEndIndex -5) {

                    float y = (mBase[c] - (DataController.amp[DataController.ampKey]*DataController.dataSrc[i+4]/ DataController.mm2px));

                    float x = (float) (i - cellStartIndex +4)/5/ DataController.mm2px ;

                    p.moveTo(x , y);
                    i = i+4;
                } else {
                    float y1 = mBase[c] - (DataController.amp[DataController.ampKey]*DataController.dataSrc[i] / DataController.mm2px);


                    float x1 = (float) (i - cellStartIndex)/5/ DataController.mm2px;
                    p.lineTo(x1 , y1);
                }

            }
            canvas.drawPath(p, wPaint);
        }





    }

    public void clear() {
        DataController.clear();
        DataController.dataSrc = new float[maxIndex];
    }

    private int getColor(int resource_id) {
        return getResources().getColor(resource_id);
    }

    public void setRunState(int runState) {
        this.runState = runState;
    }

    public int getCellSize() {
        return cellSize;
    }

    public void setCellSize(int cellSize) {
        this.cellSize = cellSize;
    }
}
