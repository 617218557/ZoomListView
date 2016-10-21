
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ListView;

/**
 * Created by yifeifeng on 16/8/29.
 */
public class ZoomListViewView extends ListView {

    private Context context;

    private static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;

    private float mScaleFactor = 1.f;
    private float maxWidth = 0.0f;
    private float maxHeight = 0.0f;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mPosX;
    private float mPosY;
    private float width;
    private float height;


    private float minScale = 0.6f;
    private float originScale = 1.0f;
    private float doubleTimesScale = 2.0f;
    private float maxScale = 3.0f;

    private float scaleCenterX, scaleCenterY;
    private float mLastScale = 1.0f;
    private boolean isScaling = false;

    public ZoomListViewView(Context context) {
        super(context);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public ZoomListViewView(Context context, AttributeSet attr) {
        super(context, attr);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        super.onTouchEvent(ev);
        final int action = ev.getAction();
        mScaleDetector.onTouchEvent(ev);
        scrollerReadViewGestureDetector.onTouchEvent(ev);
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mLastTouchX = x;
                mLastTouchY = y;

                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                float x, y;
                try {
                    x = ev.getX(pointerIndex);
                    y = ev.getY(pointerIndex);

                } catch (IllegalArgumentException ex) {
                    ex.printStackTrace();
                    return super.onTouchEvent(ev);
                }
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                if (isScaling) {
                    mPosX += scaleCenterX * (mLastScale - mScaleFactor);
                    mPosY += scaleCenterY * (mLastScale - mScaleFactor);
                    mLastScale = mScaleFactor;
                } else if (mScaleFactor > originScale) {
                    mPosX += dx;
                    mPosY += dy;

                    if (mPosX > 0.0f) {
                        mPosX = 0.0f;
                    } else if (mPosX < maxWidth) {
                        mPosX = maxWidth;
                    }

                    if (mPosY > 0.0f) {
                        mPosY = 0.0f;
                    } else if (mPosY < maxHeight) {
                        mPosY = maxHeight;
                    }
                }

                mLastTouchX = x;
                mLastTouchY = y;

                invalidate();
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.translate(mPosX, mPosY);
        canvas.scale(mScaleFactor, mScaleFactor);
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);

        canvas.translate(mPosX, mPosY);
        canvas.scale(mScaleFactor, mScaleFactor);
        super.dispatchDraw(canvas);
        canvas.restore();
        invalidate();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {

            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(minScale, Math.min(mScaleFactor, maxScale));
            maxWidth = width - (width * mScaleFactor);
            maxHeight = height - (height * mScaleFactor);
            scaleCenterX = detector.getFocusX();
            scaleCenterY = detector.getFocusY();
            isScaling = true;
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mScaleFactor < originScale) {
                scaleTo(originScale);
            }
            isScaling = false;
        }
    }

    private class scrollReaderViewGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //V1.3需求：1倍图片（正常情况）下双击图片进行放大，放大过的图片（放大倍数超过1）双击缩回1倍大小。
            scaleCenterX = e.getX();
            scaleCenterY = e.getY();
            if (originScale < mScaleFactor) {
                scaleTo(originScale);
            } else if (mScaleFactor == originScale) {
                scaleTo(doubleTimesScale);
            }
            return super.onDoubleTap(e);
        }
    }

    // 缩放到指定倍数大小
    synchronized private void scaleTo(final float value) {
        ValueAnimator scale = ValueAnimator.ofFloat(mScaleFactor, value);
        scale.setDuration(300);
        scale.setInterpolator(new AccelerateInterpolator());
        scale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScaleFactor = (Float) animation.getAnimatedValue();

                float dx = scaleCenterX * (mLastScale - mScaleFactor);
                float dy = scaleCenterY * (mLastScale - mScaleFactor);

                mPosX += dx;
                mPosY += dy;
                maxWidth = width - (width * mScaleFactor);
                maxHeight = height - (height * mScaleFactor);
                if (mPosX > 0.0f) {
                    if (mScaleFactor >= originScale) {
                        mPosX = 0.0f;
                    }
                } else if (mPosX < maxWidth) {
                    if (mScaleFactor >= originScale) {
                        mPosX = maxWidth;
                    }
                }

                if (mPosY > 0.0f) {
                    if (mScaleFactor >= originScale) {
                        mPosY = 0.0f;
                    }
                } else if (mPosY < maxHeight) {
                    mPosY = maxHeight;
                }

                invalidate();
                mLastScale = mScaleFactor;
            }
        });
        scale.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isScaling = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        isScaling = true;
        scale.start();
    }

}
