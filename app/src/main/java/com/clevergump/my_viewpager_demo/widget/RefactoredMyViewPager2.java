package com.clevergump.my_viewpager_demo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.clevergump.my_viewpager_demo.utils.DensityUtils;

/**
 * 对 {@link MyViewPager2} 的重构. 将 {@link #onTouchEvent(MotionEvent)}方法中 ACTION_MOVE 情况下的
 * 滑动逻辑抽取成一个单独的方法 {@link #instantScrollWithFinger(float)}, 该方法的含义是让该控件随手指
 * 瞬间移动, 便于接下来的代码编写 (接下来要编写手指离开屏幕后, 也就是在 ACTION_UP 情况下, 该控件的平滑滑动,
 * 并且需要智能判断到底是向左还是向右滑动, 当然这些都是下一个版本, 也就是 {@link MyViewPager3} 要做的事情).
 *
 * 下一个版本, 将会添加在手指离开屏幕后该控件会自动平滑地滑动到某一位置的功能, 具体请见 {@link MyViewPager3}
 *
 * @author zhangzhiyi
 * @version 1.0
 * @createTime 2016/4/6 11:31
 * @projectName MyViewPagerDemo
 */
public class RefactoredMyViewPager2 extends ViewGroup {

    private int mScreenWidthPixels;
    private int mScreenHeightPixels;
    private int mChildCount;
    private int mNonGoneChildCount;
    // 上一次发生滑动事件后, 手指停止处的x坐标. 对于滑动距离小于mTouchSlop的滑动, 将不会更新该变量的数值.
    private float mLastScrolledRawX;
    private int mTouchSlop;

    public RefactoredMyViewPager2(Context context) {
        this(context, null);
    }

    public RefactoredMyViewPager2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefactoredMyViewPager2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mScreenWidthPixels = DensityUtils.getScreenWidthPixels(context);
        mScreenHeightPixels = DensityUtils.getScreenHeightPixels(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
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
    public boolean onTouchEvent(MotionEvent event) {
        float currRawX = event.getRawX();
        float dx = currRawX - mLastScrolledRawX;

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                // 如果本次滑动距离小于 mTouchSlop, 则什么都不执行, 也不会更新 mLastScrolledRawX 的数值,
                // 而是直接返回 true 表示消费掉了本次事件, 而直接进入到下一次事件的响应准备中.
                if (!scrollExceedsTouchSlop(dx)) {
                    return true;
                }
                instantScrollWithFinger(dx);
                break;
        }
        mLastScrolledRawX = currRawX;
        return true;
    }

    /**
     * 判断给定的滑动坐标差所代表的滑动距离是否超过了 touchSlop (即: Android 系统认可这是一次滑动事件所能
     * 滑动的最小距离)
     * @param deltaDistance 滑动坐标差, 可能为正数, 也可能为负数.
     * @return 给定的滑动坐标差所代表的滑动距离是否超过了 touchSlop. true表示超过了, false表示未超过.
     */
    private boolean scrollExceedsTouchSlop(float deltaDistance) {
        boolean scrollExceedsTouchSlop = true;
        // 在水平方向上滑动距离的绝对值
        float deltaDistanceAbs = Math.abs(deltaDistance);
        // 如果滑动距离小于 mTouchSlop, 则什么都不执行.
        if (deltaDistanceAbs < mTouchSlop) {
            // 直接告诉开发者本次滑动未超过touchSlop, 所以不符合滑动要求, 应该舍弃本次滑动,
            // 什么设置都不做, 并且应该直接进入到下一次事件的响应准备中.
            scrollExceedsTouchSlop = false;
        }
        return scrollExceedsTouchSlop;
    }

    /**
     * 当手指还未离开屏幕时, 该控件会跟随手指的移动而移动(瞬时移动), 但不能超出左右两个边界(即: 最左边的子View的
     * 左边界和最右边的子View的右边界)
     * @param dx 在x方向上滑动的 deltaX, 注意: 可能为负数.
     * @return 本次滑动是否符合Android系统的滑动要求(即: 滑动距离是否超过了touchSlop). 返回true表示符合该要求;
     *         返回false表示不符合该要求, 这种情况下应该直接舍弃本次滑动.
     */
    private void instantScrollWithFinger(float dx) {
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

        scrollBy((int) -dx, 0);
    }
}