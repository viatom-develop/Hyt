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
import com.viatom.lpble.ble.CollectUtil;
import com.viatom.lpble.ble.DataController;
import com.viatom.lpble.constants.Constant;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class EcgView extends View {

    private TextPaint mTextPaint;
    private Paint bPaint;
    private Paint linePaint;
    private Paint wPaint;
    private Paint cPaint;

    public int mWidth;
    public int mHeight;


    private int maxIndex;


    //单元高度
    private int cellHeight;


    private int cellSize = Constant.EcgViewConfig.Companion.getECG_CELL_SIZE();

    private int paddingTop = Constant.EcgViewConfig.Companion.getPADDING_TOP();

    public int[] mBase = new int[cellSize];

    private Context context;

    private CollectUtil collectUtil;


    public EcgView(Context context) {
        super(context);
        this.context = context;
        init(null, 0);
    }

    public EcgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs, 0);
    }

    public EcgView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;

        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.EcgView, defStyle, 0);

        a.recycle();

        // Set up a default TextPaint object
        iniPaint();
        collectUtil = CollectUtil.Companion.getInstance(context.getApplicationContext());
    }

    private void iniPaint() {
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setTextSize(24);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setStrokeWidth((float) 1.5);
        mTextPaint.setColor(getColor(R.color.color_dashboard_ruler));


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


        cPaint = new Paint();
        cPaint.setColor(getColor(R.color.color_dashboard_ecg_wave_collecting/*colorWhite*/));
        cPaint.setStyle(Paint.Style.STROKE);
        cPaint.setStrokeWidth(4.0f);
        cPaint.setTextAlign(Paint.Align.LEFT);
        cPaint.setTextSize(32);

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

        if ((Constant.BluetoothConfig.Companion.getCurrentRunState() == Constant.RunState.PREPARING_TEST ||
                Constant.BluetoothConfig.Companion.getCurrentRunState()  == Constant.RunState.RECORDING) && DataController.dataSrc.length > 0) {
            drawWave(canvas);
        }
    }

    private void iniParam() {

        maxIndex = DataController.maxIndex; // 画满需要的点数

        if (DataController.dataSrc == null) {
            DataController.dataSrc = new float[maxIndex];
        }


        mWidth = getWidth();
        mHeight = getHeight();


        cellHeight = (mHeight - paddingTop * cellSize) / cellSize; // 每个单元的高度
        float cellGridCount = cellHeight / (5 / DataController.mm2px); //每个单元纵向格子数
        for (int c = 0; c < cellSize; c++) {
            float baseIndex = cellGridCount / 2;
            if (baseIndex % 1 >= 0.5)
                baseIndex++;
            baseIndex -= baseIndex % 1;

            mBase[c] = (int) (baseIndex * (5 / DataController.mm2px)) + (c + 1) * paddingTop + c * cellHeight;
        }


    }

        private void drawWave(Canvas canvas) {

        Path p = new Path();
        Path p1 = new Path();
        for (int c = 0; c < cellSize; c++) {

            p.moveTo(0, mBase[c]);
            p1.moveTo(0, mBase[c]);

            int cellStartIndex = maxIndex / cellSize * c;
            int cellEndIndex = cellStartIndex + maxIndex / cellSize;

            for (int i = cellStartIndex; i < cellEndIndex; i++) {
                if (DataController.dataSrcCollect != null && DataController.dataSrcCollect[i] != 0 ){
                    //手动
                    if (i == DataController.index && i < cellEndIndex - 5) {
                        float y = (mBase[c] - (DataController.amp[DataController.ampKey] * DataController.dataSrc[i + 4] / DataController.mm2px));
                        float x = (float) (i - cellStartIndex + 4) / 5 / DataController.mm2px;
                        p.moveTo(x, y);
                        p1.moveTo(x, y);
                        i = i + 4;

                    } else {
                        float y1 = mBase[c] - (DataController.amp[DataController.ampKey] * DataController.dataSrc[i] / DataController.mm2px);
                        float x1 = (float) (i - cellStartIndex) / 5 / DataController.mm2px;
                        p.moveTo(x1, y1);
                        p1.lineTo(x1, y1);
                    }
                }else{
                    if (i == DataController.index && i < cellEndIndex - 5) {
                        float y = (mBase[c] - (DataController.amp[DataController.ampKey] * DataController.dataSrc[i + 4] / DataController.mm2px));
                        float x = (float) (i - cellStartIndex + 4) / 5 / DataController.mm2px;
                        p.moveTo(x, y);
                        p1.moveTo(x, y);
                        i = i + 4;

                    } else {
                        float y1 = mBase[c] - (DataController.amp[DataController.ampKey] * DataController.dataSrc[i] / DataController.mm2px);
                        float x1 = (float) (i - cellStartIndex) / 5 / DataController.mm2px;
                        p.lineTo(x1, y1);
                        p1.moveTo(x1, y1);
                    }
                }
            }
            canvas.drawPath(p, wPaint);
            canvas.drawPath(p1, cPaint);

        }
    }



    public void clear() {
        DataController.clear();
        DataController.dataSrc = new float[maxIndex];
    }

    private int getColor(int resource_id) {
        return getResources().getColor(resource_id);
    }


}
