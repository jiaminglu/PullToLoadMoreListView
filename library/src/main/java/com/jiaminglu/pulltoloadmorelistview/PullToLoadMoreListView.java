package com.jiaminglu.pulltoloadmorelistview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;

import org.jetbrains.annotations.NotNull;

/**
 * Created by jiaming on 15-2-5.
 */
public class PullToLoadMoreListView extends ListView {
    public PullToLoadMoreListView(Context context) {
        super(context);
        init(context);
    }

    public PullToLoadMoreListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullToLoadMoreListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressLint("NewApi")
    public PullToLoadMoreListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    void init(Context context) {
        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();
    }

    public int getAnimationDuration() {
        return animationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }

    private int animationDuration = 200;

    private int touchSlop;

    public boolean isPullEnabled() {
        return pullEnabled;
    }

    public void setPullEnabled(boolean pullEnabled) {
        this.pullEnabled = pullEnabled;
    }

    private boolean pullEnabled = true;

    public View getPullView() {
        return pullView;
    }

    public void setPullView(View pullView) {
        this.pullView = pullView;
        addHeaderView(pullView);
    }

    private View pullView;

    /**
     * Notify the ListView how many items have been loaded, then scroll to  old items.
     * @param itemsLoaded
     */
    public void loaded(final int itemsLoaded) {
        if (pullAnimator != null) {
            pullAnimator.cancel();
        }
        setSelectionFromTop(itemsLoaded, pullProgress);
        if (itemsLoaded > 0) {
            layoutChildren();
            smoothScrollToPosition(itemsLoaded - 1);
            int newEmptyHeight = calculateEmptyHeight();
            if (newEmptyHeight >= 0 && emptyHeight - newEmptyHeight - pullView.getMeasuredHeight() > 0) {
                ObjectAnimator.ofFloat(this, "translationY", - (emptyHeight - newEmptyHeight - pullView.getMeasuredHeight()), 0).setDuration(animationDuration).start();
            }
            pullProgress = 0;
        } else {
            pullAnimateTo(0);
        }
    }

    public void setOnPullListener(OnPullListener onPullListener) {
        this.onPullListener = onPullListener;
    }

    private OnPullListener onPullListener;
    public interface OnPullListener {
        public void onPull(int distance, int total);
        public void onStartLoading();
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (pullView != null) {
            pullView.measure(widthSpec, 0);
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + pullView.getMeasuredHeight());
        }
    }

    @Override
    public void dispatchDraw(@NotNull Canvas canvas) {
        if (pullView != null) {
            canvas.save();
            canvas.translate(0, pullProgress - pullView.getMeasuredHeight());
            super.dispatchDraw(canvas);
            canvas.restore();
        } else {
            super.dispatchDraw(canvas);
        }
    }

    private int pullProgress = 0;

    private float lastX;
    private float lastY;
    @Override
    public final boolean onInterceptTouchEvent(@NotNull MotionEvent event) {
        if (!isPullEnabled())
            return super.onInterceptTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                emptyHeight = calculateEmptyHeight();
                break;
            case MotionEvent.ACTION_MOVE:
                if (listIsAtTop() && event.getY() - lastY > touchSlop && event.getX() - lastX < touchSlop) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    private boolean pulling = false;

    private ValueAnimator pullAnimator;
    void pullAnimateTo(final int progress) {
        if (pullAnimator != null)
            pullAnimator.cancel();
        pullAnimator = ValueAnimator.ofInt(pullProgress, progress);
        pullAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                pullProgress = (int) animation.getAnimatedValue();
                invalidate();
                if (onPullListener != null) {
                    onPullListener.onPull(pullProgress, pullView.getMeasuredHeight());
                }
            }
        });
        pullAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                pullProgress = progress;
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        pullAnimator.setDuration(animationDuration).start();
    }

    private int emptyHeight = 0;

    int calculateEmptyHeight() {
        return Math.max(0, getChildCount() == 0 ? getHeight() : getBottom() - getChildAt(getChildCount() - 1).getBottom()) - getPaddingBottom();
    }

    private final static double OVERSCROLL_SPEED = 1.0 / 4;
    private final static double A = (OVERSCROLL_SPEED - 1) / 2;

    @Override
    public final boolean onTouchEvent(@NotNull MotionEvent event) {
        if (!isPullEnabled())
            return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                emptyHeight = calculateEmptyHeight();
                return super.onTouchEvent(event);
            case MotionEvent.ACTION_MOVE:
                if (pulling || (pullProgress == 0 && listIsAtTop() && event.getY() - lastY > touchSlop)) {
                    if (!pulling) {
                        lastX = event.getX();
                        lastY = event.getY();
                    }
                    int max = pullView.getMeasuredHeight();
                    float delta = event.getY() - lastY;
                    if (delta < 0) {
                        pullProgress = (int) delta;
                    } else if (delta <= max) {
                        pullProgress = (int) (A / max * delta * delta + delta);
                    } else {
                        pullProgress = (int) (max * (1 + A) + (delta - max) * OVERSCROLL_SPEED);
                    }
                    if (onPullListener != null) {
                        onPullListener.onPull(pullProgress, pullView.getMeasuredHeight());
                    }
                    if (pullProgress < 0) {
                        if (pulling) {
                            event.setAction(MotionEvent.ACTION_DOWN);
                            pulling = false;
                        }
                        pullProgress = 0;
                    } else {
                        if (!pulling) {
                            event.setAction(MotionEvent.ACTION_CANCEL);
                            pulling = true;
                        }
                    }
                    invalidate();
                    super.onTouchEvent(event);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (pulling) {
                    if (pullProgress >= pullView.getMeasuredHeight() && onPullListener != null) {
                        pullAnimateTo(pullView.getMeasuredHeight());
                        onPullListener.onStartLoading();
                    } else {
                        pullAnimateTo(0);
                    }
                    pulling = false;
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (pullProgress != 0) {
                    pullAnimateTo(0);
                }
                if (pulling) {
                    pulling = false;
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean listIsAtTop()   {
        return ! (getChildCount() > 0
                && (getFirstVisiblePosition() > 0 || getChildAt(0).getTop() < getPaddingTop()));
    }
}
