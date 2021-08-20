package com.hongming.image.abc;

import android.content.Intent;
import android.net.Uri;
import android.util.SparseArray;


public interface DraggablePresenter {

    void onActivityResult(int requestCode, int resultCode, Intent result);

    void beginCrop(Uri source);

    void handleCrop(int resultCode, Intent result);

    SparseArray<String> getImageUrls();

    void setImages(String... imageUrls);

    void setCustomActionDialog(ActionDialog actionDialog);
}