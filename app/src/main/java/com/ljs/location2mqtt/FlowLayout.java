package com.ljs.location2mqtt;

import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout  extends ViewGroup {
    private List<List<View>> views; // 存放所有子元素（一行一行存储）
    private List<View> lineViews; // 存储每一行中的子元素
    private List<Integer> heights; // 存储每一行的高度

    private boolean scrollable; // 是否可以滚动
    private int measuredHeight; // 测量得到的高度
    private int realHeight; // 整个流式布局控件的实际高度
    private int scrolledHeight = 0; // 已经滚动过的高度
    private int startY; // 本次滑动开始的Y坐标位置
    private int offsetY; // 本次滑动的偏移量
    private boolean pointerDown; // 在ACTION_MOVE中，视第一次触发为手指按下，从第二次触发开始计入正式滑动

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 初始化
     */
    private void init() {
        views = new ArrayList<>();
        lineViews = new ArrayList<>();
        heights = new ArrayList<>();
    }

    /**
     * 计算布局中所有子元素的宽度和高度，累加得到整个布局最终显示的宽度和高度
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        // 当前行的宽度和高度（宽度是子元素宽度的和，高度是子元素高度的最大值）
        int lineWidth = 0;
        int lineHeight = 0;
        // 整个流式布局最终显示的宽度和高度
        int flowLayoutWidth = 0;
        int flowLayoutHeight = 0;
        // 初始化各种参数（列表）
        init();
        // 遍历所有子元素，对子元素进行排列
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            // 获取到子元素的宽度和高度
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            // 如果当前行中剩余的空间不足以容纳下一个子元素，则换行
            // 换行的同时，保存当前行中的所有元素，叠加行高，然后将行宽和行高重置为0
            if (lineWidth + childWidth + lp.leftMargin + lp.rightMargin > widthSize - getPaddingLeft() - getPaddingRight()) {
                views.add(lineViews);
                lineViews = new ArrayList<>();
                flowLayoutWidth = Math.max(flowLayoutWidth, lineWidth); // 以最宽的行的宽度作为最终布局的宽度
                flowLayoutHeight += lineHeight;
                heights.add(lineHeight);
                lineWidth = 0;
                lineHeight = 0;
            }
            // 无论换不换行，都需要将元素添加到列表中、处理宽度和高度的值
            lineViews.add(child);
            lineWidth += childWidth + lp.leftMargin + lp.rightMargin;
            lineHeight = Math.max(lineHeight, childHeight + lp.topMargin + lp.bottomMargin);
            // 处理最后一行，否则最后一行不能显示
            if (i == childCount - 1) {
                flowLayoutHeight += lineHeight;
                flowLayoutWidth = Math.max(flowLayoutWidth, lineWidth);
                heights.add(lineHeight);
                views.add(lineViews);
            }
        }
        // 得到最终的宽高
        // 宽度：如果是EXACTLY模式，则遵循测量值，否则使用我们计算得到的宽度值
        // 高度：只要布局中内容的高度大于测量高度，就使用内容高度（无视测量模式）；否则才使用测量高度
        int width = widthMode == MeasureSpec.EXACTLY ? widthSize : flowLayoutWidth + getPaddingLeft() + getPaddingRight();
        realHeight = flowLayoutHeight + getPaddingTop() + getPaddingBottom();
        if (heightMode == MeasureSpec.EXACTLY) {
            realHeight = Math.max(measuredHeight, realHeight);
        }
        scrollable = realHeight > measuredHeight;
        // 设置最终的宽高
        setMeasuredDimension(width, realHeight);
    }

    /**
     * 对所有子元素进行布局
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // 当前子元素应该布局到的X、Y坐标
        int currentX = getPaddingLeft();
        int currentY = getPaddingTop();
        // 遍历所有子元素，对每个子元素进行布局
        // 遍历每一行
        for (int i = 0; i < views.size(); i++) {
            int lineHeight = heights.get(i);
            List<View> lineViews = views.get(i);
            // 遍历当前行中的每一个子元素
            for (int j = 0; j < lineViews.size(); j++) {
                View child = lineViews.get(j);
                // 获取到当前子元素的上、下、左、右的margin值
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int childL = currentX + lp.leftMargin;
                int childT = currentY + lp.topMargin;
                int childR = childL + child.getMeasuredWidth();
                int childB = childT + child.getMeasuredHeight();
                // 对当前子元素进行布局
                child.layout(childL, childT, childR, childB);
                // 更新下一个元素要布局的X、Y坐标
                currentX += lp.leftMargin + child.getMeasuredWidth() + lp.rightMargin;
            }
            currentY += lineHeight;
            currentX = getPaddingLeft();
        }
    }

    /**
     * 滚动事件的处理，当布局可以滚动（内容高度大于测量高度）时，对手势操作进行处理
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 只有当布局可以滚动的时候（内容高度大于测量高度的时候），才会对手势操作进行处理
        if (scrollable) {
            int currY = (int) event.getY();
            switch (event.getAction()) {
                // 因为ACTION_DOWN手势可能是为了点击布局中的某个子元素，因此在onInterceptTouchEvent()方法中没有拦截这个手势
                // 因此，在这个事件中不能获取到startY，也因此才将startY的获取移动到第一次滚动的时候进行
                case MotionEvent.ACTION_DOWN:
                    break;
                // 当第一次触发ACTION_MOVE事件时，视为手指按下；以后的ACTION_MOVE事件才视为滚动事件
                case MotionEvent.ACTION_MOVE:
                    // 用pointerDown标志位只是手指是否已经按下
                    if (!pointerDown) {
                        startY = currY;
                        pointerDown = true;
                    } else {
                        offsetY = startY - currY; // 下滑大于0
                        // 布局中的内容跟随手指的滚动而滚动
                        // 用scrolledHeight记录以前的滚动事件中滚动过的高度（因为不一定每一次滚动都是从布局的最顶端开始的）
                        this.scrollTo(0, scrolledHeight + offsetY);
                    }
                    break;
                // 手指抬起时，更新scrolledHeight的值；
                // 如果滚动过界（滚动到高于布局最顶端或低于布局最低端的时候），设置滚动回到布局的边界处
                case MotionEvent.ACTION_UP:
                    scrolledHeight += offsetY;
                    if (scrolledHeight + offsetY < 0) {
                        this.scrollTo(0, 0);
                        scrolledHeight = 0;
                    } else if (scrolledHeight + offsetY + measuredHeight > realHeight) {
                        this.scrollTo(0, realHeight - measuredHeight);
                        scrolledHeight = realHeight - measuredHeight;
                    }
                    // 手指抬起后别忘了重置这个标志位
                    pointerDown = false;
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 调用在这个布局中的子元素对象的getLayoutParams()方法，会得到一个MarginLayoutParams对象
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    /**
     * 事件拦截，当手指按下或抬起的时候不进行拦截（因为可能这个操作只是点击了布局中的某个子元素）；
     * 当手指移动的时候，才将事件拦截；
     * 因此，我们在onTouchEvent()方法中，只能将ACTION_MOVE的第一次触发作为手指按下
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted = false;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                intercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                intercepted = true;
                break;
            case MotionEvent.ACTION_UP:
                intercepted = false;
                break;
        }
        return intercepted;
    }
}
