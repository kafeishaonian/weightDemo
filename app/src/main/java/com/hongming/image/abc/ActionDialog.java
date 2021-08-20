package com.hongming.image.abc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

public abstract class ActionDialog extends AlertDialog implements DialogInterface.OnShowListener {

    protected boolean showDeleteButton;
    protected ActionDialogClick actionDialogClick;

    protected ActionDialog(Context context) {
        super(context);
    }

    protected ActionDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    protected ActionDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOnShowListener(this);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (getDeleteButtonView() == null) return;
        if (showDeleteButton()) {
            getDeleteButtonView().setVisibility(View.VISIBLE);
        } else {
            getDeleteButtonView().setVisibility(View.GONE);
        }
    }

    public abstract View getDeleteButtonView();

    public abstract ActionDialog setActionDialogClick(ActionDialogClick actionDialogClick);

    public boolean showDeleteButton() {
        return showDeleteButton;
    }

    public ActionDialog setShowDeleteButton(boolean showDeleteButton) {
        this.showDeleteButton = showDeleteButton;
        return this;
    }
}
