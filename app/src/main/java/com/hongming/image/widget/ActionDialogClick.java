package com.hongming.image.widget;

import android.view.View;

public interface ActionDialogClick {

    /**
     * 拍照
     * @param view
     */
    void onTakePhotoClick(View view);

    /**
     * 照片
     * @param view
     */
    void onPickImageClick(View view);

    /**
     * 删除操作
     * @param view
     */
    void onDeleteClick(View view);

}
