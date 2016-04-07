package com.clevergump.my_viewpager_demo.widget;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
import android.widget.Toast;

import com.clevergump.my_viewpager_demo.utils.DensityUtils;

/**
 * 添加了在手指离开屏幕后该控件会自动平滑地滑动到某一位置的功能. 当手指抬起时已滑动的距离大于屏幕宽度的一半时,
 * 就会继续完成后续滑动. 如果已滑动的距离不足屏幕宽度的一半, 那么会回退到原先的位置. 并且以上这些后续的滑动
 * 都是平滑滑动而非瞬时滑动.
 *
 * 但 bug是: 虽然在 ACTION_MOVE 的过程中, 该控件的左右边界不会滑入屏幕内, 但是在 ACTION_UP 以后, 会存在这种
 * 情况. 所以需要对 ACTION_UP 以后该控件自动滑动的距离也添加限制条件, 保证左右边界依然不能滑入屏幕内.
 * 该bug将会在下一个版本 {@link MyViewPager4}中得到修复. 另外, 还有待完善的细节是: 图片被显著拉伸, 需保持
 * 一定范围内的宽高比.
 *
 * @author zhangzhiyi
 * @version 1.0
 * @createTime 2016/4/6 11:31
 * @projectName MyViewPagerDemo
 */
public class MyViewPager3 extends ViewGroup {

    private int mScreenWidthPixels;
    private int mScreenHeightPixels;
    private int mChildCount;
    private int mNonGoneChildCount;
    // 上一次发生滑动事件后, 手指停止处的x坐标. 对于滑动距离小于mTouchSlop的滑动, 将不会更新该变量的数值.
    private float mLastScrolledRawX;
    private int mTouchSlop;
    private Scroller mScroller;
    // 发生 ACTION_DOWN 时的 rawX.
    private float mDownRawX;
    // 发生 ACTION_DOWN 时的 scrollX.
    private int mDownScrollX;
    // 发生 ACTION_DOWN 时所触摸的子 View的 index (从0开始).
    private int mChildIndexWhenDown;


    public MyViewPager3(Context context) {
        this(context, null);
    }

    public MyViewPager3(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyViewPager3(Context context, AttributeSet attrs, int defStyleAttr) {
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
    public boolean onTouchEvent(MotionEvent event) {
        float currRawX = event.getRawX();
        // 本次事件和上次成功发生移动的事件相比, 在x方向上移动的位置坐标差.
        float dx = currRawX - mLastScrolledRawX;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 如果上一次的平滑移动还没有结束, 那么就让其立即结束, 立即移动到目标位置
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mDownRawX = currRawX;
                calcChildIndexWhenActionDown();
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
                // 从手指按下到抬起的整个过程中, 在x方向上移动的位置坐标差.
                float downToUpDx = currRawX - mDownRawX;
                // 屏幕宽度的一半
                int halfScreenWidth = mScreenWidthPixels >> 1;
                // 如果手指离开时, 总体来看是手指向左滑动, 并且滑动的距离大于屏幕宽度的一半时, 就继续平滑移动到
                // 下一个子View
                if (downToUpDx < 0 && Math.abs(downToUpDx) >= halfScreenWidth) {
                    smoothScrollToNextChild();
                }
                // 如果手指离开时, 总体来看是手指向右滑动, 并且滑动的距离大于屏幕宽度的一半时, 就继续平滑移动到
                // 下一个子View
                else if (downToUpDx > 0 && Math.abs(downToUpDx) >= halfScreenWidth) {
                    smoothScrollToPreviousChild();
                }
                // 如果手指离开时, 不论是向哪个方向滑动, 只要滑动的距离小于屏幕宽度的一半, 就会平滑移动回到最开始的位置
                else {
                    smoothScrollBack();
                }
                break;
        }
        mLastScrolledRawX = currRawX;
        return true;
    }

    /**
     * 平滑移动回到原先的位置(手指按下前, 该控件的位置)
     */
    private void smoothScrollBack() {
        float dx = Math.abs(getScrollX()) - mScreenWidthPixels * mChildIndexWhenDown;
        int durationTimeMillis = 200;
        smoothScrollBy((int) (dx + 0.5f), 0, durationTimeMillis);
    }

    /**
     * 平滑移动到上一个子View
     */
    private void smoothScrollToPreviousChild() {
        // 说明当前的子View已经随手指向右移动了一大半的距离(该距离超过了整个屏幕宽度的一半)了, 那么只需要继续
        // 向右移动剩余的一小半距离(即: 整个屏幕宽度减去已经移动过的那一大半距离后剩下的距离)即可.
        float dx = Math.abs(getScrollX()) - mScreenWidthPixels * (mChildIndexWhenDown - 1);
        int durationTimeMillis = 200;
        smoothScrollBy((int) (dx + 0.5f), 0, durationTimeMillis);
    }

    /**
     * 平滑移动到下一个子View
     */
    private void smoothScrollToNextChild() {
        // 说明当前的子View已经随手指向左移动了一大半的距离(该距离超过了整个屏幕宽度的一半)了, 那么只需要继续
        // 向左移动剩余的一小半距离(即: 整个屏幕宽度减去已经移动过的那一大半距离后剩下的距离)即可.
        float dx = - (mScreenWidthPixels * (mChildIndexWhenDown + 1) - Math.abs(getScrollX()));
        int durationTimeMillis = 200;
        smoothScrollBy((int) (dx + 0.5f), 0, durationTimeMillis);
    }

    /**
     * 计算发生 ACTION_DOWN 事件时, 所触摸到的子View的 index(index从0开始). 该计算值结合手指抬起时一共滑动
     * 的距离值, 可以计算出发生手指抬起后, 页面要进行平滑滑动的方向和滑动距离.
     */
    private void calcChildIndexWhenActionDown() {
        // 发生 ACTION_DOWN 事件时(即: 有手指按下时), 计算手指按下的点到该ViewGroup的左边框(该左边框可能在屏幕外)的距离
        float fingerToMyLeftBorderDistanceWhenDown = Math.abs(getScrollX()) + Math.abs(mDownRawX);
        mChildIndexWhenDown = (int)(fingerToMyLeftBorderDistanceWhenDown / mScreenWidthPixels);
        Toast.makeText(getContext(), "down: childIndex = " + mChildIndexWhenDown, Toast.LENGTH_LONG).show();
    }

    /**
     * 判断给定的滑动坐标差所代表的滑动距离是否超过了 touchSlop (即: Android 系统认可这是一次滑动事件所能
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
     *         返回false表示不符合该要求, 这种情况下应该直接舍弃本次滑动.
     */
    private void instantScrollWithFinger(float dx) {
        dx = adjustDeltaXDistance(dx);
        scrollBy(-(int)(dx + 0.5f), 0);
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
     * @param dx
     * @param dy
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
     * @return
     */
    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}