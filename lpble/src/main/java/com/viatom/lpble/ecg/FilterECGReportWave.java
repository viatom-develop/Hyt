package com.viatom.lpble.ecg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;


import com.viatom.lpble.data.entity.RecordEntity;
import com.viatom.lpble.data.entity.ReportEntity;
import com.viatom.lpble.ui.ReportListFragment;

import java.util.Collections;
import java.util.List;

public class FilterECGReportWave extends View {
    public static final String TAG = FilterECGReportWave.class.getSimpleName();

    public final static int ECG_DATA_SAMPLING_FREQUENCY = 125; //500Hz
    public static final short NULL_VALUE = Short.MAX_VALUE;



    private static final float grid1mmLength = 6.0f; //1mm长度
    private static final float sampleDis = 1; //抽点距离
    //    private static final float standard1mV = (32767 / 4033) * 12 * 8;
    private final double standard1mV = (float) ((1.0035 * 1800) / (4096 * 178.74));
    private static final float rulerStandardWidth = grid1mmLength * 5 * 0.7f;
    private static final float rulerZeroWidth = grid1mmLength * 5 * 0.15f;//5mm
    private static final float rulerTotalWidth = rulerStandardWidth + rulerZeroWidth * 2;
    private static final float perLineVal = 4f;//每行2.5毫伏最大
    private static final float perLineHeight = perLineVal * grid1mmLength * 10;//每行2.5mV,10mm/mV
    private static final float zeroLineVal = 1.0f;//基线从1mV处画，上面1.5mV下面1mV

    //画图相关
    private float imgWidth, imgHeight;
    private float xDis;//未抽点画图点距离

    private int source = 1;
//    private int fragmentIndex = 0; // 当前报告页码
    private int startPoint = 0;   // 开始取点位置
    private int drawDataSize = 0; // 当前绘图取点长度

    private ReportEntity.Fragment fragment;

    //画笔
    private Paint labelPaint;

    //数据
    int maxInvalidValue = 32767;
    int minInvalidValue = -32768;
    final static int FIXED_POINT_COUNT = (int) (7.6 * 125); //固定画7.6s

    private ReportEntity report;
    private RecordEntity record;

    public FilterECGReportWave(Context context, RecordEntity record, ReportEntity report,
                               float imgWidth, float imgHeight, int source, ReportEntity.Fragment fragment, int startPoint) {
        super(context);
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.report = report;
        this.record = record;
        this.source = source;
        this.fragment = fragment;
        this.startPoint = startPoint;

        xDis = 1f / ECG_DATA_SAMPLING_FREQUENCY * 25 * grid1mmLength; //25mm/s,未抽点的
        xDis *= sampleDis;//抽点后的两点距离

        initParams();

    }

    private void initParams() {
        labelPaint = new Paint();
    }


    public int getDrawDataSize() {
        return drawDataSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        Log.d(TAG, "绘制ECG波形");
        drawGrid(canvas);
        drawRuler(canvas);
        drawWave(canvas);

        drawLabels(canvas);
    }

    private void drawLabels(Canvas canvas) {
//        ReportEntity.Fragment item = report.getFragmentList().get(fragmentIndex);
        int startIndex = Collections.binarySearch(report.getPosList(), Integer.valueOf(fragment.getStartPose()), Integer::compareTo);
        //第一个标记的index为第一个大于或等于startPose的pos值的index;
        int startLabelIndex = startIndex >= 0 ? startIndex : (Math.abs(startIndex) - 1);
        //最后一个标记的index为第一个大于或等于endPose的pos值的index - 1;
        int endIndex = Collections.binarySearch(report.getPosList(), Integer.valueOf(fragment.getEndPose()), Integer::compareTo);
        int endLabelIndex = endIndex >= 0 ? endIndex : (Math.abs(endIndex) - 1) - 1;

        //从 startLabelIndex 遍历到endLabelIndex;
        List<Integer> posList = report.getPosList();
        List<String>  labelList = report.getLabelList();
        int preLabelPose = -1;
        float preLabelX = -1;
        final float textSize = 15;
        labelPaint.setTextSize(textSize);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = startLabelIndex; i <= endLabelIndex; i++) {
            int pos = posList.get(i);
            if(pos / 2 - startPoint > FIXED_POINT_COUNT)
                return;
            String label = labelList.get(i);
            float tempX = rulerTotalWidth + (pos/2 -startPoint) * xDis;
            labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(label, tempX, textSize, labelPaint);

            //计算rr间期和心率
            if(preLabelPose != -1) {
                // 两个点的间距 除频率 250得到秒数， 乘1000得到毫秒数；
                int rrPeriod = (pos - preLabelPose) * 4;
                labelPaint.setTypeface(Typeface.SANS_SERIF);
                canvas.drawText(String.valueOf(rrPeriod), (tempX + preLabelX)/2, textSize + 30, labelPaint);

                //心率等于60s除以rr间隙（一次心跳的时长）
                int hr = (int) (60/(rrPeriod/1000f));
                canvas.drawText(String.valueOf(hr), (preLabelX + tempX)/2, textSize, labelPaint);
            }
            preLabelPose = pos;
            preLabelX = tempX;
        }
    }

    /**
     * 画网格
     *
     * @param canvas
     */
    protected void drawGrid(Canvas canvas) {
        //初始化画笔
        Paint gridPaint1mm = new Paint();
        gridPaint1mm.setAntiAlias(true);
        gridPaint1mm.setStyle(Paint.Style.FILL);
        gridPaint1mm.setStrokeWidth(1.0f);
//        gridPaint1mm.setColor(Color.argb(255, 255, 150, 240));
        gridPaint1mm.setColor(Color.parseColor("#FED4E8"));

        Paint gridPaint5mm = new Paint();
        gridPaint5mm.setAntiAlias(true);
        gridPaint5mm.setStyle(Paint.Style.FILL);
        gridPaint5mm.setStrokeWidth(1.5f);
//        gridPaint5mm.setColor(Color.argb(255, 255, 170, 220));
        gridPaint5mm.setColor(Color.parseColor("#FF8384"));

        int lineNum = (int) (imgWidth / grid1mmLength); //列数
        int rowNum = (int) (imgHeight / grid1mmLength); //行数
        Log.d(TAG, "列数" + lineNum + "行数" + rowNum);

        //1mm 竖线
        for (float i = 0, j = 0; j <= lineNum; i += grid1mmLength, j++) {
            //最后一行，画上n像素，避免画在View外面
            if (j == lineNum)
                i -= gridPaint1mm.getStrokeWidth();
            canvas.drawLine(i, 0, i, imgHeight, gridPaint1mm);
        }

        //1mm 横线
        for (float i = 0, j = 0; j <= rowNum; i += grid1mmLength, j++) {
            if (j == rowNum)
                i -= gridPaint1mm.getStrokeWidth();
            canvas.drawLine(0, i, imgWidth, i, gridPaint1mm);
        }

        //5mm竖线
        for (float i = 0, j = 0; j <= (lineNum / 5); i += grid1mmLength * 5, j++) {
            canvas.drawLine(i, 0, i, imgHeight, gridPaint5mm);
            if (j == lineNum / 5)
                i -= gridPaint5mm.getStrokeWidth();
        }

        // 5mm横线
        for (float i = 0, j = 0; j <= (rowNum / 5); i += grid1mmLength * 5, j++) {
            if (j == rowNum / 5)
                i -= gridPaint5mm.getStrokeWidth();
            canvas.drawLine(0, i, imgWidth, i, gridPaint5mm);
        }

    }

    /**
     * 画标尺
     *
     * @param canvas
     */
    protected void drawRuler(Canvas canvas) {
        float zeroLine = 0;
        float standardLine = 0;

        Paint linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.FILL);
//        linePaint.setStrokeWidth((float) 2.5);
        linePaint.setColor(Color.argb(255, 0, 0, 0));

        //基线，从1mV处开始画，行高2.5mV,10mm/mV
        zeroLine = (perLineVal - zeroLineVal) * grid1mmLength * 10;
        standardLine = zeroLine - grid1mmLength * 10;//1mV线
        canvas.drawLine(0, zeroLine, rulerZeroWidth,
                zeroLine, linePaint);
        canvas.drawLine(rulerZeroWidth, zeroLine, rulerZeroWidth
                , standardLine, linePaint);
        canvas.drawLine(rulerZeroWidth, standardLine
                , rulerZeroWidth + rulerStandardWidth, standardLine, linePaint);
        canvas.drawLine(rulerZeroWidth + rulerStandardWidth,
                zeroLine, rulerZeroWidth + rulerStandardWidth,
                standardLine, linePaint);
        canvas.drawLine(rulerZeroWidth + rulerStandardWidth,
                zeroLine, rulerZeroWidth * 2 + rulerStandardWidth,
                zeroLine, linePaint);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(25);
        textPaint.setStrokeWidth((float) 2.5);
        textPaint.setColor(Color.argb(255, 0, 0, 0));
        textPaint.setTextAlign(Paint.Align.CENTER);
        String rulerStr = "1mV";
//        canvas.drawText(rulerStr, rulerZeroWidth + rulerStandardWidth * 0.5f, zeroLine + getStringTopHeight(textPaint, rulerStr), textPaint);
    }

    /**
     * 画心电波形
     *
     * @param canvas
     */
    protected void drawWave(Canvas canvas) {
        if (report == null) {
            Log.d(TAG, "record is null");
            return;
        }
        //画笔
        Paint linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setStrokeWidth((float) 1.5);
        linePaint.setColor(Color.argb(255, 0, 0, 0));

        //数据
//        short[] chartY = RecordEntity.Companion.getFilterWaveData(record);
        float[] chartY = record.getData();
        Log.d(TAG, "drawWave");
        Log.d(TAG, "chartY.length == " + chartY.length);
        float preTempX = 0, preTempY = 0, preChartY = 0;

        int i = startPoint;

        int lineNum = 0;//行号

        for (int l = 0; i < startPoint + FIXED_POINT_COUNT; i += sampleDis, l++) {
//        for (int l = 0; i < (startPoint + drawDataSize); i+=sampleDis,l++) {
            //第一行从ruler后面开始
//            float tempX = ( (lineNum==0 && fragmentIndex == 1) ? rulerTotalWidth : 0)+ l * xDis;
//            if(i == chartY.length) {
//                return;
//            }
            float tempX = ((lineNum == 0) ? rulerTotalWidth : 0) + l * xDis;
            //Y值，基线Y坐标-数据毫伏长度+行号*行高
            if (chartY[i] == NULL_VALUE)
                Log.d(TAG, "null value found");

//            float yVal = (float) (chartY[i] * (1.0035 * 1800) / (4096 * 178.74));
            float yVal = chartY[i];
            float tempY = (perLineHeight - zeroLineVal * grid1mmLength * 10)
                    - (yVal/*chartY[i]*/) * grid1mmLength * 10 + lineNum * perLineHeight;

            //换行
            if (tempX >= imgWidth) {
                tempX = 0;
                tempY += perLineHeight;
                preTempX = tempX;
                preTempY = tempY;
                preChartY = chartY[i];
                l = 0;//行内index
                lineNum = lineNum + 1;
            }

            //画线，第一个点不画
            if (preTempX != 0 && chartY[i] != NULL_VALUE && preChartY != NULL_VALUE/*|| preTempY != 0*/) {
                canvas.drawLine(preTempX, preTempY, tempX, tempY, linePaint);
            }

            preTempX = tempX;
            preTempY = tempY;
            preChartY = chartY[i];
        }
    }

    private float getStringWidth(Paint paint, String str) {
        return paint.measureText(str);
    }

    private float getStringHeight(Paint paint, String str) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return (Math.abs(fontMetrics.top)) + Math.abs(fontMetrics.bottom);
    }

    private float getStringTopHeight(Paint paint, String str) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return Math.abs(fontMetrics.top);
    }

    private float getStringBottomHeight(Paint paint, String str) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return Math.abs(fontMetrics.bottom);
    }
}
