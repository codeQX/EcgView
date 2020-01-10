package com.qixin.ecg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

/**
 * @ClassName EcgView
 * @Description 心电图绘制
 * 默认：横向单位为（mm/0.04s即25mm/s），纵向单位为(mm/0.1mv即10mm/mv)
 * @Author qixin
 * @Date 2019-01-09 16:22
 * @Version 1.0
 */
@SuppressWarnings("unused")
public class EcgView extends View {
    //每个小网格的宽度，是高度的多少等分
    final static int VIEW_HEIGHT_EQUAL = 40;

    /**
     * 心电图所需参数
     */
    // 几毫米每毫伏  mm/mv
    public static final float MM_PER_MV_20 = 20f;
    public static final float MM_PER_MV_10 = 10f;
    public static final float MM_PER_MV_5 = 5f;
    public static final float MM_PER_MV_4 = 4f;

    // 增益 心电图的振幅大小 leadData / gain = 电压值mv
    public static final float WAVE_GAIN = 2000f;

    //采样率
    public static final int WAVE_SAMPLING_RATE = 250;

    // 波速 mm/s
    public static final float WAVE_SPEED_VALUE_12 = 12.5f;
    public static final float WAVE_SPEED_VALUE_25 = 25f;
    public static final float WAVE_SPEED_VALUE_50 = 50f;

    /**
     * 标尺相关
     */
    // 1mv标尺距离左右的间距
    final static float RULER_MARGIN_DISTANCE = 15;
    //顶部横线的宽度
    final static float RULER_WIDTH = 20;
    //底部横线的宽度
    final static float RULER_ZERO_WIDTH = 15;
    //标尺总体宽度
    final static float RULER_TOTAL_WIDTH = RULER_WIDTH + 2 * RULER_ZERO_WIDTH
            + RULER_MARGIN_DISTANCE * 2;


    /**
     * 画笔相关
     */
    //心电图的画笔
    private Paint mPaintLine;
    //1mV文字的画笔
    private Paint mPaintText;
    //1mv标尺的画笔
    private Paint mPaintRuler;
    //网格1mm细线的画笔
    private Paint mPaintGrid1mm;
    //网格5mm粗线的画笔
    private Paint mPaintGrid5mm;

    private int mColorGrid1mm = Color.parseColor("#40FF7F50");
    private int mColorGrid5mm = Color.parseColor("#FFFF7F50");

    //定义1mm网格宽高的像素，view高度的40等分
    private float mGrid1mmLength;
    //采样点间的横向距离,
    private float mDisSample;
    //心电图总共多长
    private float mEcgWidth;
    //1mV标尺的高度，像素
    private float mRulerHeight;

    //默认阻抗和振幅 2mv 5mv 10mv
    private float mMmPerMv = MM_PER_MV_10;
    private float mSpeed = WAVE_SPEED_VALUE_25;

    //心电图数据
    private float[] mLeadData = new float[0];

    //View的宽高
    private int mWidth;
    private int mHeight;

    private int mTotalWidth;

    //当前手指在屏幕上坐标
    private float mTouchX;
    @SuppressWarnings("FieldCanBeLocal")
    private float mTouchY;

    //当前手指按下的时候x，y坐标
    private float mTouchDownX;
    private float mTouchDownY;


    /**
     * 滑动相关
     */
    //速度追踪器
    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;
    // 最小滑动速度
    private int mMinimumFlingVelocity;
    // 最大滑动速度
    private int mMaximumFlingVelocity;
    //系统最小滑动距离
    private int mTouchSlop;
    //横向做大能滑动的x坐标
    private int mMaxScrollX = 0;

    public EcgView(Context context) {
        this(context, null);
    }

    public EcgView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EcgView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
        initScroller();

    }

    /**
     * 初始化画笔
     */
    private void initPaints() {
        //心电图的画笔
        mPaintLine = new Paint();
        mPaintLine.setAntiAlias(true);
        mPaintLine.setStyle(Paint.Style.STROKE);
        mPaintLine.setStrokeWidth((float) 3);
        mPaintLine.setColor(Color.BLACK);

        //1mv文字的画笔
        mPaintText = new Paint();
        mPaintText.setTextSize(25);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setStrokeWidth(1.5f);
        mPaintText.setColor(Color.BLACK);// 黑线
        mPaintText.setFakeBoldText(true);

        //1mv标尺的画笔
        mPaintRuler = new Paint();
        mPaintRuler.setAntiAlias(true);
        mPaintRuler.setStyle(Paint.Style.STROKE);//描边
        mPaintRuler.setStrokeWidth(2f);
        mPaintRuler.setColor(Color.BLACK);// 黑线

        //网格1mm细线的画笔
        mPaintGrid1mm = new Paint();
        mPaintGrid1mm.setAntiAlias(true);
        mPaintGrid1mm.setStyle(Paint.Style.FILL);
        mPaintGrid1mm.setStrokeWidth(1);
        mPaintGrid1mm.setColor(mColorGrid1mm);

        //网格5mm粗线的画笔
        mPaintGrid5mm = new Paint();
        mPaintGrid5mm.setAntiAlias(true);
        mPaintGrid5mm.setStyle(Paint.Style.FILL);
        mPaintGrid5mm.setStrokeWidth(1);
        mPaintGrid5mm.setColor(mColorGrid5mm);
    }

    /**
     * 初始化滑动控制参数
     */
    private void initScroller() {
        mScroller = new Scroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();
    }


    /**
     * 初始化相关参数
     */
    private void initParams() {
        //1mm网格宽高像素
        mGrid1mmLength = mHeight / (VIEW_HEIGHT_EQUAL * 1f);
        //每个采样点的横向距离
        mDisSample = mSpeed * mGrid1mmLength / WAVE_SAMPLING_RATE;
        // 心电图的总宽度
        mEcgWidth = mDisSample * mLeadData.length;
        mTotalWidth = (int) (mEcgWidth + RULER_TOTAL_WIDTH);
        //控制滑动的最大距离
        mMaxScrollX = mTotalWidth - mWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        setMeasuredDimension(mTotalWidth, mHeight);

        initParams();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        drawGrid(canvas);
        //将画布位移操作，把原点向下移动半个view高度，此时原点在view中间
        canvas.translate(0, mHeight / 2);
        //开始画1mV标尺
        drawRuler(canvas);
        //将画布位移操作，再把原点向右移动1mV标尺的宽度，此时原点在view中间，1mV标尺的右边
        canvas.translate(RULER_TOTAL_WIDTH, 0);
        //开始画Ecg
        drawEcg(canvas);

    }

    /**
     * 画背景网格
     */
    private void drawGrid(Canvas canvas) {
        if (mTotalWidth < mWidth){
            mTotalWidth = mWidth;
        }
        //画几条竖线
        int countVerLine = (int) (mTotalWidth / mGrid1mmLength);
        // 竖线
        for (int i = 0; i < countVerLine; i++) {
            // 1mm
            canvas.drawLine(i * mGrid1mmLength, 0, i * mGrid1mmLength, mHeight, mPaintGrid1mm);
            // 5mm
            if (i % 5 == 0)
                canvas.drawLine(i * mGrid1mmLength, 0, i * mGrid1mmLength, mHeight, mPaintGrid5mm);
        }

        //画几条横线
        int countHorLine = (int) (mHeight / mGrid1mmLength);
        // 横线
        for (int i = 0; i < countHorLine; i++) {
            // 1mm
            canvas.drawLine(0, i * mGrid1mmLength, mTotalWidth, i * mGrid1mmLength, mPaintGrid1mm);
            // 5mm
            if (i % 5 == 0)
                canvas.drawLine(0, i * mGrid1mmLength, mTotalWidth, i * mGrid1mmLength, mPaintGrid5mm);
        }
    }

    /**
     * 画1mv标尺
     */
    private void drawRuler(Canvas canvas) {
        // 1mV的高度，负数是因为y轴正值朝下，减去2是去掉了1mV画笔的粗的一半
        mRulerHeight = -(mGrid1mmLength * mMmPerMv - 2);

        Path path = new Path();
        path.moveTo(RULER_MARGIN_DISTANCE, 0);
        path.lineTo(RULER_MARGIN_DISTANCE + RULER_ZERO_WIDTH, 0);
        path.lineTo(RULER_MARGIN_DISTANCE + RULER_ZERO_WIDTH, mRulerHeight);
        path.lineTo(RULER_MARGIN_DISTANCE + RULER_ZERO_WIDTH + RULER_WIDTH, mRulerHeight);
        path.lineTo(RULER_MARGIN_DISTANCE + RULER_ZERO_WIDTH + RULER_WIDTH, 0);
        path.lineTo(RULER_MARGIN_DISTANCE + RULER_ZERO_WIDTH * 2 + RULER_WIDTH, 0);

        canvas.drawPath(path, mPaintRuler);

        //写文字
        float baseLine = mPaintText.getTextSize() + 5;
        canvas.drawText("1mV", RULER_MARGIN_DISTANCE, baseLine, mPaintText);

    }

    /**
     * 画心电图轨迹
     */
    private void drawEcg(Canvas canvas) {
        float preTempX = 0f;
        float preTempY = 0f;

        float curX;
        float curY;

        for (int i = 0; i < mLeadData.length; i++) {
            curX = mDisSample * i;
            //数据点 / 增益 = 毫伏
            float mv = mLeadData[i] / WAVE_GAIN;
            curY = mv * mRulerHeight;
            canvas.drawLine(preTempX, preTempY, curX, curY, mPaintLine);
            preTempX = curX;
            preTempY = curY;
        }
    }


    /**
     * 1.重写这个方法是为了消除onTouchEvent的警告
     * 2.还要在ACTION_UP里面调用performClick();
     */
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        obtainVelocityTracker(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                getParent().requestDisallowInterceptTouchEvent(true);

                mTouchX = event.getX();
                mTouchY = event.getY();

                mTouchDownX = mTouchX;
                mTouchDownY = mTouchY;

                //手指按下立刻停止滑动
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                //手指开始在屏幕上移动，不允许父控件拦截，事件交由子控件处理，只能左右滑动
                getParent().requestDisallowInterceptTouchEvent(true);

                float currentX = event.getX();
                float deltaX = currentX - mTouchX;

                mTouchX = currentX;


                int scrollToX = (int) (getScrollX() - deltaX);
                if (scrollToX < 0) {
                    scrollToX = 0;
                }

                if (scrollToX > mMaxScrollX) {
                    scrollToX = mMaxScrollX;
                }
                scrollTo(scrollToX, 0);

                break;
            }
            case MotionEvent.ACTION_UP: {
                //手指抬起后，允许父控件拦截
                getParent().requestDisallowInterceptTouchEvent(false);

                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);

                //获取x轴速度，往左滑是负数，往右滑是正数。和x轴的正负方向一样
                int initialVelocity = (int) velocityTracker.getXVelocity();

                if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                    //如果滑动
                    fling(-initialVelocity);
                } else {
                    //计算手指移动的距离，如果距离小，调用点击事件，否则不操作
                    float currentX = event.getX();
                    float currentY = event.getY();

                    float deltaX = Math.abs(mTouchDownX - currentX);
                    float deltaY = Math.abs(mTouchDownY - currentY);
                    if (deltaX < mTouchSlop && deltaY < mTouchSlop) {
                        //x,y移动的距离都小于系统滑动距离，当做点击事件处理
                        //这里可以 计算按下和抬起的间隔时间，执行长按事件
                        performClick();
                    }

                }

                break;
            }
            case MotionEvent.ACTION_CANCEL: {

                releaseVelocityTracker();
                break;
            }
        }
        return true;
    }

    private void releaseVelocityTracker() {
        if (null != mVelocityTracker) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }


    private void fling(int i) {
        mScroller.fling(getScrollX(), getScrollY(), i, 0, 0, mMaxScrollX, 0, 0);

//        awakenScrollBars(mScroller.getDuration());

        invalidate();
    }


    private void obtainVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            scrollTo(x, y);
            postInvalidate();
        }
    }

    //********************************************公共方法******************************************

    /**
     * 设置心电图数据点
     *
     * @param leadData 心电图滤波后数据
     */
    public void setLeadData(float[] leadData) {
        this.mLeadData = leadData;
        initParams();
        requestLayout();

        invalidate();
    }


    /**
     * 设置阻抗大小 mm/mv
     *
     * @param mmPerMv 阻抗大小
     */
    public void setMmPerMv(float mmPerMv) {
        this.mMmPerMv = mmPerMv;
        invalidate();
    }

    /**
     * 设置波速 (mm/s)
     *
     * @param speed 波速
     */
    public void setSpeed(float speed) {
        this.mSpeed = speed;
        initParams();
        requestLayout();
        //刷新，滑动到最前面
        mScroller.fling(0, getScrollY(), -mMaximumFlingVelocity, 0, 0, mTotalWidth - mWidth, 0, 0);

        invalidate();
    }

}
