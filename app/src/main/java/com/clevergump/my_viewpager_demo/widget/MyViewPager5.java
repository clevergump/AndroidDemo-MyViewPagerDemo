package com.clevergump.my_viewpager_demo.widget;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import com.clevergump.my_viewpager_demo.utils.DensityUtils;

/**
 * 版本5 (功能相对较完善, bug相对较少的版本). 添加了滑动冲突的解决措施, 可以内嵌 ListView 等.
 *
 * @author zhangzhiyi
 * @version 1.0
 * @createTime 2016/4/6 11:31
 * @projectName MyViewPagerDemo
 */
public class MyViewPager5 extends ViewGroup {

    private int mScreenWidthPixels;
    private int mScreenHeightPixels;
    private int mChildCount;

    // visibility不是GONE的子View的总数
    private int mNonGoneChildCount;

    // 上一次发生成功滑动事件 (成功滑动的意思是: 滑动距离大于mTouchSlop的滑动, 反之则认为是失败滑动, 失败滑动的事件
    // 将会直接舍弃)后, 手指停止处的x坐标. 只有发生了成功滑动, 才会更新该变量的数值; 发生失败滑动, 将不更新该变量的数值.
    private float mLastScrolledRawX;

    // Android系统规定的一个滑动事件如果能被系统认可为滑动事件所必须滑动的最小距离. 也就是本文提到的成功滑动所
    // 必须滑动的最小距离.
    private int mTouchSlop;

    private Scroller mScroller;

    // 发生 ACTION_DOWN 时的 rawX, rawY.
    private float mDownRawX;
    private float mDownRawY;

    // 发生 ACTION_DOWN 时所触摸的子 View的 index (从0开始).
    private int mChildIndexWhenDown;

    // 在某一次事件序列("一次事件序列"是指: 从某次手指按下到本次手指离开屏幕的过程)中, 发生拦截后是不是首次
    // 进入onTouchEvent()方法.
    private boolean mIsFirstEntryAfterInterceptingInThisEventSeries = true;


    public MyViewPager5(Context context) {
        this(context, null);
    }

    public MyViewPager5(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyViewPager5(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mScreenWidthPixels = DensityUtils.getScreenWidthPixels(context);
        mScreenHeightPixels = DensityUtils.getScreenHeightPixels(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
        if (attrs == null) {
            return;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mChildCount == 0) {
            mChildCount = getChildCount();
            // 因为要进行多次测量, 所以将visibility为非GONE的child总数缓存起来, 以供下次测量时直接使用.
            for (int i = 0; i < mChildCount; i++) {
                View child = getChildAt(i);
                int childVisibility = child.getVisibility();
                if (childVisibility != GONE) {
                    measureChild(child, widthMeasureSpec, heightMeasureSpec);
                    mNonGoneChildCount++;
                }
            }
        }

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int width = mScreenWidthPixels * mNonGoneChildCount;
        int height = heightSpecSize;

        // 如果该 ViewGroup的高度指定为 wrap_content
        if (heightSpecMode == MeasureSpec.AT_MOST) {
            int childMaxHeightWithMargin = 0;
            for (int i = 0; i < mChildCount; i++) {
                View child = getChildAt(i);
                int childVisibility = child.getVisibility();
                if (childVisibility != GONE) {
                    MarginLayoutParams mlp = (MarginLayoutParams) child.getLayoutParams();
                    childMaxHeightWithMargin = Math.max(childMaxHeightWithMargin, child.getMeasuredHeight() + mlp.topMargin + mlp.bottomMargin);
                    // 如果有某个child的高度超过了上限值 heightSpecSize, 则整个容器的高度就直接使用该上限值 heightSpecSize
                    if (childMaxHeightWithMargin >= heightSpecSize) {
                        height = heightSpecSize;
                        setMeasuredDimension(width, height);
                        return;
                    }
                }
            }

            // 每一个child的 measuredHeight + mlp.topMargin + mlp.bottomMargin 都小于 heightSpecSize, 那么height就取 childMaxHeightWithMargin
            height = childMaxHeightWithMargin;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mChildCount == 0) {
            mChildCount = getChildCount();
        }
        // 该 ViewGroup的 padding
        int myPaddingLeft = getPaddingLeft();
        int myPaddingRight = getPaddingRight();
        int myPaddingTop = getPaddingTop();
        int myPaddingBottom = getPaddingBottom();

        int childLeft = 0;
        int childRight = 0;
        int childTop = 0;
        int childBottom = 0;

        int myMeasuredHeight = getMeasuredHeight();
        int nextNonGoneChildIndex = 0;

        for (int i = 0; i < mChildCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                MarginLayoutParams childMarginLayoutParams = (MarginLayoutParams) child.getLayoutParams();
                int childMarginLeft = childMarginLayoutParams.leftMargin;
                int childMarginRight = childMarginLayoutParams.rightMargin;
                int childMarginTop = childMarginLayoutParams.topMargin;
                int childMarginBottom = childMarginLayoutParams.bottomMargin;

                childTop = myPaddingTop + childMarginTop;
                childBottom = myMeasuredHeight - myPaddingBottom - childMarginBottom;
                childLeft = mScreenWidthPixels * nextNonGoneChildIndex + childMarginLeft;
                childRight = mScreenWidthPixels * (nextNonGoneChildIndex + 1) - childMarginRight;

                // 仅供测试.
//                if (i > 0) {
//                    childLeft -= 900;
//                    childRight -= 900;
//                }

                child.layout(childLeft, childTop, childRight, childBottom);
                nextNonGoneChildIndex ++;
            }
        }
    }

    // 要想使用 margin, 就必须在该方法内返回一个 MarginLayoutParams 的实例.
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean intercept = false;
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mDownRawX = rawX;
                mDownRawY = rawY;
                calcFingerTouchedChildIndexWhenActionDown();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = rawX - mDownRawX;
                float dy = rawY - mDownRawY;
                // 滑动距离太短, 直接不拦截.
                if (!isScrollExceedingTouchSlop(caclSquareRoot(dx, dy))) {
                    return false;
                }

                // 此时的滑动距离已经大于了 mTouchSlop

                // 如果主要是向水平方向滑动, 那么就拦截
                if (Math.abs(dx) >= Math.abs(dy)) {
                    intercept = true;
                }
                break;
        }

        return intercept;
    }

    /**
     * 计算给定的两个数的平方和的平方根
     */
    private float caclSquareRoot(float dx, float dy) {
        return (float) Math.pow(Math.pow(dx, 2) + Math.pow(dy, 2), 0.5);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currRawX = event.getRawX();

        // 如果这是本次事件序列中发生拦截后首次进入该方法, 那么必须设置 mLastScrolledRawX 的值为本次按下时的坐标
        // mDownRawX. 如果不设置, 将会出错, 因为不设置的话, 该变量将使用默认值0, 这将导致计算出的 dx值与手指实际
        // 滑动的距离差不符. 而添加该判断条件, 表示本次事件序列中, 在拦截后只有首次进入该方法时, 才会设置
        // mLastScrolledRawX 的值为按下时的坐标, 由于在首次进入该方法以后, 最终会在该方法体的最后一行更新
        // mLastScrolledRawX 的值为手指当前滑动到的新位置的坐标(而不再是原先按下时的位置坐标), 那么在本次事件序列
        // 中的第2次, 第3次...进入该方法时, 就不能再将 mLastScrolledRawX 的值设置为最初按下时的位置坐标了. 但是
        // 需要在本次事件序列结束后, 再次重置 mIsFirstEntryAfterInterceptingInThisEventSeries 的值为true, 用于
        // 为下一事件序列做准备. 所以, 需要在 ACTION_UP 中做这个重置的工作.
        if (mIsFirstEntryAfterInterceptingInThisEventSeries) {
            mIsFirstEntryAfterInterceptingInThisEventSeries = false;
            mLastScrolledRawX = mDownRawX;
        }

        // 本次事件和上次成功发生移动的事件相比, 在x方向上移动的位置坐标差.
        float dx = currRawX - mLastScrolledRawX;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 如果上一次的平滑移动还没有结束, 那么就让其立即结束, 立即移动到目标位置
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 如果本次滑动距离小于 mTouchSlop, 则什么都不执行, 也不会更新 mLastScrolledRawX 的数值,
                // 而是直接返回 true 表示消费掉了本次事件, 而直接进入到下一次事件的响应准备中.
                if (!isScrollExceedingTouchSlop(dx)) {
                    return true;
                }
                instantScrollWithFinger(dx);
                break;
            case MotionEvent.ACTION_UP:
                // 手指抬起后, 本次事件序列就结束了, 就重置 mIsFirstEntryAfterIntercepting的值为true, 以不影响下一次的触摸事件.
                mIsFirstEntryAfterInterceptingInThisEventSeries = true;

                // 从手指按下到抬起的整个过程中, 在x方向上移动的位置坐标差.
                float downToUpDx = currRawX - mDownRawX;
                // 屏幕宽度的一半
                int halfScreenWidth = mScreenWidthPixels >> 1;
                // 如果手指离开时, 从总体来看是手指向左滑动(即: 下一个子View逐渐进入到屏幕中), 并且滑动的距离
                // 大于屏幕宽度的一半时, 就继续平滑移动到下一个子View完整停留在屏幕内为止.
                if (downToUpDx < 0 && Math.abs(downToUpDx) >= halfScreenWidth) {
                    smoothScrollToNextChild();
                }
                // 如果手指离开时, 从总体来看是手指向右滑动(即: 上一个子View逐渐进入到屏幕中), 并且滑动的距离
                // 大于屏幕宽度的一半时, 就继续平滑移动到上一个子View完整停留在屏幕内为止.
                else if (downToUpDx > 0 && Math.abs(downToUpDx) >= halfScreenWidth) {
                    smoothScrollToPreviousChild();
                }
                // 如果手指离开时, 从总体来看不论是向哪个方向滑动, 只要滑动的距离小于屏幕宽度的一半, 就会
                // 向与刚才滑动相反的方向平滑移动回到原先的位置然后停止.
                else {
                    smoothScrollBack();
                }
                break;
        }
        mLastScrolledRawX = currRawX;
        return true;
    }

    /**
     * 平滑移动回到原先的位置 (即: 手指按下前, 该控件停留的位置)
     */
    private void smoothScrollBack() {
        float dx = Math.abs(getScrollX()) - mScreenWidthPixels * mChildIndexWhenDown;
        dx = adjustDeltaXDistance(dx);
        int durationTimeMillis = 200;
        smoothScrollBy((int) (dx + 0.5f), 0, durationTimeMillis);
    }

    /**
     * 平滑移动到上一个子View, 然后停止
     */
    private void smoothScrollToPreviousChild() {
        // 说明当前的子View已经随手指向右移动了一大半的距离(该距离超过了整个屏幕宽度的一半)了, 那么只需要继续
        // 向右移动剩余的一小半距离(即: 整个屏幕宽度减去已经移动过的那一大半距离后剩下的距离)即可.
        float dx = Math.abs(getScrollX()) - mScreenWidthPixels * (mChildIndexWhenDown - 1);
        dx = adjustDeltaXDistance(dx);
        int durationTimeMillis = 200;
        smoothScrollBy((int) (dx + 0.5f), 0, durationTimeMillis);
    }

    /**
     * 平滑移动到下一个子View, 然后停止
     */
    private void smoothScrollToNextChild() {
        // 说明当前的子View已经随手指向左移动了一大半的距离(该距离超过了整个屏幕宽度的一半)了, 那么只需要继续
        // 向左移动剩余的一小半距离(即: 整个屏幕宽度减去已经移动过的那一大半距离后剩下的距离)即可.
        float dx = - (mScreenWidthPixels * (mChildIndexWhenDown + 1) - Math.abs(getScrollX()));
        dx = adjustDeltaXDistance(dx);
        int durationTimeMillis = 200;
        smoothScrollBy((int) (dx + 0.5f), 0, durationTimeMillis);
    }

    /**
     * 计算发生 ACTION_DOWN 事件时, 所触摸到的子View的 index(index从0开始). 该计算值结合手指抬起时一共滑动
     * 的距离值, 可以计算出发生手指抬起后, 页面要进行平滑滑动的方向和滑动距离.
     */
    private void calcFingerTouchedChildIndexWhenActionDown() {
        // 发生 ACTION_DOWN 事件时(即: 有手指按下时), 计算手指按下的点到该ViewGroup的左边框(该左边框可能在屏幕外)的距离
        float fingerToMyLeftBorderDistanceWhenDown = Math.abs(getScrollX()) + Math.abs(mDownRawX);
        mChildIndexWhenDown = (int)(fingerToMyLeftBorderDistanceWhenDown / mScreenWidthPixels);
//        Toast.makeText(getContext(), "down: childIndex = " + mChildIndexWhenDown, Toast.LENGTH_LONG).show();
    }

    /**
     * 判断给定的滑动坐标差所代表的滑动距离是否超过了 touchSlop (即: Android 系统认可这是一次滑动事件时所能
     * 滑动的最小距离)
     * @param deltaDistance 滑动坐标差, 可能为正数, 也可能为负数.
     * @return 给定的滑动坐标差所代表的滑动距离是否超过了 touchSlop. true表示超过了, false表示未超过.
     */
    private boolean isScrollExceedingTouchSlop(float deltaDistance) {
        boolean isScrollExceedingTouchSlop = true;
        // 在水平方向上滑动距离的绝对值
        float deltaDistanceAbs = Math.abs(deltaDistance);
        // 如果滑动距离小于 mTouchSlop, 则什么都不执行.
        if (deltaDistanceAbs < mTouchSlop) {
            // 直接告诉开发者本次滑动未超过touchSlop, 所以不符合滑动要求, 应该舍弃本次滑动,
            // 什么设置都不做, 并且应该直接进入到下一次事件的响应准备中.
            isScrollExceedingTouchSlop = false;
        }
        return isScrollExceedingTouchSlop;
    }

    /**
     * 当手指还未离开屏幕时, 该控件会跟随手指的移动而移动(瞬时移动), 但不能超出左右两个边界(即: 最左边的子View的
     * 左边界和最右边的子View的右边界)
     * @param dx 在x方向上滑动的 deltaX, 注意: 可能为负数.
     * @return 本次滑动是否符合Android系统的滑动要求(即: 滑动距离是否超过了touchSlop). 返回true表示符合该要求;
     *         返回false表示不符合该要求, false 情况下应该直接舍弃本次滑动.
     */
    private void instantScrollWithFinger(float dx) {
        float dxBeforeAdjust = dx;
        dx = adjustDeltaXDistance(dx);
//        Toast.makeText(getContext(), "修正前: dx = " + dxBeforeAdjust + "修正后: dx = " + dx, Toast.LENGTH_SHORT).show();
        scrollBy(-(int) (dx + 0.5f), 0);
    }

    /**
     * 修正要滑动的距离差, 使其数值要保证该ViewGroup的最左边的子View的左边框和最右边的子View的右边框都不能滑入屏幕内.
     * @param dx 要修正的距离差
     * @return 修正以后的距离差
     */
    private float adjustDeltaXDistance(float dx) {
        float scrolledXDistanceAbs = Math.abs(dx);

        int currScrollX = getScrollX();
        // 该ViewGroup在屏幕左边框以外的剩余宽度
        int widthOutOfScreenLeftBorder = Math.abs(currScrollX);
        // 如果是向右滑动并且滑动距离大于了该ViewGroup在屏幕左边框以外的剩余宽度, 则要保证最左边的子View
        // 的左边界不能滑入屏幕内
        if (dx > 0 && widthOutOfScreenLeftBorder >= 0 && scrolledXDistanceAbs > widthOutOfScreenLeftBorder) {
            dx = widthOutOfScreenLeftBorder;
        }

        // 添加上边if的约束条件后, 就保证了 currScrollX 一直 >= 0, 也就是 getScrollX() 一直 >= 0

        // 该ViewGroup在屏幕右边框以外的剩余宽度
        int widthOutOfScreenRightBorder = getWidth() - currScrollX - mScreenWidthPixels;
        // 如果是向左滑动并且滑动距离大于了该ViewGroup在屏幕右边框以外的剩余宽度, 则要保证最右边的子View
        // 的右边界不能滑入屏幕内
        if (dx < 0 && widthOutOfScreenRightBorder >= 0 && scrolledXDistanceAbs > widthOutOfScreenRightBorder) {
            dx = - widthOutOfScreenRightBorder;
        }
        return dx;
    }

    /**
     * 平滑移动
     * @param dx 在x方向上平滑移动的距离
     * @param dy 在y方向上平滑移动的距离
     * @param durationTimeMillis 完成这次平滑移动所需要的时间
     */
    private void smoothScrollBy(int dx, int dy,  int durationTimeMillis){
        int startX = getScrollX();
        int startY = getScrollY();
        // 开始进行平滑移动
        mScroller.startScroll(startX, startY, -dx, -dy, durationTimeMillis);
        // 必须手动要求重绘, 系统才会开始重绘
        safeInvalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            safeInvalidate();
        }
    }

    /**
     * 执行重绘, 在主线程还是子线程中均可调用该重绘方法.
     */
    private void safeInvalidate() {
        if (isMainThread()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    /**
     * 判断执行这句代码的语句所在的线程是不是主线程
     * @return true表示执行这句代码的语句所在的线程是主线程, false表示执行这句代码的语句所在的线程不是主线程.
     */
    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}