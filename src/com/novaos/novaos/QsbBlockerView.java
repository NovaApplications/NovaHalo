package com.novaos.novaos;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.widget.FrameLayout;

import com.novaos.novaos.config.FeatureFlags;

public class QsbBlockerView extends FrameLayout implements Workspace.OnStateChangeListener, View.OnClickListener {
    public static final Property<QsbBlockerView, Integer> QSB_BLOCKER_VIEW_ALPHA = new QsbBlockerViewAlpha(Integer.TYPE, "bgAlpha");
    private final Paint mBgPaint = new Paint(1);

    public QsbBlockerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mBgPaint.setColor(-1);
        mBgPaint.setAlpha(0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
        View gIcon = findViewById(R.id.g_icon);
        if (gIcon != null) gIcon.setOnClickListener(this);
        View micIcon = findViewById(R.id.mic_icon);
        if (micIcon != null) micIcon.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Launcher launcher = Launcher.getLauncher(getContext());
        if (v.getId() == R.id.mic_icon) {
            launcher.startVoiceAssistant();
        } else {
            launcher.startSearch("", false, null, true);
        }
    }


    @Override
    public void setPadding(int i, int i2, int i3, int i4) {
        super.setPadding(0, 0, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Workspace workspace = Launcher.getLauncher(getContext()).getWorkspace();
        workspace.setOnStateChangeListener(this);
        prepareStateChange(workspace.getState(), null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void prepareStateChange(Workspace.State state, AnimatorSet animatorSet) {
        int i;
        if (state == Workspace.State.SPRING_LOADED) {
            i = 60;
        } else {
            i = 0;
        }
        if (animatorSet == null) {
            QSB_BLOCKER_VIEW_ALPHA.set(this, i);
            return;
        }
        animatorSet.play(ObjectAnimator.ofInt(this, QSB_BLOCKER_VIEW_ALPHA, i));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPaint(mBgPaint);
    }

    private static final class QsbBlockerViewAlpha extends Property<QsbBlockerView, Integer> {

        public QsbBlockerViewAlpha(Class<Integer> type, String name) {
            super(type, name);
        }

        @Override
        public void set(QsbBlockerView qsbBlockerView, Integer num) {
            qsbBlockerView.mBgPaint.setAlpha(num);
            qsbBlockerView.setWillNotDraw(num == 0);
            qsbBlockerView.invalidate();
        }

        @Override
        public Integer get(QsbBlockerView obj) {
            return obj.mBgPaint.getAlpha();
        }

    }
}