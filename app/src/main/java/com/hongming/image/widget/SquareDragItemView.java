package com.hongming.image.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.hongming.image.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 头像动图拖拽Item
 */
public class SquareDragItemView extends FrameLayout implements ActionDialogClick {

    public static final int SCALE_LEVEL_1 = 1; // 最大状态，缩放比例是100%
    public static final int SCALE_LEVEL_2 = 2; // 中间状态，缩放比例scaleRate
    public static final int SCALE_LEVEL_3 = 3; // 最小状态，缩放比例是s

    private Context mContext;
    private SimpleDraweeView imageView;
    private ImageView addView;
    private View maskView;
    private SquareDragView parentView;
    private Listener listener;

    private boolean hasSetCurrentSpringValue = false;
    //序号 取这个值
    private int status;
    private float scaleRate = 0.49f;
    private Spring springX;
    private Spring springY;
    private String imagePath;
    private float smallerRate = scaleRate * 0.8f;
    private ObjectAnimator scaleAnimator;
    private SpringConfig springConfigCommon = SpringConfig.fromOrigamiTensionAndFriction(140, 140);
    private int moveDstX = Integer.MIN_VALUE, moveDstY = Integer.MIN_VALUE;
    private TextView tvStatus;
    private ImageUploadStatus uploadStatusHolder=null;

    private boolean isFlag = false;

    public SquareDragItemView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SquareDragItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SquareDragItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SquareDragItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        inflate(mContext, R.layout.view_square_drag_item, this);
        uploadStatusHolder = new AvatarChangeHolder(this,null);
        tvStatus = findViewById(R.id.tvStatus);
        imageView = findViewById(R.id.drag_item_imageview);
        addView = findViewById(R.id.add_view);
        maskView = findViewById(R.id.drag_item_mask_view);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!hasSetCurrentSpringValue) {
                    adjustImageView();
                    hasSetCurrentSpringValue = true;
                }
            }
        });

        maskView.setOnClickListener(v -> {
            //判断是否有图片，没有图片添加图片，有图片替换图片，调用Dialog弹窗
            if (!isDraggable()) {
                if (parentView.getActionDialog() == null) {
                    SquareDragDialog dialog = new SquareDragDialog(getContext());
                    dialog.setActionDialogClick(SquareDragItemView.this)
                            .setShowDeleteButton(false)
                            .show();
                } else {
                    parentView.getActionDialog()
                            .setActionDialogClick(SquareDragItemView.this)
                            .setShowDeleteButton(false)
                            .show();
                }
            } else {
                boolean showDelete = true;
                if (parentView.getImageUrls().size() <= 1){
                    showDelete = false;
                } else {
                    String value = parentView.getImageUrls().valueAt(1);
                    if (TextUtils.isEmpty(value)){
                        showDelete = false;
                    }
                }

                if (parentView.getActionDialog() == null) {
                    SquareDragDialog dialog = new SquareDragDialog(getContext());
                    dialog.setActionDialogClick(SquareDragItemView.this)
                            .setShowDeleteButton(showDelete)
                            .show();
                } else {
                    parentView.getActionDialog().setActionDialogClick(SquareDragItemView.this);
                    parentView.getActionDialog()
                            .setActionDialogClick(SquareDragItemView.this)
                            .setShowDeleteButton(showDelete)
                            .show();
                }
            }
        });
        initSpring();
    }

    public ImageUploadStatus getUploadStatusHolder(){
        return uploadStatusHolder;
    }

    @Override
    public void onTakePhotoClick(View view) {
        if (listener != null) listener.takePhoto(status, isDraggable());
    }

    @Override
    public void onPickImageClick(View view) {
        if (listener != null) listener.pickImage(status, isDraggable());

    }

    @Override
    public void onDeleteClick(View view) {
        imageView.setImageBitmap(null);
        addView.setVisibility(View.VISIBLE);
        parentView.onDeleteImage(SquareDragItemView.this);
        tvStatus.setVisibility(View.GONE);
        if (uploadStatusHolder instanceof  AvatarChangeHolder){
            ((AvatarChangeHolder) uploadStatusHolder).setKey("");
            ((AvatarChangeHolder) uploadStatusHolder).clearStatus();
        }
        imagePath = null;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void pickImage(int imageStatus, boolean isModify);

        void takePhoto(int imageStatus, boolean isModify);
    }

    /**
     * 初始化Spring相关
     */
    private void initSpring() {
        SpringSystem mSpringSystem = SpringSystem.create();
        springX = mSpringSystem.createSpring();
        springY = mSpringSystem.createSpring();

        springX.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int xPos = (int) spring.getCurrentValue();
                setScreenX(xPos);
            }
        });

        springY.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int yPos = (int) spring.getCurrentValue();
                setScreenY(yPos);
            }
        });

//        springX.setSpringConfig(springConfigCommon);
//        springY.setSpringConfig(springConfigCommon);
    }

    /**
     * 调整ImageView的宽度和高度各为FrameLayout的一半
     */
    private void adjustImageView() {
        if (!isFlag && status != SquareDragIndexKt.STATUS_LEFT_TOP) {
            imageView.setScaleX(scaleRate);
            imageView.setScaleY(scaleRate);

            maskView.setScaleX(scaleRate);
            maskView.setScaleY(scaleRate);
        }

        setCurrentSpringPos(getLeft(), getTop());
    }

    public void setScaleRate(float scaleRate) {
        this.scaleRate = scaleRate;
        this.smallerRate = scaleRate * 0.8f;
    }

    /**
     * 从一个状态切换到另一个状态
     */
    public void switchPosition(int toStatus) {
        if (this.status == toStatus) {
            throw new RuntimeException("program insanity");
        }
        if (isFlag) {
            scaleSize(SCALE_LEVEL_1);
        } else {
            if (toStatus == SquareDragIndexKt.STATUS_LEFT_TOP) {
                scaleSize(SCALE_LEVEL_1);
            } else if (this.status == SquareDragIndexKt.STATUS_LEFT_TOP) {
                scaleSize(SCALE_LEVEL_2);
            }
        }

        this.status = toStatus;
        Point point = parentView.getOriginViewPos(status);
        this.moveDstX = point.x;
        this.moveDstY = point.y;
        animTo(moveDstX, moveDstY);
    }

    public void animTo(int xPos, int yPos) {
        springX.setEndValue(xPos);
        springY.setEndValue(yPos);
    }

    /**
     * 设置缩放大小
     */
    public void scaleSize(int scaleLevel) {
        float rate = scaleRate;
        if (scaleLevel == SCALE_LEVEL_1) {
//            rate = 0.98f;
            rate = 1f;
        } else if (scaleLevel == SCALE_LEVEL_3) {
            rate = smallerRate;
        }

        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }

        scaleAnimator = ObjectAnimator
                .ofFloat(this, "custScale", imageView.getScaleX(), rate)
                .setDuration(200);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.start();
    }

    public void saveAnchorInfo(int downX, int downY) {
        int halfSide = getMeasuredWidth() / 2;
        moveDstX = downX - halfSide;
        moveDstY = downY - halfSide;
    }

    /**
     * 真正开始动画
     */
    public void startAnchorAnimation() {
        if (moveDstX == Integer.MIN_VALUE || moveDstX == Integer.MIN_VALUE) {
            return;
        }

        springX.setOvershootClampingEnabled(true);
        springY.setOvershootClampingEnabled(true);
        animTo(moveDstX, moveDstY);
        scaleSize(SquareDragItemView.SCALE_LEVEL_3);
    }

    public void setScreenX(int screenX) {
        this.offsetLeftAndRight(screenX - getLeft());
    }

    public void setScreenY(int screenY) {
        Log.d("", "setScreenY,offset="+(screenY - getTop()));
        Log.d("", "setScreenY,screenY="+screenY+",getTop="+getTop());
        this.offsetTopAndBottom(screenY - getTop());
    }

    public int computeDraggingX(int dx) {
        this.moveDstX += dx;
        return this.moveDstX;
    }

    public int computeDraggingY(int dy) {
        this.moveDstY += dy;
        return this.moveDstY;
    }

    /**
     * 设置当前spring位置
     */
    private void setCurrentSpringPos(int xPos, int yPos) {
        springX.setCurrentValue(xPos);
        springY.setCurrentValue(yPos);
    }

    public void setStatus(int status) {
        this.status = status;

        try {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#F1F1F1"));
            gd.setCornerRadius(PixelUtils.dip2px(getContext(),10));
            gd.setShape(GradientDrawable.RECTANGLE);
            imageView.setBackground(gd);
        }catch (Exception e){
            Log.d("", e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }

    public void setParentView(SquareDragView parentView) {
        this.parentView = parentView;
    }

    public void onDragRelease() {
        if (isFlag) {
            scaleSize(SquareDragItemView.SCALE_LEVEL_1);
        } else {
            if (status == SquareDragIndexKt.STATUS_LEFT_TOP) {
                scaleSize(SquareDragItemView.SCALE_LEVEL_1);
            } else {
                scaleSize(SquareDragItemView.SCALE_LEVEL_2);
            }
        }

        springX.setOvershootClampingEnabled(false);
        springY.setOvershootClampingEnabled(false);
//        springX.setSpringConfig(springConfigCommon);
//        springY.setSpringConfig(springConfigCommon);

        Point point = parentView.getOriginViewPos(status);
        setCurrentSpringPos(getLeft(), getTop());
        this.moveDstX = point.x;
        this.moveDstY = point.y;
        animTo(moveDstX, moveDstY);
    }

    public void fillImageView(String imagePath) {
        this.imagePath = imagePath;
        addView.setVisibility(View.GONE);
        loadPhoto(imagePath);
    }

    private void loadPhoto(final String avatar) {
        Log.d("","头像uri,show,"+avatar);
        updateIdentifyStatus();
        if (uploadStatusHolder instanceof  AvatarChangeHolder){
            ((AvatarChangeHolder) uploadStatusHolder).setKey(avatar.replace("file://", ""));
        }
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setUri(avatar)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        imageView.setImageURI(Uri.parse(avatar));
                    }
                })
                .build();
        imageView.setController(controller);
    }

    public void updateIdentifyStatus(){
        tvStatus.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(imagePath)) {
            List<String> userAvatars = new ArrayList<>();
            if (userAvatars != null && userAvatars.size() > 0){
                for (String info:userAvatars){
                    if (imagePath.equals(info)){
                        //0：审核通过、1：审核中、2：审核失败
                        if (!info.equals("")){
                            GradientDrawable gd = new GradientDrawable();
                            gd.setColor(Color.parseColor("#60000000"));
                            gd.setCornerRadius(PixelUtils.dip2px(getContext(),24));
                            tvStatus.setBackground(gd);
                            tvStatus.setVisibility(View.VISIBLE);
                            tvStatus.setText("");
                        }

                        break;
                    }
                }
            }
        }
    }


    // 以下两个get、set方法是为自定义的属性动画CustScale服务，不能删
    public void setCustScale(float scale) {
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);

        maskView.setScaleX(scale);
        maskView.setScaleY(scale);
    }

    public float getCustScale() {
        return imageView.getScaleX();
    }

    public void updateEndSpringX(int dx) {
        springX.setEndValue(springX.getEndValue() + dx);
    }

    public void updateEndSpringY(int dy) {
        springY.setEndValue(springY.getEndValue() + dy);
    }

    public boolean isDraggable() {
        return imagePath != null;
    }

    public String getImagePath() {
        return imagePath;
    }

}
