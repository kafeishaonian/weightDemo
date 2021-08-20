package com.hongming.image.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;


public class SquareNumberView extends RelativeLayout {

    public SquareNumberView(Context context) {
        super(context);
    }

    public SquareNumberView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareNumberView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SquareNumberView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        int size = PixelUtils.dip2px(getContext(),20f);
        for (int index = 0;index<6;index++){
            TextView textView = new TextView(getContext());
            textView.setLayoutParams(new RelativeLayout.LayoutParams(size,size));
            textView.setTextColor(Color.parseColor("#ffffff"));
            textView.setTextSize(12);
            textView.setGravity(Gravity.CENTER);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(Color.parseColor("#32000000"));
            textView.setBackground(gd);
            textView.setText((index+1)+"");
            addView(textView);
        }
    }

    public void updateLayout(int index,int left,int top,int right,int bottom){
        View child = getChildAt(index);
        int size = PixelUtils.dip2px(getContext(),20f);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) child.getLayoutParams();
        if (layoutParams==null){
            layoutParams = new RelativeLayout.LayoutParams(size,size);
        }
        layoutParams.setMargins(left,top,0,0);
//        if (index == 0){
//        }else if (index == 1){
//            layoutParams.setMargins(right-size,bottom-size,0 ,0);
//        }


        child.setLayoutParams(layoutParams);
    }
}
