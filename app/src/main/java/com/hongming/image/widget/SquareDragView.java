package com.hongming.image.widget;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;
import androidx.core.view.GestureDetectorCompat;
import androidx.customview.widget.ViewDragHelper;

import com.hongming.image.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 头像动图拖拽View
 */
public class SquareDragView extends ViewGroup implements SquareDragItemView.Listener {

    private Context mContext;
    private static final int INTERCEPT_TIME_SLOP = 200;
    private int[] allStatus;

    private ViewDragHelper mDragHelper;
    // 保存最初状态时每个itemView的坐标位置
    private List<Point> originViewPositionList = new ArrayList<>();
    //正在拖拽的View
    private SquareDragItemView draggingView;

    // 判定为滑动的阈值，单位是像素
    private int mTouchSlop = 5;
    // 小方块之间的间隔
    private int spaceInterval = 4;
    private GestureDetectorCompat moveDetector;

    // 每一个小方块的边长
    private int bigSideLength;
    // 按下的时间
    private long downTime = 0;
    // 按下时的坐标位置
    private int downX, downY;
    // 按下的时候，itemView的重心移动，此为对应线程
    private Thread moveAnchorThread;
    // itemView需要移动重心，此为对应的Handler
    private Handler anchorHandler;
    private Object synObj = new Object();
    private Listener listener;
    private ActionDialog actionDialog;

    private boolean isFlag = false;

    public SquareDragView(Context context) {
        super(context);
        init(context);
    }

    public SquareDragView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SquareDragView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SquareDragView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    private void init(Context context) {
        if (isFlag) {
            allStatus = new int[] {
                    SquareDragIndexKt.STATUS_LEFT_TOP,
                    SquareDragIndexKt.STATUS_RIGHT_MIDDLE,
                    SquareDragIndexKt.STATUS_RIGHT_TOP,
                    SquareDragIndexKt.STATUS_LEFT_BOTTOM,
                    SquareDragIndexKt.STATUS_MIDDLE_BOTTOM,
                    SquareDragIndexKt.STATUS_RIGHT_BOTTOM
            };
        } else {
            allStatus = new int[] {
                    SquareDragIndexKt.STATUS_LEFT_TOP,
                    SquareDragIndexKt.STATUS_RIGHT_TOP,
                    SquareDragIndexKt.STATUS_RIGHT_MIDDLE,
                    SquareDragIndexKt.STATUS_RIGHT_BOTTOM,
                    SquareDragIndexKt.STATUS_MIDDLE_BOTTOM,
                    SquareDragIndexKt.STATUS_LEFT_BOTTOM
            };
        }
        mContext = context;
        mDragHelper = ViewDragHelper
                .create(this, 10f, new DragHelperCallback());
        moveDetector = new GestureDetectorCompat(context,
                new MoveDetector());
        moveDetector.setIsLongpressEnabled(false); // 不能处理长按事件，否则违背最初设计的初衷
        spaceInterval = (int) getResources().getDimension(R.dimen.profile_image_margin); // 小方块之间的间隔

        // 滑动的距离阈值由系统提供
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        anchorHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (draggingView != null) {
                    // 开始移动重心的动画
                    draggingView.startAnchorAnimation();
                }
            }
        };

        this.uploadStatus = new ImageUploadStatus() {
            @Override
            public void uploadStart(String key) {
                for (int i = 0; i < allStatus.length; i++) {
                    SquareDragItemView itemView = getItemViewByStatus(i);
                    if (itemView != null && itemView.getUploadStatusHolder() != null) {
                        itemView.getUploadStatusHolder().uploadStart(key);
                    }
                }
            }

            @Override
            public void uploadSuccess(String key, String value) {
                for (int i = 0; i < allStatus.length; i++) {
                    SquareDragItemView itemView = getItemViewByStatus(i);
                    if (itemView != null && itemView.getUploadStatusHolder() != null) {
                        itemView.getUploadStatusHolder().uploadSuccess(key, value);
                    }
                }
            }

            @Override
            public void uploadFailure(String key, String error) {
                for (int i = 0; i < allStatus.length; i++) {
                    SquareDragItemView itemView = getItemViewByStatus(i);
                    if (itemView != null && itemView.getUploadStatusHolder() != null) {
                        itemView.getUploadStatusHolder().uploadFailure(key, error);
                    }
                }
            }
        };
    }


    private ImageUploadStatus uploadStatus;

    public ImageUploadStatus getUploadStatusListener() {
        return uploadStatus;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int len = allStatus.length;
        for (int i = 0; i < len; i++) {
            // 渲染结束之后，朝viewGroup中添加子View
            SquareDragItemView itemView = new SquareDragItemView(getContext());
            itemView.setStatus(allStatus[i]);
            itemView.setParentView(this);
            itemView.setListener(this);
            originViewPositionList.add(new Point()); //  原始位置点，由此初始化，一定与子View的status绑定
            addView(itemView);
        }
    }

    public Point getOriginViewPos(int status) {
        return originViewPositionList.get(status);
    }

    /**
     * 给imageView添加图片
     */
    public void fillItemImage(int imageStatus, String imagePath, boolean isModify) {
        // 1. 如果是修改图片，直接填充就好
        if (isModify) {
            SquareDragItemView itemView = getItemViewByStatus(imageStatus);
            if (itemView != null) {
                itemView.fillImageView(imagePath);
                if (imageChangesListener != null)
                    imageChangesListener.onImageEdited(imagePath, imageStatus);
                return;
            }
        }

        // 2. 新增图片
        SquareDragItemView itemView = getItemViewByStatus(imageStatus);
        if (itemView != null) {
            if (!itemView.isDraggable()||(!TextUtils.isEmpty(itemView.getImagePath())&&!itemView.getImagePath().equals(imagePath))){
                itemView.fillImageView(imagePath);
                if (imageChangesListener != null)
                    imageChangesListener.onImageAdded(imagePath, itemView.getStatus());
            }

        }
    }

    /**
     * 删除某一个ImageView时，该imageView变成空的，需要移动到队尾
     */
    public void onDeleteImage(SquareDragItemView deleteView) {
        int status = deleteView.getStatus();
        String path = deleteView.getImagePath();
        int lastDraggableViewStatus = -1;
        // 顺次将可拖拽的view往前移
        for (int i = status + 1; i < allStatus.length; i++) {
            SquareDragItemView itemView = getItemViewByStatus(i);
            if (itemView.isDraggable()) {
                // 可拖拽的view往前移
                lastDraggableViewStatus = i;
                switchPosition(i, i - 1);
            } else {
                break;
            }
        }
        if (lastDraggableViewStatus > 0) {
            // 被delete的view移动到队尾
            deleteView.switchPosition(lastDraggableViewStatus);
        }
        if (imageChangesListener != null) imageChangesListener.onImageDeleted(path, status);
    }

    @Override
    public void pickImage(int imageStatus, boolean isModify) {
        SparseArray<String> imageUrls = getRealImageUrls();
        if (imageStatus<=imageUrls.size()){
            if (listener != null) listener.pickImage(imageStatus, isModify);
        } else {
            if (listener != null) listener.pickImage(imageUrls.size(), isModify);
        }
    }

    @Override
    public void takePhoto(int imageStatus, boolean isModify) {
        SparseArray<String> imageUrls = getRealImageUrls();
        if (imageStatus<=imageUrls.size()){
            if (listener != null) listener.takePhoto(imageStatus, isModify);
        } else {
            if (listener != null) listener.takePhoto(imageUrls.size(), isModify);
        }
    }

    public void setCustomActionDialog(ActionDialog actionDialog) {
        this.actionDialog = actionDialog;
    }

    public ActionDialog getActionDialog() {
        return actionDialog;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void updateIdentifyStatus() {
        for (int index = 0;index<getChildCount();index++){
            SquareDragItemView child = (SquareDragItemView) getChildAt(index);
            child.updateIdentifyStatus();
        }
    }

    public interface Listener {
        void pickImage(int imageStatus, boolean isModify);

        void takePhoto(int imageStatus, boolean isModify);
    }

    /**
     * 这是viewdraghelper拖拽效果的主要逻辑
     */
    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            // draggingView拖动的时候，如果与其它子view交换位置，其他子view位置改变，也会进入这个回调
            // 所以此处加了一层判断，剔除不关心的回调，以优化性能
            if (changedView == draggingView) {
                SquareDragItemView changedItemView = (SquareDragItemView) changedView;
                switchPositionIfNeeded(changedItemView);
            }
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            // 按下的时候，缩放到最小的级别
            draggingView = (SquareDragItemView) child;
            // 手指按下的时候，需要把某些view bringToFront，否则的话，tryCapture将不按预期工作
            if (draggingView.isDraggable()) getParent().requestDisallowInterceptTouchEvent(true);
            return draggingView.isDraggable();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            SquareDragItemView itemView = (SquareDragItemView) releasedChild;
            itemView.onDragRelease();
            if (imageChangesListener != null) imageChangesListener.onSortChanged(getImageUrls());
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            SquareDragItemView itemView = (SquareDragItemView) child;
            return itemView.computeDraggingX(dx);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            SquareDragItemView itemView = (SquareDragItemView) child;
            return itemView.computeDraggingY(dy);
        }
    }

    /**
     * 根据draggingView的位置，看看是否需要与其它itemView互换位置
     */
    private void switchPositionIfNeeded(SquareDragItemView draggingView) {

        int fromStatus = -1, toStatus = draggingView.getStatus();

        if (isFlag) {
            int everyLength = (getMeasuredWidth() - 4 * spaceInterval) / 3;
            int centerX = draggingView.getLeft() + everyLength / 2;
            int centerY = draggingView.getTop() + everyLength / 2;
            int everyWidth = getMeasuredWidth() / 3;

            switch (draggingView.getStatus()) {
                case SquareDragIndexKt.STATUS_LEFT_TOP:
                    if (centerY < everyWidth) {
                        if (centerX >= everyWidth) {
                            fromStatus = SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                        }
                    } else {
                        fromStatus = SquareDragIndexKt.STATUS_LEFT_BOTTOM;
                    }
                    break;
                case SquareDragIndexKt.STATUS_RIGHT_MIDDLE:
                    if (centerY < everyWidth) {
                        if (centerX < everyWidth) {
                            fromStatus = SquareDragIndexKt.STATUS_LEFT_TOP;
                        } else if (centerX >= everyWidth * 2) {
                            fromStatus = SquareDragIndexKt.STATUS_RIGHT_TOP;
                        }
                    } else {
                        fromStatus = SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
                    }
                    break;
                case SquareDragIndexKt.STATUS_RIGHT_TOP:
                    if (centerY < everyWidth) {
                        if (centerX < everyWidth * 2) {
                            fromStatus = SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                        }
                    } else {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
                    }
                    break;
                case SquareDragIndexKt.STATUS_LEFT_BOTTOM:
                    if (centerY >= everyWidth) {
                        if (centerX >= everyWidth) {
                            fromStatus = SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
                        }
                    } else {
                        if (centerX >= everyWidth && centerX < everyWidth * 2) {
                            fromStatus = SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                        } else if (centerX >= everyWidth * 2) {
                            fromStatus = SquareDragIndexKt.STATUS_RIGHT_TOP;
                        } else {
                            fromStatus = SquareDragIndexKt.STATUS_LEFT_TOP;
                        }
                    }
                    break;
                case SquareDragIndexKt.STATUS_MIDDLE_BOTTOM:
                    if (centerY >= everyWidth) {
                        if (centerX < everyWidth) {
                            fromStatus = SquareDragIndexKt.STATUS_LEFT_BOTTOM;
                        } else if (centerX >= everyWidth * 2) {
                            fromStatus = SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
                        }
                    } else {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                    }
                    break;
                case SquareDragIndexKt.STATUS_RIGHT_BOTTOM:
                    if (centerY >= everyWidth) {
                        if (centerX < everyWidth * 2) {
                            fromStatus = SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
                        }
                    } else {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_TOP;
                    }
                    break;
                default:
                    break;
            }
            if (fromStatus != -1) {
                System.out.println("fromStatus " + fromStatus + " toStatus " + toStatus);
            }
        } else {
            int centerX = draggingView.getLeft() + bigSideLength / 2;
            int centerY = draggingView.getTop() + bigSideLength / 2;
            int everyWidth = getMeasuredWidth() / 3;

            switch (draggingView.getStatus()) {
                case SquareDragIndexKt.STATUS_LEFT_TOP:
                    // 拖动的是左上角的大图
                    // 依次将小图向上顶
                    int fromChangeIndex = 0;
                    if (centerX > everyWidth * 2) {
                        // 大图往右越过了位置，一定会跟右侧的三个View交换位置才行
                        if (centerY < everyWidth) {
                            // 跟右上角的View交换位置
                            fromChangeIndex = SquareDragIndexKt.STATUS_RIGHT_TOP;
                        } else if (centerY < everyWidth * 2) {
                            fromChangeIndex = SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                        } else {
                            fromChangeIndex = SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
                        }
                    } else if (centerY > everyWidth * 2) {
                        if (centerX < everyWidth) {
                            fromChangeIndex = SquareDragIndexKt.STATUS_LEFT_BOTTOM;
                        } else if (centerX < everyWidth * 2) {
                            fromChangeIndex = SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
                        } else {
                            fromChangeIndex = SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
                        }
                    }

                    SquareDragItemView toItemView = getItemViewByStatus(fromChangeIndex);
                    if (!toItemView.isDraggable()) {
                        return;
                    }

                    synchronized (this) {
                        for (int i = 1; i <= fromChangeIndex; i++) {
                            switchPosition(i, i - 1);
                        }
                        draggingView.setStatus(fromChangeIndex);
                    }
                    return;
                case SquareDragIndexKt.STATUS_RIGHT_TOP:
                    if (centerX < everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_LEFT_TOP;
                    } else if (centerY > everyWidth) {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                    }
                    break;

                case SquareDragIndexKt.STATUS_RIGHT_MIDDLE:
                    if (centerX < everyWidth * 2 && centerY < everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_LEFT_TOP;
                    } else if (centerY < everyWidth) {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_TOP;
                    } else if (centerY > everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
                    }
                    break;
                case SquareDragIndexKt.STATUS_RIGHT_BOTTOM:
                    if (centerX < everyWidth * 2 && centerY < everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_LEFT_TOP;
                    } else if (centerX < everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
                    } else if (centerY < everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                    }
                    break;
                case SquareDragIndexKt.STATUS_MIDDLE_BOTTOM:
                    if (centerX < everyWidth) {
                        fromStatus = SquareDragIndexKt.STATUS_LEFT_BOTTOM;
                    } else if (centerX > everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
                    } else if (centerY < everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_LEFT_TOP;
                    }
                    break;
                case SquareDragIndexKt.STATUS_LEFT_BOTTOM:
                    if (centerX > everyWidth) {
                        fromStatus = SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
                    } else if (centerY < everyWidth * 2) {
                        fromStatus = SquareDragIndexKt.STATUS_LEFT_TOP;
                    }
                    break;
                default:
                    break;
            }
        }


        synchronized (synObj) {
            if (fromStatus > 0) {
                if (switchPosition(fromStatus, toStatus)) {
                    draggingView.setStatus(fromStatus);
                }
            } else if (fromStatus == 0) {
                for (int i = toStatus - 1; i >= 0; i--) {
                    switchPosition(i, i + 1);
                }
                draggingView.setStatus(fromStatus);
            }
        }
    }

    /**
     * 调换位置
     */
    private boolean switchPosition(int fromStatus, int toStatus) {
        SquareDragItemView itemView = getItemViewByStatus(fromStatus);
        if (itemView.isDraggable()) {
            itemView.switchPosition(toStatus);
            return true;
        }
        return false;
    }

    /**
     * 根据status获取itemView
     */
    public SquareDragItemView getItemViewByStatus(int status) {
        int num = getChildCount();
        for (int i = 0; i < num; i++) {
            SquareDragItemView itemView = (SquareDragItemView) getChildAt(i);
            if (itemView.getStatus() == status) {
                return itemView;
            }
        }
        return null;
    }

    class MoveDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx,
                                float dy) {
            // 拖动了，touch不往下传递
            return Math.abs(dy) + Math.abs(dx) > mTouchSlop;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int everyLength = (getMeasuredWidth() - 4 * spaceInterval) / 3;//最小的边长
        int itemLeft = 0;
        int itemTop = 0;
        int itemRight = 0;
        int itemBottom = 0;
        // 每个view的边长是everyLength * 2 + spaceInterval
        bigSideLength = everyLength * 2 + spaceInterval;//左上角的最大的图形的边长
        int halfSideLength = bigSideLength / 2; // 边长的一半
        int rightCenter = r - spaceInterval - everyLength / 2;
        int bottomCenter = b - spaceInterval - everyLength;

//        float scaleRate = (float) everyLength / sideLength;
        int num = getChildCount();
        if (isFlag) {
            for (int i = 0; i < num; i++) {
                SquareDragItemView itemView = (SquareDragItemView) getChildAt(i);
//            PPLog.e("======" + scaleRate);
//            itemView.setScaleRate(scaleRate);
                switch (itemView.getStatus()) {
                    case SquareDragIndexKt.STATUS_LEFT_TOP:
                        itemLeft = l + spaceInterval;
                        itemRight = itemLeft + everyLength;
                        itemTop = spaceInterval;
                        itemBottom = spaceInterval + everyLength;
                        break;
                    case SquareDragIndexKt.STATUS_RIGHT_MIDDLE:
                        itemLeft = l + spaceInterval * 2 + everyLength;
                        itemRight = itemLeft + everyLength;
                        itemTop = spaceInterval;
                        itemBottom = spaceInterval + everyLength;
                        break;
                    case SquareDragIndexKt.STATUS_RIGHT_TOP:
                        itemLeft = l + spaceInterval * 3 + everyLength * 2;
                        itemRight = itemLeft + everyLength;
                        itemTop = spaceInterval;
                        itemBottom = spaceInterval + everyLength;
                        break;
                    case SquareDragIndexKt.STATUS_LEFT_BOTTOM:
                        itemLeft = l + spaceInterval;
                        itemRight = itemLeft + everyLength;
                        itemTop = spaceInterval * 2 + everyLength;
                        itemBottom = itemTop + everyLength;
                        break;
                    case SquareDragIndexKt.STATUS_MIDDLE_BOTTOM:
                        itemLeft = l + spaceInterval * 2 + everyLength;
                        itemRight = itemLeft + everyLength;
                        itemTop = spaceInterval * 2 + everyLength;
                        itemBottom = itemTop + everyLength;
                        break;
                    case SquareDragIndexKt.STATUS_RIGHT_BOTTOM:
                        itemLeft = l + spaceInterval * 3 + everyLength * 2;
                        itemRight = itemLeft + everyLength;
                        itemTop = spaceInterval * 2 + everyLength;
                        itemBottom = itemTop + everyLength;
                        break;
                }

                ViewGroup.LayoutParams lp = itemView.getLayoutParams();
                lp.width = everyLength;
                lp.height = everyLength;
                itemView.setLayoutParams(lp);

                Point itemPoint = originViewPositionList.get(itemView.getStatus());
                itemPoint.x = itemLeft;
//            if (itemView.getStatus()>=3){
//                itemPoint.y = itemTop+PixelUtils.dip2px(getContext(),45);
//                itemTop = itemTop+PixelUtils.dip2px(getContext(),45);
//            }else {
                itemPoint.y = itemTop;
//            }
                fitNumber(i, everyLength);

                Log.d("TAG", "viewInfo,status=" + itemView.getStatus() + ",left=" + itemLeft + ",right=" + itemRight + ",top=" + itemTop + ",bottom=" + itemBottom);
                itemView.layout(itemLeft, itemTop, itemRight, itemBottom);
            }
            return;
        }
        for (int i = 0; i < num; i++) {
            SquareDragItemView itemView = (SquareDragItemView) getChildAt(i);
//            PPLog.e("======" + scaleRate);
//            itemView.setScaleRate(scaleRate);
            switch (itemView.getStatus()) {
                case SquareDragIndexKt.STATUS_LEFT_TOP:
                    int centerPos = spaceInterval + everyLength + spaceInterval / 2;
                    itemLeft = centerPos - halfSideLength;
                    itemRight = centerPos + halfSideLength;
                    itemTop = centerPos - halfSideLength;
                    itemBottom = centerPos + halfSideLength;
                    break;
                case SquareDragIndexKt.STATUS_RIGHT_TOP:
                    itemLeft = rightCenter - halfSideLength;
                    itemRight = rightCenter + halfSideLength;
                    int hCenter1 = spaceInterval + everyLength / 2;
                    itemTop = hCenter1 - halfSideLength;
                    itemBottom = hCenter1 + halfSideLength;
                    break;
                case SquareDragIndexKt.STATUS_RIGHT_MIDDLE:
                    itemLeft = rightCenter - halfSideLength;
                    itemRight = rightCenter + halfSideLength;
                    int hCenter2 = getMeasuredHeight() / 2;
                    itemTop = hCenter2 - halfSideLength;
                    itemBottom = hCenter2 + halfSideLength;
                    break;
                case SquareDragIndexKt.STATUS_RIGHT_BOTTOM:
                    itemLeft = rightCenter - halfSideLength;
                    itemRight = rightCenter + halfSideLength;
                    itemTop = bottomCenter - halfSideLength;
                    itemBottom = bottomCenter + halfSideLength;
                    break;
                case SquareDragIndexKt.STATUS_MIDDLE_BOTTOM:
                    int vCenter1 = l + getMeasuredWidth() / 2;
                    itemLeft = vCenter1 - halfSideLength;
                    itemRight = vCenter1 + halfSideLength;
                    itemTop = bottomCenter - halfSideLength;
                    itemBottom = bottomCenter + halfSideLength;
                    break;
                case SquareDragIndexKt.STATUS_LEFT_BOTTOM:
                    int vCenter2 = l + spaceInterval + everyLength / 2;
                    itemLeft = vCenter2 - halfSideLength;
                    itemRight = vCenter2 + halfSideLength;
                    itemTop = bottomCenter - halfSideLength;
                    itemBottom = bottomCenter + halfSideLength;
                    break;
            }

            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            lp.width = bigSideLength;
            lp.height = bigSideLength;
            itemView.setLayoutParams(lp);

            Point itemPoint = originViewPositionList.get(itemView.getStatus());
            itemPoint.x = itemLeft;
            if (itemView.getStatus()>=3){
                itemPoint.y = itemTop+PixelUtils.dip2px(getContext(),45);
                itemTop = itemTop+PixelUtils.dip2px(getContext(),45);
            }else {
                itemPoint.y = itemTop;
            }
            fitNumber(i,everyLength);

            Log.d("TAG", "viewInfo,status=" + itemView.getStatus() + ",left=" + itemLeft + ",right=" + itemRight + ",top=" + itemTop + ",bottom=" + itemBottom);
            itemView.layout(itemLeft, itemTop, itemRight, itemBottom);
        }
    }

    private void fitNumber(int i,int everyLength){
        if (isFlag) {
            int left = spaceInterval;
            if (squareNumberView != null) {
                switch (i) {
                    case SquareDragIndexKt.STATUS_LEFT_TOP:
                        squareNumberView.updateLayout(i, (int) (everyLength - left), everyLength - left, 0, 0);
                        break;
                    case SquareDragIndexKt.STATUS_RIGHT_MIDDLE:
                        squareNumberView.updateLayout(i, (int) (everyLength * 2), everyLength - left, 0, 0);
                        break;
                    case SquareDragIndexKt.STATUS_RIGHT_TOP:
                        squareNumberView.updateLayout(i, (int) (everyLength * 3 + left), everyLength - left, 0, 0);
                        break;
                    case SquareDragIndexKt.STATUS_LEFT_BOTTOM:
                        squareNumberView.updateLayout(i, (int) (everyLength - left), everyLength*2, 0, 0);
                        break;
                    case SquareDragIndexKt.STATUS_MIDDLE_BOTTOM:
                        squareNumberView.updateLayout(i, (int) (everyLength * 2), everyLength*2, 0, 0);
                        break;
                    case SquareDragIndexKt.STATUS_RIGHT_BOTTOM:
                        squareNumberView.updateLayout(i, (int) (everyLength * 3 + left), everyLength*2, 0, 0);
                        break;
                }
            }
            return;
        }
        if (squareNumberView != null) {
            if (i == 0) {
                squareNumberView.updateLayout(i, everyLength * 2 - spaceInterval*2, everyLength * 2 - spaceInterval*2, 0, 0);
            }else if (i ==1){
                squareNumberView.updateLayout(i, (int) (everyLength * 3 - spaceInterval*1.5)+ PixelUtils.dip2px(getContext(),2), everyLength - spaceInterval*3, 0, 0);
            }else if (i ==2){
                squareNumberView.updateLayout(i, (int) (everyLength * 3 - spaceInterval*1.5)+ PixelUtils.dip2px(getContext(),2), everyLength*2 - spaceInterval*2, 0, 0);
            }else if (i ==3){
                squareNumberView.updateLayout(i, (int) (everyLength * 3 - spaceInterval*1.5)+ PixelUtils.dip2px(getContext(),2), everyLength*3 - spaceInterval*2+ PixelUtils.dip2px(getContext(),2), 0, 0);

            }else if (i ==4){
                squareNumberView.updateLayout(i, (int) (everyLength * 2 - spaceInterval*2), everyLength*3 - spaceInterval*2+ PixelUtils.dip2px(getContext(),3), 0, 0);
            }else if (i ==5){
                squareNumberView.updateLayout(i, (int) (everyLength - spaceInterval*3), everyLength*3 - spaceInterval*2+ PixelUtils.dip2px(getContext(),3), 0, 0);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int width = resolveSizeAndState(maxWidth, widthMeasureSpec, 0);
        if (isFlag) {
            setMeasuredDimension(width, width/3*2);
            return;
        }
        setMeasuredDimension(width, width);
    }

    SquareNumberView squareNumberView;

    public void changeNumber(SquareNumberView squareNumberView) {
        this.squareNumberView = squareNumberView;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            downX = (int) ev.getX();
            downY = (int) ev.getY();
            downTime = System.currentTimeMillis();
            bringToFrontWhenTouchDown(downX, downY);
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (draggingView != null) {
                draggingView.onDragRelease();
            }
            draggingView = null;

            if (null != moveAnchorThread) {
                moveAnchorThread.interrupt();
                moveAnchorThread = null;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 按下时根据触点的位置，将某个view bring到前台
     */
    private void bringToFrontWhenTouchDown(final int downX, final int downY) {
        int statusIndex = getStatusByDownPoint(downX, downY);
        final SquareDragItemView itemView = getItemViewByStatus(statusIndex);
        if (indexOfChild(itemView) != getChildCount() - 1) {
            bringChildToFront(itemView);
        }
        if (!itemView.isDraggable()) {
            return;
        }

        itemView.saveAnchorInfo(downX, downY);
        moveAnchorThread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(INTERCEPT_TIME_SLOP);
                } catch (InterruptedException e) {
                }

                Message msg = anchorHandler.obtainMessage();
                msg.sendToTarget();
            }
        };
        moveAnchorThread.start();
    }

    private int getStatusByDownPoint(int downX, int downY) {
        int everyWidth = getMeasuredWidth() / 3;
        if (isFlag) {
            if (downX < everyWidth) {
                if (downY < everyWidth) {
                    return SquareDragIndexKt.STATUS_LEFT_TOP;
                } else {
                    return SquareDragIndexKt.STATUS_LEFT_BOTTOM;
                }
            } else if (downX < everyWidth * 2 && downX >= everyWidth) {
                if (downY < everyWidth) {
                    return SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
                } else {
                    return SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
                }
            } else {
                if (downY < everyWidth) {
                    return SquareDragIndexKt.STATUS_RIGHT_TOP;
                } else {
                    return SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
                }
            }
        }
        if (downX < everyWidth) {
            if (downY < everyWidth * 2) {
                return SquareDragIndexKt.STATUS_LEFT_TOP;
            } else {
                return SquareDragIndexKt.STATUS_LEFT_BOTTOM;
            }
        } else if (downX < everyWidth * 2) {
            if (downY < everyWidth * 2) {
                return SquareDragIndexKt.STATUS_LEFT_TOP;
            } else {
                return SquareDragIndexKt.STATUS_MIDDLE_BOTTOM;
            }
        } else {
            if (downY < everyWidth) {
                return SquareDragIndexKt.STATUS_RIGHT_TOP;
            } else if (downY < everyWidth * 2) {
                return SquareDragIndexKt.STATUS_RIGHT_MIDDLE;
            } else {
                return SquareDragIndexKt.STATUS_RIGHT_BOTTOM;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (downTime > 0 && System.currentTimeMillis() - downTime > INTERCEPT_TIME_SLOP) {
            return true;
        }
        boolean shouldIntercept = mDragHelper.shouldInterceptTouchEvent(ev);
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mDragHelper.processTouchEvent(ev);
        }

        boolean moveFlag = moveDetector.onTouchEvent(ev);
        if (moveFlag) {
            if (null != moveAnchorThread) {
                moveAnchorThread.interrupt();
                moveAnchorThread = null;
            }

            if (null != draggingView && draggingView.isDraggable()) {
                draggingView.startAnchorAnimation();
            }
        }
        return shouldIntercept && moveFlag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        try {
            // 该行代码可能会抛异常，正式发布时请将这行代码加上try catch
            mDragHelper.processTouchEvent(e);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    /**
     * if Itemview is empty then return null
     *
     * @return
     */
    public SparseArray<String> getImageUrls() {
        SparseArray<String> stringSparseArray = new SparseArray<>();
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) instanceof SquareDragItemView) {
                stringSparseArray.put(((SquareDragItemView) getChildAt(i)).getStatus(), ((SquareDragItemView) getChildAt(i)).getImagePath());
            }
        }
        return stringSparseArray;
    }

    public SparseArray<String> getRealImageUrls() {
        SparseArray<String> stringSparseArray = new SparseArray<>();
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) instanceof SquareDragItemView&&!TextUtils.isEmpty(((SquareDragItemView) getChildAt(i)).getImagePath())) {
                stringSparseArray.put(((SquareDragItemView) getChildAt(i)).getStatus(), ((SquareDragItemView) getChildAt(i)).getImagePath());
            }
        }
        return stringSparseArray;
    }

    public int getImageSetSize() {
        return allStatus.length;
    }

    public void setImageChangesListener(ImageChangesListener imageChangesListener) {
        this.imageChangesListener = imageChangesListener;
    }

    ImageChangesListener imageChangesListener;

    public interface ImageChangesListener {
        void onImageAdded(String uri, int index);

        void onImageEdited(String uri, int index);

        void onImageDeleted(String uri, int index);

        void onSortChanged(SparseArray<String> urls);
    }

}
