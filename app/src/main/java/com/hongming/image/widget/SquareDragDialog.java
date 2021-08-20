package com.hongming.image.widget;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.hongming.image.R;

public class SquareDragDialog extends ActionDialog {

    private TextView tvTakeCamera; // 拍照
    private TextView tvChoosePhoto; // 选择相册
    private TextView tvDelete; //删除
    private TextView tvCancel; // 取消

    protected ActionDialogClick actionDialogClick;

    public SquareDragDialog(Context context) {
        super(context);
    }

    protected SquareDragDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    protected SquareDragDialog(Context context, int themeResId) {
        super(context, themeResId);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setGravity(Gravity.BOTTOM);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//        setCanceledOnTouchOutside(false);
        setContentView(R.layout.dialog_square_drag_photo);

        WindowManager.LayoutParams mLayoutParams = getWindow().getAttributes();
        mLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(mLayoutParams);
        initView();
    }

    private void initView() {
        tvTakeCamera = findViewById(R.id.tv_take_camera);
        tvChoosePhoto = findViewById(R.id.tv_choose_photo);
        tvDelete = findViewById(R.id.tv_choose_delete);
        tvCancel = findViewById(R.id.tv_cancel);

        tvTakeCamera.setOnClickListener(view -> {
            if (actionDialogClick != null) actionDialogClick.onTakePhotoClick(view);
            dismiss();
        });
        tvChoosePhoto.setOnClickListener(view -> {
            if (actionDialogClick != null) actionDialogClick.onPickImageClick(view);
            dismiss();
        });
        tvDelete.setOnClickListener(v -> {
            if (actionDialogClick != null) actionDialogClick.onDeleteClick(v);
            dismiss();
        });
        tvCancel.setOnClickListener(view -> {
            dismiss();
        });
    }

    @Override
    public View getDeleteButtonView() {
        return findViewById(R.id.tv_choose_delete);
    }

    public ActionDialog setActionDialogClick(ActionDialogClick actionDialogClick) {
        this.actionDialogClick = actionDialogClick;
        return this;
    }
}
