package com.yezi.springrecyclerview;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.lang.reflect.Field;

public class SpringRecyclerView extends RecyclerView {

    private static final float SCALE_FACTOR = 0.1f;
    private static final int SCALE_TIME = 450;
    private static final float MAX_VELOCITY = 20000;

    private int mMaxLength;
    private LinearLayoutManager mLayoutManager;
    private boolean mDoAnimation;
    private VelocityTracker mVelocityTracker;
    private float downY, moveY, mVelocity, mScale = 1.0f;

    public SpringRecyclerView(Context context) {
        this(context, null);
    }

    public SpringRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpringRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    private void init() {
        mVelocityTracker = VelocityTracker.obtain();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (mLayoutManager == null) {
            mLayoutManager = (LinearLayoutManager) getLayoutManager();
        }
        if (mMaxLength == 0) {
            mMaxLength = getHeight() / 3 * 2;
        }
        mVelocityTracker.addMovement(e);
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = e.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = e.getY();

                if (mLayoutManager.findLastCompletelyVisibleItemPosition() == mLayoutManager.getItemCount() - 1
                        && moveY < downY && downY - moveY < mMaxLength && !mDoAnimation) {
                    mScale = (downY - moveY) / mMaxLength * SCALE_FACTOR + 1.0f;
                    if (mScale - 1.0f < 0.01f)
                        mScale = 1.0f;
                    setPivotX((getRight() - getLeft()) / 2);
                    setPivotY(getBottom());
                    setScaleY(mScale);
                }
                if (mLayoutManager.findFirstCompletelyVisibleItemPosition() == 0
                        && moveY > downY && moveY - downY < mMaxLength && !mDoAnimation) {
                    mScale = (moveY - downY) / mMaxLength * SCALE_FACTOR + 1.0f;
                    if (mScale - 1.0f < 0.01f)
                        mScale = 1.0f;
                    setPivotX((getRight() - getLeft()) / 2);
                    setPivotY(getTop());
                    setScaleY(mScale);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isScaled() && !mDoAnimation) {
                    backToNoScale();
                }
                downY = moveY = 0.0f;
                mScale = 1.0f;
                break;
        }

        return isScaled() || super.dispatchTouchEvent(e);
    }

    private void backToNoScale() {
        doScaleAnimation(false, moveY > downY, getScaleY(), 0.98f, 1.0f);
    }

    private void flingScale(boolean isHeader, float scale) {
        doScaleAnimation(true, isHeader, 1.0f, scale, 0.99f, 1.0f);
    }

    private boolean isScaled() {
        return getScaleY() != 1.0f;
    }

    private void doScaleAnimation(boolean isFling, boolean isHeader, float... values) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.SCALE_Y, values);
        animator.setDuration(isFling ? 2 * SCALE_TIME : SCALE_TIME);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mDoAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mDoAnimation = false;

                changeRVScrollState();
                //用于结束动画后将RV中的mScrollState置为SCROLL_STATE_IDLE，不然将会出现动画结束后第一次点击条目失效的Bug
            }
        });
        ObjectAnimator pivotX = ObjectAnimator.ofFloat(this, "pivotX", (getRight() - getLeft()) / 2);
        ObjectAnimator pivotY = ObjectAnimator.ofFloat(this, "pivotY", isHeader ? getTop() : getBottom());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(animator).with(pivotX).with(pivotY);
        animatorSet.start();
    }

    private void changeRVScrollState() {
        Class<?> clazz = getClass().getSuperclass();
        try {
            Field mScrollState = clazz.getDeclaredField("mScrollState");
            mScrollState.setAccessible(true);
            mScrollState.set(this, RecyclerView.SCROLL_STATE_IDLE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE && (mLayoutManager.findFirstCompletelyVisibleItemPosition() == 0 ||
                    mLayoutManager.findLastCompletelyVisibleItemPosition() == mLayoutManager.getItemCount() - 1)) {
                mVelocityTracker.computeCurrentVelocity(1000);
                mVelocity = Math.abs(mVelocityTracker.getYVelocity());
                if (mVelocity > MAX_VELOCITY) {
                    mVelocity = MAX_VELOCITY;
                }
                float scale = mVelocity / 1000 / 20 * SCALE_FACTOR / 3 + 1.0f;

                if (mLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    flingScale(true, scale);
                }
                if (mLayoutManager.findLastCompletelyVisibleItemPosition() == mLayoutManager.getItemCount() - 1) {
                    flingScale(false, scale);
                }
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnScrollListener(mOnScrollListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnScrollListener(mOnScrollListener);
        mVelocityTracker.recycle();
    }
}
