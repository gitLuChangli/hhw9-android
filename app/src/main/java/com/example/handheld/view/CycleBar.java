package com.example.handheld.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.example.handheld.R;

public class CycleBar extends View {

    private Paint paint, paintLeft, paintInfo;//mPaintTime,mPaintTimeUnit;
    private int mRoundColor;
    private int mRoundProgressColor;
    private int mTextColor;
    private float mTextSize;
    private float mRoundWidth;
    private int mMax;
    private int mProgress;
    private boolean bTextIsDisplayable;
    private int mStyle;
    private int mPosition;
    private String mTextDis;
    private String mInfo;

    public static final int STROKE = 0;
    public static final int FILL = 1;

    public CycleBar(Context context) {
        this(context, null);
    }

    public CycleBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CycleBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paint = new Paint();
        paintInfo = new Paint();

        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.cycleBar);

        mRoundColor = mTypedArray.getColor(R.styleable.cycleBar_roundColor, Color.WHITE);
        mRoundProgressColor = mTypedArray.getColor(R.styleable.cycleBar_roundProgressColor, Color.GRAY);
        mTextColor = mTypedArray.getColor(R.styleable.cycleBar_textColor, Color.WHITE);
        mTextSize = mTypedArray.getDimension(R.styleable.cycleBar_textSize, 40);
        mTextDis = mTypedArray.getString(R.styleable.cycleBar_textDis);
        mRoundWidth = mTypedArray.getDimension(R.styleable.cycleBar_roundWidth, 5);
        mMax = mTypedArray.getInteger(R.styleable.cycleBar_max, 100);
        bTextIsDisplayable = mTypedArray.getBoolean(R.styleable.cycleBar_textIsDisplayable, true);
        mStyle = mTypedArray.getInt(R.styleable.cycleBar_style, 0);
        mPosition = mTypedArray.getInteger(R.styleable.cycleBar_position, 0);
        mInfo = mTypedArray.getString(R.styleable.cycleBar_information);

        mTypedArray.recycle();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centre = getWidth() / 2;
        int radius = (int) (centre - mRoundWidth / 2);
        paint.setColor(Color.BLACK);
        paint.setAlpha(30);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mRoundWidth);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Cap.ROUND);
        canvas.drawCircle(centre, centre, radius, paint);

        paint.setStrokeWidth(0);
        paint.setColor(mTextColor);
        paint.setTextSize(mTextSize);
        paint.setTypeface(Typeface.DEFAULT);

        paintLeft = new Paint();
        paintLeft.setStrokeWidth(0);
        paintLeft.setColor(mTextColor);
        paintLeft.setAntiAlias(true);
        paintLeft.setTextSize(mTextSize);

        paintInfo.setAntiAlias(true);
        paintInfo.setStrokeWidth(0);
        paintInfo.setColor(mTextColor);
        paintInfo.setTextSize(mTextSize / 3);
        paintInfo.setTypeface(Typeface.DEFAULT);

        mTextDis = this.mProgress + "%";

        float textWidth = paintLeft.measureText(mTextDis);
        float infoWidth = paintInfo.measureText(mInfo);

        if (bTextIsDisplayable && mStyle == STROKE) {
            canvas.drawText(mTextDis, centre - textWidth / 2, centre + mTextSize / 5, paintLeft);
            canvas.drawText(mInfo, centre - infoWidth / 2, centre + mTextSize, paintInfo);
        }
        paint.setAlpha(100);
        paint.setStrokeWidth(mRoundWidth);
        paint.setColor(mRoundProgressColor);

        RectF oval = new RectF(centre - radius, centre - radius, centre
                + radius, centre + radius);

        switch (mStyle) {
            case STROKE: {
                paint.setStyle(Paint.Style.STROKE);
                if (mMax != 0) {
                    canvas.drawArc(oval, 180, 360 * mProgress / mMax, false, paint);
                }
                break;
            }
            case FILL: {
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                if (mProgress != 0 && mMax != 0)
                    canvas.drawArc(oval, 180, 360 * mProgress / mMax, true, paint);
                break;
            }
        }
    }


    public synchronized int getMax() {
        return mMax;
    }

    public synchronized void setMax(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max not less than 0");
        }
        this.mMax = max;
    }

    public synchronized int getProgress() {
        return mProgress;
    }

    public synchronized void setProgress(int progress) {
        if (progress < 0) {
            throw new IllegalArgumentException("progress not less than 0");
        }
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress <= mMax) {
            this.mProgress = progress;
            postInvalidate();
        }
    }

    public int getCricleColor() {
        return mRoundColor;
    }

    public void setCricleColor(int cricleColor) {
        this.mRoundColor = cricleColor;
    }

    public int getCricleProgressColor() {
        return mRoundProgressColor;
    }

    public void setCricleProgressColor(int cricleProgressColor) {
        this.mRoundProgressColor = cricleProgressColor;
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSize) {
        this.mTextSize = textSize;
    }

    public float getRoundWidth() {
        return mRoundWidth;
    }

    public void setRoundWidth(float roundWidth) {
        this.mRoundWidth = roundWidth;
    }
	
}
