package com.viatom.lpble.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorRes;

import com.viatom.lpble.R;
import com.viatom.lpble.ble.DataController;
import com.viatom.lpble.constants.Constant.EcgViewConfig;

import static com.viatom.lpble.ble.DataController.dataSrc;

public class EcgBkg extends View {
    private Paint bkg;
    private Paint bkg_paint_1;
    private Paint bkg_paint_5;
    private Paint cellBkg;

    private Canvas canvas;

    public int mWidth;
    public int mHeight;

    private int maxIndex;

    private int cellSize = EcgViewConfig.Companion.getECG_CELL_SIZE();

    private int paddingTop = EcgViewConfig.Companion.getPADDING_TOP();
    private int cellHeight;


    @ColorRes
    private int bgColor = R.color.color_ecgbg;
    @ColorRes
    private int gridColor5mm = R.color.color_ecgbg_grid_5mm;
    @ColorRes
    private int gridColor1mm = R.color.color_ecgbg_grid_1mm;

    @ColorRes
    private int cellBkgColor = R.color.color_ecgbg_cell;

    public EcgBkg(Context context) {
        super(context);
        init(null, 0);
    }

    public EcgBkg(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public EcgBkg(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }


    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.EcgView, defStyle, 0);

        a.recycle();

        iniPaint();
    }

    private void iniPaint() {

        bkg = new Paint();
        bkg.setColor(getColor(bgColor));

        bkg_paint_1 = new Paint();
        bkg_paint_1.setColor(getColor(gridColor1mm));
        bkg_paint_1.setStyle(Paint.Style.STROKE);
        bkg_paint_1.setStrokeWidth(2.0f);

        bkg_paint_5 = new Paint();
        bkg_paint_5.setColor(getColor(gridColor5mm));
        bkg_paint_5.setStyle(Paint.Style.STROKE);
        bkg_paint_5.setStrokeWidth(1.0f);

        cellBkg = new Paint();
        cellBkg.setColor(getColor(cellBkgColor));
        cellBkg.setStyle(Paint.Style.FILL);
        cellBkg.setAntiAlias(true);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        iniParam();

        this.canvas = canvas;
        drawBkg(canvas);
    }

    private void iniParam() {

        maxIndex = DataController.maxIndex;

        if (dataSrc == null) {
            dataSrc = new float[maxIndex];
        }

        mWidth = getWidth();
        mHeight = getHeight();

        cellHeight = (mHeight - cellSize * paddingTop ) / cellSize;
    }

    private void drawBkg(Canvas canvas) {

        canvas.drawColor(getColor(bgColor));

        for (int c = 0; c < cellSize;  c++){

            RectF rectF = new RectF(0, (paddingTop * (c + 1) + cellHeight * c), mWidth,  (paddingTop * (c + 1) + cellHeight * c) + cellHeight);// 设置个新的长方形
            canvas.drawRoundRect(rectF, 20, 15, cellBkg);


            // 5mm y
            for (int i = 0; i < cellHeight/(5/DataController.mm2px); i++) {
                Path p = new Path();
                p.moveTo(0,  i*(5/DataController.mm2px) + paddingTop * (c + 1) + cellHeight * c);
                p.lineTo(mWidth,  i*(5/DataController.mm2px) + paddingTop * (c + 1) + cellHeight * c );


                canvas.drawPath(p, bkg_paint_5);
            }


            // 5mm x
            for (int i = 0; i < mWidth/(5/ DataController.mm2px) + 1; i++) {
                Path p = new Path();
                p.moveTo(i*5/ DataController.mm2px, paddingTop* (c+1) + c * cellHeight );
                p.lineTo(i*5/ DataController.mm2px, paddingTop* (c+1) + c * cellHeight + cellHeight  );
                canvas.drawPath(p, bkg_paint_5);
            }

        }



    }

    private int getColor(int resource_id) {
        return getResources().getColor(resource_id);
    }

    public void setBgColor(@ColorRes int bgColor) {
        this.bgColor = bgColor;
    }

}
