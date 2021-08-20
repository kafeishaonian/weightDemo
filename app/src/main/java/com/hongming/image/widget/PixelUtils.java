package com.hongming.image.widget;

import android.content.Context;
import android.content.res.Resources;

public class PixelUtils {

    /**
     * dp2px
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
