package com.jiaminglu.pulltoloadmorelistview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
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

    private int touchSlop;

    public boolean isPullEnabled() {
        return pullEnabled;
    }

    public void setPullEnabled(boolean pullEnabled) {
        this.pullEnabled = pullEnabled;
    }

    boolean pullEnabled = true;

    public View getPullView() {
        return pullView;
    }

    public void setPullView(View pullView) {
        this.pullView = pullView;
        addHeaderView(pullView);
    }

    View pullView;

    /**
     * Notify the ListView how many items have been loaded, then scroll to  old items.
     * @param itemsLoaded
     */
    public void loaded(final int itemsLoaded) {
        loading = false;
        if (pullAnimator != null) {
            pullAnimator.cancel();
        }
        setSelectionFromTop(itemsLoaded, pullProgress);
        if (itemsLoaded > 0) {
            layoutChildren();
            smoothScrollToPosition(itemsLoaded - 1);
            int newEmptyHeight = calculateEmptyHeight();
            if (newEmptyHeight >= 0 && emptyHeight - newEmptyHeight - pullView.getMeasuredHeight() > 0) {
                ObjectAnimator.ofFloat(this, "translationY", - (emptyHeight - newEmptyHeight - pullView.getMeasuredHeight()), 0).setDuration(200).start();
            }
            pullProgress = 0;
        } else {
            pullAnimateTo(0);
        }
    }
    boolean loading = false;

    OnLoadMoreListener onLoadMoreListener;

    public void setOnPullListener(OnPullListener onPullListener) {
        this.onPullListener = onPullListener;
    }

    OnPullListener onPullListener;

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        onLoadMoreListener = listener;
    }

    /**
     * Do actual load more oprations here, must call listView.loaded(int) afterward.
     */
    public interface OnLoadMoreListener {
        public void onLoadMore(PullToLoadMoreListView listView);
    }

    public interface OnPullListener {
        public void onPull(float distance, float total);
        public void onStartLoading();
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (pullEnabled) {
            pullView.measure(widthSpec, 0);
            setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + pullView.getMeasuredHeight());
        }
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (pullEnabled)
            super.onLayout(changed, l, t, r, b + pullView.getMeasuredHeight());
        else
            super.onLayout(changed, l, t, r, b);
    }

    @Override
    public void dispatchDraw(@NotNull Canvas canvas) {
        if (pullEnabled) {
            canvas.save();
            canvas.translate(0, pullProgress - pullView.getMeasuredHeight());
            super.dispatchDraw(canvas);
            canvas.restore();
        } else {
            super.dispatchDraw(canvas);
        }
    }

    int pullProgress = 0;

    float lastX;
    float lastY;
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

    boolean pulling = false;

    ValueAnimator pullAnimator;
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
        pullAnimator.setDuration(200).start();
    }

    int emptyHeight = 0;

    int calculateEmptyHeight() {
        return Math.max(0, getChildCount() == 0 ? getHeight() : getBottom() - getChildAt(getChildCount() - 1).getBottom()) - getPaddingBottom();
    }

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
                    float delta = event.getY() - lastY;
                    pullProgress = (int) (delta / 2);
                    invalidate();
                    if (onPullListener != null) {
                        onPullListener.onPull(pullProgress, pullView.getMeasuredHeight());
                    }
                    pulling = true;
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(event);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (pulling) {
                    if (pullProgress >= pullView.getMeasuredHeight() && onLoadMoreListener != null) {
                        pullAnimateTo(pullView.getMeasuredHeight());
                        if (onPullListener != null) {
                            onPullListener.onStartLoading();
                            loading = true;
                        }
                        pullAnimator.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                onLoadMoreListener.onLoadMore(PullToLoadMoreListView.this);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
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
