package com.clevergump.my_viewpager_demo.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.clevergump.my_viewpager_demo.utils.DensityUtils;

/**
 * 版本2. 只能随手指的滑动而瞬间滑动, 并且能保证该ViewGroup的左右两个边框不会滑入屏幕内, 没有更多其他的功能.
 * 下一个版本, 会进行重构, 将 {@link #onTouchEvent(MotionEvent)}方法中 ACTION_MOVE 情况下的
 * 滑动逻辑抽取成一个单独的方法 {@link RefactoredMyViewPager2#instantScrollWithFinger(float)}, 具体请见
 * {@link RefactoredMyViewPager2}.
 *
 * @author zhangzhiyi
 * @version 1.0
 * @createTime 2016/4/6 11:31
 * @projectName MyViewPagerDemo
 */
public class MyViewPager2 extends ViewGroup {

    private int mScreenWidthPixels;
    private int mScreenHeightPixels;
    private int mChildCount;
    private int mNonGoneChildCount;
    // 上一次发生滑动事件后, 手指停止处的x坐标. 对于滑动距离小于mTouchSlop的滑动, 将不会更新该变量的数值.
    private float mLastScrolledRawX;
    private int mTouchSlop;

    public MyViewPager2(Context context) {
        this(context, null);
    }

    public MyViewPager2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyViewPager2(Context context, AttributeSet attrs, int defStyleAttr) {
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
                // 在水平方向上滑动的距离.
                float scrolledXDistance = Math.abs(dx);
                // 如果滑动距离小于 mTouchSlop, 则什么都不执行.
                if (scrolledXDistance < mTouchSlop) {
                    // 不更新 mLastScrolledRawX 的值, 直接返回true从而可以继续进入下一次touch事件.
                    return true;
                }

                // 限定滑动时左右两个边界的滑动距离, 保证最左边的子View的左边界和最右边的子View的右边界都不能滑入屏幕内.

                int currScrollX = getScrollX();
                // 该ViewGroup在屏幕左边框以外的剩余宽度
                int widthOutOfScreenLeftBorder = Math.abs(currScrollX);
                // 如果是向右滑动并且滑动距离大于了该ViewGroup在屏幕左边框以外的剩余宽度, 则要保证最左边的子View的左边界不能滑入屏幕内
                if (dx > 0 && widthOutOfScreenLeftBorder >= 0 && scrolledXDistance > widthOutOfScreenLeftBorder) {
                    dx = widthOutOfScreenLeftBorder;
                }

                // 添加上边if的约束条件后, 就保证了 currScrollX 一直 >= 0, 也就是 getScrollX() 一直 >= 0

                // 该ViewGroup在屏幕右边框以外的剩余宽度
                int widthOutOfScreenRightBorder = getWidth() - currScrollX - mScreenWidthPixels;
                // 如果是向左滑动并且滑动距离大于了该ViewGroup在屏幕右边框以外的剩余宽度, 则要保证最右边的子View的右边界不能滑入屏幕内
                if (dx < 0 && widthOutOfScreenRightBorder >= 0 && scrolledXDistance > widthOutOfScreenRightBorder) {
                    dx = - widthOutOfScreenRightBorder;
                }

                scrollBy((int) -dx, 0);
                break;
        }
        mLastScrolledRawX = currRawX;
        return true;
    }
}