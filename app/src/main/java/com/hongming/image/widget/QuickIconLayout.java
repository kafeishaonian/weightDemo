package com.hongming.image.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuickIconLayout extends LinearLayout {

    private static final int ACTION_ICON_ANIMATION_START = 0x0000000001;
    private static final int ACTION_ICON_ANIMATION_END = 0x0000000002;

    private int sizeWidth;
    private int sizeHeight;
    private View childAt;
    private View childAtBackUp;
    private int measuredWidth;
    private int measuredHeight;
    private Point point1;
    private List<Point> list = new ArrayList<>();
    private int position = 0;
    private View mIconView;
    private int iconCircleX = 0;
    private int iconCircleY = 0;
    private int iconLeftTopX = 0;
    private int iconLeftTopY = 0;
    private int iconMeasureWidth = 0;
    private int iconMeasureHeight = 0;
    private Animation animation;
    private int roundRadius = dp2px(50);
    private InnerHandler innerHandler;
    private int mChildCount;
    private boolean isOnceWaitFlag = true;
    private int iconCount = 1;
    private Context mContext;

    public QuickIconLayout(Context context) {
        super(context);
        mContext = context;
        innerHandler = new InnerHandler(context);
    }

    public QuickIconLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        innerHandler = new InnerHandler(context);
    }

    public QuickIconLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        innerHandler = new InnerHandler(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public QuickIconLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        innerHandler = new InnerHandler(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        /**
         * 获得此ViewGroup上级容器为其推荐的宽和高，以及计算模式
         */
        sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        // 计算出所有的childView的宽和高
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }


    public void setPaintView(View view) {
        mIconView = view;
        mIconView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                iconMeasureWidth = mIconView.getMeasuredWidth();
                iconMeasureHeight = mIconView.getMeasuredHeight();
                iconCircleX = sizeWidth / 2;
                iconCircleY = sizeHeight / 2;
                iconLeftTopX = iconCircleX - iconMeasureWidth / 2;
                iconLeftTopY = iconCircleY - iconMeasureHeight / 2;
            }
        });
    }

    public void startAnimation() {
        //得到子View总的个数
        mChildCount = getChildCount();

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isOnceWaitFlag) {
                    isOnceWaitFlag = false;
                    innerHandler.removeCallbacksAndMessages(null);
                    innerHandler.sendMessageDelayed(innerHandler.obtainMessage(ACTION_ICON_ANIMATION_START), 1500);
                }
            }
        });
    }

    class InnerHandler extends Handler {
        private WeakReference<Context> mContextReference;

        public InnerHandler(Context context) {
            mContextReference = new WeakReference<Context>(context);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Context context = mContextReference.get();
            if (context == null) {
                return;
            }
            switch (msg.what) {
                case ACTION_ICON_ANIMATION_START:
                    setViewChild(position++);
                    if (position >= mChildCount) {
                        position = 0;
                    }
                    break;
                case ACTION_ICON_ANIMATION_END:
                    removeViewChild();
                    break;
            }
        }
    }

    public void stopAnimation() {
        innerHandler.removeCallbacksAndMessages(null);
        isOnceWaitFlag = true;
        if (list != null) {
            list.clear();
        }
        if (childAt != null) {
            childAt.layout(0, 0, 0, 0);
            childAt = null;
        }
        if (childAtBackUp != null) {
            childAtBackUp.layout(0, 0, 0, 0);
            childAtBackUp = null;
        }
        removeViewChild();
    }


    private void removeViewChild() {
        if (list != null && list.size() >= 2) {
            list.remove(0);
        }
        if (childAtBackUp != null) {
            childAtBackUp.layout(0, 0, 0, 0);
        }
    }

    private void setViewChild(int i) {
        try {
            childAtBackUp = childAt;
            childAt = getChildAt(i);
            measuredWidth = childAt.getMeasuredWidth();
            measuredHeight = childAt.getMeasuredHeight();
            //取 X Y坐标
            setXY(i);
        } catch (Exception e) {
            Log.d("TAG", e.getMessage());
            innerHandler.removeCallbacksAndMessages(null);
            innerHandler.sendMessageDelayed(innerHandler.obtainMessage(ACTION_ICON_ANIMATION_START), 1800);
        }
    }

    private void setImageResource(Random random) {
        try {
            int position = random.nextInt(36);
            while (position == 0) {
                position = random.nextInt(36);
            }
            String positionStr = String.valueOf(position);
            ((ImageView) childAt).setImageResource(mContext.getResources().getIdentifier("haya_match_female_" + positionStr,
                    "mipmap", mContext.getPackageName()));
        } catch (Exception e) {
            Log.d("TAG", e.getMessage());
        }
    }


    private void setXY(int i) {
        try {
            //随机取两个数 作为每一个子View的左上角坐标   最大为父控件的X Y 最小为0
            //random.nextInt(max) % (max - min + 1) + min;
            Random random = new Random();
            setImageResource(random);
            int x = random.nextInt(sizeWidth - measuredWidth) % ((sizeWidth - measuredWidth) - 1 + 1) + 1;
            int y = random.nextInt(sizeHeight - measuredHeight) % ((sizeHeight - measuredHeight) - 1 + 1) + 1;
            point1 = new Point(x, y);

            while (twototwo(point1)) { //判断有没满足重叠的部分，如果有就返回true 重新创建点 去比较
                int x2 = random.nextInt(sizeWidth - measuredWidth) % ((sizeWidth - measuredWidth) - 1 + 1) + 1;
                int y2 = random.nextInt(sizeHeight - measuredHeight) % ((sizeHeight - measuredHeight) - 1 + 1) + 1;
                point1 = new Point(x2, y2);
            }

            if (list.size() == getChildCount()) {
                return;
            }
            //把没有重合的点放到集合数据中去
            list.add(point1);
            //摆放每一个view的位置
            childAt.layout(point1.x, point1.y, point1.x + measuredWidth, point1.y + measuredHeight);
            animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(1500);
            animation.setRepeatCount(1);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatMode(ValueAnimator.REVERSE);
            childAt.startAnimation(animation);
            innerHandler.sendMessageDelayed(innerHandler.obtainMessage(ACTION_ICON_ANIMATION_START), 2800);
            innerHandler.sendMessageDelayed(innerHandler.obtainMessage(ACTION_ICON_ANIMATION_END), 3000);
        } catch (Exception e) {
            Log.d("TAG", e.getMessage());
            innerHandler.removeCallbacksAndMessages(null);
            innerHandler.sendMessageDelayed(innerHandler.obtainMessage(ACTION_ICON_ANIMATION_START), 1800);
        }
    }

    private boolean twototwo(Point point) {
        for (int i = 0; i < list.size(); i++) {
            Point poition = list.get(i);
            int x1 = poition.x;
            int y1 = poition.y;

            int x2 = point.x; //新的
            int y2 = point.y;

            if (Math.abs(x2 - iconLeftTopX) <= iconMeasureWidth + 20 && Math.abs(y2 - iconLeftTopY) <= iconMeasureHeight + 20) {
                return true;
            }
            if (Math.abs(x2 - x1) <= measuredWidth && Math.abs(y2 - y1) <= measuredHeight) {
                return true;
            }

            if (roundRadius > x2 && roundRadius > y2) {
                return true;
            }
            if ((sizeWidth - roundRadius * 2) < x2 && roundRadius > y2) {
                return true;
            }
            if (roundRadius > x2 && (sizeHeight - roundRadius * 2) < y2) {
                return true;
            }
            if ((sizeWidth - roundRadius * 2) < x2 && (sizeHeight - roundRadius * 2) < y2) {
                return true;
            }
        }
        return false;
    }

    private static int dp2px(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
