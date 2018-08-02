package com.meitu.qihangni.scrollpageproject;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.OverScroller;

/**
 * @author nqh 2018/7/31
 */
public class ScrollPageLayout extends LinearLayout implements NestedScrollingParent {
    private static final String TAG = "ScrollPageLayout";
    private View mTop;
    private View mNav;
    private ViewPager mViewPager;
    private int mTopViewHeight;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private ValueAnimator mOffsetAnimator;
    private Interpolator mInterpolator;
    private int mMaximumVelocity, mMinimumVelocity;
    private int TOP_CHILD_FLING_THRESHOLD = 3;
    private float mLastY;
    private boolean mDragging;
    private int mMaxHight = 600;
    private int mMinHight = 400;
    private int mAnimateTime = 300;
    private boolean isSetAnimateTime = true;
    private int mDy = 0;

    public ScrollPageLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
        mScroller = new OverScroller(context);
        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    }

    /**
     * @param maxHight 底部可以提起的最高高度
     */
    public void setMaxHight(int maxHight) {
        this.mMaxHight = maxHight;
    }

    /**
     * @param minHight 底部初始高度
     */
    public void setMinHight(int minHight) {
        this.mMinHight = minHight;
    }

    /**
     * @param time 固定的动画时间
     */
    public void setAnimateTime(int time) {
        isSetAnimateTime = true;
        mAnimateTime = time;
    }

    /**
     * 取消动画时间固定
     */
    public void cancelAnimateTime() {
        isSetAnimateTime = false;
    }


    /**
     * 该方法决定了当前控件是否能接收到其内部View（而非直接子View）滑动时的参数
     * 如果只涉及到纵向滑动，这里可以根据nestedScrollAxes进行纵向判断
     */
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
//        Log.i(TAG, "onStartNestedScroll");
        return true;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
//        Log.i(TAG, "onNestedScrollAccepted");
    }

    @Override
    public void onStopNestedScroll(View target) {
        Log.i(TAG, "onStopNestedScroll: " + mDy);
        boolean hiddenTop = mDy > 0 && getScrollY() < (mMaxHight);
        Log.i(TAG, " hiddenTop: " + hiddenTop + " getScrollY: " + getScrollY() + " mTopViewHeight: " + mTopViewHeight);
        boolean showTop = mDy < 0 && getScrollY() >= 0 && !target.canScrollVertically(-1);
        if (hiddenTop || showTop) {
            animateScroll(mDy, computeDuration(mDy), false);
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
//        Log.i(TAG, "onNestedScroll");
    }

    /**
     * 该方法的会传入内部View移动的dx,dy
     * 如果你需要消耗一定的dx,dy，就通过最后一个参数consumed进行指定，例如我要消耗一半的dy，就可以写consumed[1]=dy/2
     */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
// 不可以置为零，会导致悬停情况下不回弹
//        mDy = 0;
        boolean hiddenTop = dy > 0 && getScrollY() < (mMaxHight);
        Log.i(TAG, " hiddenTop: " + hiddenTop + " getScrollY: " + getScrollY() + " mTopViewHeight: " + mTopViewHeight);
        boolean showTop = dy < 0 && getScrollY() >= 0 && !target.canScrollVertically(-1);
        if (hiddenTop || showTop) {
//            拖拽
            scrollBy(0, dy);
//            animateScroll(dy, computeDuration(dy), false);
            consumed[1] = dy;
            mDy = dy;
        }

    }

    /**
     * 你可以捕获对内部View的fling事件，如果return true则表示拦截掉内部View的事件。
     */
    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Log.i(TAG, "onNestedFling");
        //如果是recyclerView 根据判断第一个元素是哪个位置可以判断是否消耗
        //这里判断如果第一个元素的位置是大于TOP_CHILD_FLING_THRESHOLD的
        //认为已经被消耗，在animateScroll里不会对velocityY<0时做处理
        if (target instanceof RecyclerView && velocityY < 0) {
            final RecyclerView recyclerView = (RecyclerView) target;
            final View firstChild = recyclerView.getChildAt(0);
            final int childAdapterPosition = recyclerView.getChildAdapterPosition(firstChild);
            consumed = childAdapterPosition > TOP_CHILD_FLING_THRESHOLD;
        }
        if (!consumed) {
            animateScroll(velocityY, computeDuration(0), consumed);
        } else {
            animateScroll(velocityY, computeDuration(velocityY), consumed);
        }
        return true;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
//        Log.i(TAG, "onNestedPreFling");
        //不做拦截 可以传递给子View
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
//        Log.i(TAG, "getNestedScrollAxes");
        return 0;
    }

    /**
     * 根据速度计算滚动动画持续时间
     *
     * @param velocityY
     * @return
     */
    private int computeDuration(float velocityY) {
        if (isSetAnimateTime) {
            return mAnimateTime;
        }
        final int distance;
        if (velocityY > 0) {
            distance = Math.abs(mTop.getHeight() - getScrollY());
        } else {
            distance = Math.abs(mTop.getHeight() - (mTop.getHeight() - getScrollY()));
        }
        final int duration;
        velocityY = Math.abs(velocityY);
        if (velocityY > 0) {
            duration = 3 * Math.round(1000 * (distance / velocityY));
        } else {
            final float distanceRatio = (float) distance / getHeight();
            duration = (int) ((distanceRatio + 1) * 150);
        }
        return duration;
    }

    private void animateScroll(float velocityY, final int duration, boolean consumed) {
        final int currentOffset = getScrollY();
        final int topHeight = mTop.getHeight();
        if (mOffsetAnimator == null) {
            mOffsetAnimator = new ValueAnimator();
            mOffsetAnimator.setInterpolator(mInterpolator);
            mOffsetAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (animation.getAnimatedValue() instanceof Integer) {
                        scrollTo(0, (Integer) animation.getAnimatedValue());
                    }
                }
            });
        } else {
            mOffsetAnimator.cancel();
        }
        mOffsetAnimator.setDuration(Math.min(duration, 600));
//        Log.i(TAG, "mOffsetAnimator duration" + mOffsetAnimator.getDuration());
        if (velocityY >= 0) {
            mOffsetAnimator.setIntValues(currentOffset, topHeight);
            mOffsetAnimator.start();
        } else {
            //如果子View没有消耗down事件 那么就让自身滑倒0位置
            if (!consumed) {
                mOffsetAnimator.setIntValues(currentOffset, 0);
                mOffsetAnimator.start();
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTop = findViewById(R.id.id_scrollpagelayout_topview);
        mNav = findViewById(R.id.id_scrollpagelayout_indicator);
        View view = findViewById(R.id.id_scrollpagelayout_viewpager);
        if (!(view instanceof ViewPager)) {
            throw new RuntimeException(
                    "id_scrollpagelayout_viewpager show used by ViewPager !");
        }
        mViewPager = (ViewPager) view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //不限制顶部的高度
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        LinearLayout.LayoutParams mTopLayoutParams = (LayoutParams) mTop.getLayoutParams();
        mTopLayoutParams.height = heightMeasureSpec - mMinHight;
        mTop.setLayoutParams(mTopLayoutParams);
        getChildAt(0).measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        ViewGroup.LayoutParams params = mViewPager.getLayoutParams();
        params.height = getMeasuredHeight() - mNav.getMeasuredHeight();
        setMeasuredDimension(getMeasuredWidth(), mMinHight + mNav.getMeasuredHeight() + mViewPager.getMeasuredHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mTopViewHeight = mTop.getMeasuredHeight();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) {
            y = 0;
        }
        if (y > mMaxHight) {
            y = mMaxHight;
        }
        if (y != getScrollY()) {
            super.scrollTo(x, y);
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDy = 0;
//        Log.i(TAG, " event: " + event.toString());
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        int action = event.getAction();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                mLastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                scrollBy(0, (int) -dy);
                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                recycleVelocityTracker();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                mDragging = false;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityY = (int) mVelocityTracker.getYVelocity();
                animateScroll(-velocityY, computeDuration(-velocityY), false);
//                拖拽
//                if (Math.abs(velocityY) > mMinimumVelocity) {
//                    fling(-velocityY);
//                }
                recycleVelocityTracker();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public void fling(int velocityY) {
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mMaxHight);
        invalidate();
    }
}
