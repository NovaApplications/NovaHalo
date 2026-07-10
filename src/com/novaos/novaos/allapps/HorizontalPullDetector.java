package com.novaos.novaos.allapps;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class HorizontalPullDetector {

    private float mTouchSlop;

    private ScrollState mState = ScrollState.IDLE;

    enum ScrollState {
        IDLE,
        DRAGGING,
        SETTLING
    }

    private void setState(ScrollState newState) {
        if (newState == ScrollState.DRAGGING) {
            initializeDragging();
            if (mState == ScrollState.IDLE) {
                reportDragStart(false);
            } else if (mState == ScrollState.SETTLING) {
                reportDragStart(true);
            }
        }
        if (newState == ScrollState.SETTLING) {
            reportDragEnd();
        }

        mState = newState;
    }

    public boolean isDraggingOrSettling() {
        return mState == ScrollState.DRAGGING || mState == ScrollState.SETTLING;
    }

    public float getDisplacement() {
        return mDisplacementX;
    }

    private float mDownX;
    private float mDownY;

    private float mLastX;
    private long mCurrentMillis;

    private float mVelocity;
    private float mDisplacementX;
    private float mDisplacementY;

    private float mSubtractDisplacement;

    Listener mListener;

    public void setListener(Listener l) {
        mListener = l;
    }

    interface Listener {
        void onDragStart(boolean start);
        void onDrag(float displacement, float velocity);
        void onDragEnd(float velocity, boolean fling);
    }

    public HorizontalPullDetector(Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private boolean shouldScrollStart() {
        if (Math.abs(mDisplacementX) < mTouchSlop) {
            return false;
        }

        float deltaX = Math.abs(mDisplacementX);
        float deltaY = Math.abs(mDisplacementY);
        // We require a steeper angle for horizontal swipe to avoid conflicts with vertical scrolling
        return deltaX > (deltaY * 1.2f);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                mDisplacementX = 0;
                mDisplacementY = 0;
                mVelocity = 0;
                mState = ScrollState.IDLE;
                break;
            case MotionEvent.ACTION_MOVE:
                mDisplacementX = ev.getX() - mDownX;
                mDisplacementY = ev.getY() - mDownY;
                computeVelocity(ev);

                if (mState != ScrollState.DRAGGING && shouldScrollStart()) {
                    setState(ScrollState.DRAGGING);
                }
                if (mState == ScrollState.DRAGGING) {
                    reportDragging();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mState == ScrollState.DRAGGING) {
                    setState(ScrollState.SETTLING);
                }
                mState = ScrollState.IDLE;
                break;
        }
        mLastX = ev.getX();
        return isDraggingOrSettling();
    }

    private void reportDragStart(boolean recatch) {
        mListener.onDragStart(!recatch);
    }

    private void initializeDragging() {
        if (mDisplacementX > 0) {
            mSubtractDisplacement = mTouchSlop;
        } else {
            mSubtractDisplacement = -mTouchSlop;
        }
    }

    private void reportDragging() {
        mListener.onDrag(mDisplacementX - mSubtractDisplacement, mVelocity);
    }

    private void reportDragEnd() {
        mListener.onDragEnd(mVelocity, Math.abs(mVelocity) > 0.8f);
    }

    private void computeVelocity(MotionEvent to) {
        float delta = to.getX() - mLastX;
        long currentMillis = to.getEventTime();
        long previousMillis = mCurrentMillis;
        mCurrentMillis = currentMillis;

        float deltaTimeMillis = mCurrentMillis - previousMillis;
        float velocity = (deltaTimeMillis > 0) ? (delta / deltaTimeMillis) : 0;
        if (Math.abs(mVelocity) < 0.001f) {
            mVelocity = velocity;
        } else {
            float alpha = deltaTimeMillis / (800f / (2f * (float) Math.PI * 10) + deltaTimeMillis);
            mVelocity = (1.0f - alpha) * mVelocity + alpha * velocity;
        }
    }
}
