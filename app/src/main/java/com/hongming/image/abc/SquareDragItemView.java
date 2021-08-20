package com.hongming.image.abc;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.myapplication.SquareDragView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.squareup.picasso.Picasso;

public class SquareDragItemView extends FrameLayout implements ActionDialogClick {

    public static final int STATUS_LEFT_TOP = 0;
    public static final int STATUS_RIGHT_TOP = 1;
    public static final int STATUS_RIGHT_MIDDLE = 2;
    public static final int STATUS_RIGHT_BOTTOM = 3;
    public static final int STATUS_MIDDLE_BOTTOM = 4;
    public static final int STATUS_LEFT_BOTTOM = 5;

    public static final int SCALE_LEVEL_1 = 1; // 最大状态，缩放比例是100%
    public static final int SCALE_LEVEL_2 = 2; // 中间状态，缩放比例scaleRate
    public static final int SCALE_LEVEL_3 = 3; // 最小状态，缩放比例是s
    /**
     * View
     */
    private ImageView imageView;
    private ImageView addView;
    private View maskView;
    private SquareDragView parentView;
    private Listener listener;
    /**
     * params
     */
    private Context mContext;
    private boolean hasSetCurrentSpringValue = false;
    private int status;
    private float scaleRate = 0.5f;
    private Spring springX;
    private Spring springY;
    private String imagePath;
    private float smallerRate = scaleRate * 0.9f;
    private ObjectAnimator scaleAnimator;
    private SpringConfig springConfigCommon = SpringConfig.fromOrigamiTensionAndFriction(140, 7);
    private int moveDstX = Integer.MIN_VALUE, moveDstY = Integer.MIN_VALUE;


    public SquareDragItemView(Context context) {
        this(context, null);
    }

    public SquareDragItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SquareDragItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SquareDragItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        mContext = context;
        inflate(mContext, R.layout.view_square_drag_item, this);
        imageView = (ImageView) findViewById(R.id.drag_item_imageview);
        addView = (ImageView) findViewById(R.id.add_view);
        maskView = findViewById(R.id.drag_item_mask_view);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!hasSetCurrentSpringValue){
                    adjustImageView();
                    hasSetCurrentSpringValue = true;
                }
            }
        });

        maskView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDraggable()) {
                    if (parentView.getActionDialog() == null) {
                        DefaultActionDialog dialog = new DefaultActionDialog(getContext());
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
                    if (parentView.getActionDialog() == null) {
                        DefaultActionDialog dialog = new DefaultActionDialog(getContext());
                        dialog.setActionDialogClick(SquareDragItemView.this)
                                .setShowDeleteButton(true)
                                .show();
                    } else {
                        parentView.getActionDialog().setActionDialogClick(SquareDragItemView.this);
                        parentView.getActionDialog()
                                .setActionDialogClick(SquareDragItemView.this)
                                .setShowDeleteButton(true)
                                .show();
                    }
                }
            }
        });
        initSpring();
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
        imagePath = null;
        imageView.setImageBitmap(null);
        addView.setVisibility(View.VISIBLE);
        parentView.onDeleteImage(SquareDragItemView.this);
    }

    /**
     * 调整ImageView的宽度和高度各为FrameLayout的一半
     */
    private void adjustImageView() {
        if (status != STATUS_LEFT_TOP) {
            imageView.setScaleX(scaleRate);
            imageView.setScaleY(scaleRate);

            maskView.setScaleX(scaleRate);
            maskView.setScaleY(scaleRate);
        }

        setCurrentSpringPos(getLeft(), getTop());
    }

    /**
     * 设置当前spring位置
     */
    private void setCurrentSpringPos(int xPos, int yPos) {
        springX.setCurrentValue(xPos);
        springY.setCurrentValue(yPos);
    }

    public boolean isDraggable() {
        return imagePath != null;
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

        springX.setSpringConfig(springConfigCommon);
        springY.setSpringConfig(springConfigCommon);
    }

    public void setScreenX(int screenX) {
        this.offsetLeftAndRight(screenX - getLeft());
    }

    public void setScreenY(int screenY) {
        this.offsetTopAndBottom(screenY - getTop());
    }

    public void setScaleRate(float scaleRate) {
        this.scaleRate = scaleRate;
        this.smallerRate = scaleRate * 0.9f;
    }

    /**
     * 从一个状态切换到另一个状态
     */
    public void switchPosition(int toStatus) {
        if (this.status == toStatus) {
            throw new RuntimeException("程序错乱");
        }

        if (toStatus == STATUS_LEFT_TOP) {
            scaleSize(SCALE_LEVEL_1);
        } else if (this.status == STATUS_LEFT_TOP) {
            scaleSize(SCALE_LEVEL_2);
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
    @SuppressLint("ObjectAnimatorBinding")
    public void scaleSize(int scaleLevel) {
        float rate = scaleRate;
        if (scaleLevel == SCALE_LEVEL_1) {
            rate = 1.0f;
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

    public int computeDraggingX(int dx) {
        this.moveDstX += dx;
        return this.moveDstX;
    }

    public int computeDraggingY(int dy) {
        this.moveDstY += dy;
        return this.moveDstY;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setParentView(SquareDragView parentView) {
        this.parentView = parentView;
    }

    public void onDragRelease() {
        if (status == SquareDragItemView.STATUS_LEFT_TOP) {
            scaleSize(SquareDragItemView.SCALE_LEVEL_1);
        } else {
            scaleSize(SquareDragItemView.SCALE_LEVEL_2);
        }

        springX.setOvershootClampingEnabled(false);
        springY.setOvershootClampingEnabled(false);
        springX.setSpringConfig(springConfigCommon);
        springY.setSpringConfig(springConfigCommon);

        Point point = parentView.getOriginViewPos(status);
        setCurrentSpringPos(getLeft(), getTop());
        this.moveDstX = point.x;
        this.moveDstY = point.y;
        animTo(moveDstX, moveDstY);
    }

    public void fillImageView(String imagePath) {
        this.imagePath = imagePath;
        addView.setVisibility(View.GONE);
        Picasso.with(getContext()).load(imagePath).into(imageView);

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

    public String getImagePath() {
        return imagePath;
    }



    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void pickImage(int imageStatus, boolean isModify);

        void takePhoto(int imageStatus, boolean isModify);
    }
}
