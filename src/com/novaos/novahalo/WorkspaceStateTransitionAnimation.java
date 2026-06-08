/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.novaos.novahalo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.novaos.novahalo.util.Thunk;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A convenience class to update a view's visibility state after an alpha animation.
 */
class AlphaUpdateListener extends AnimatorListenerAdapter {
    private static final float ALPHA_CUTOFF_THRESHOLD = 0.01f;

    private View mView;
    private boolean mAccessibilityEnabled;

    public AlphaUpdateListener(View v, boolean accessibilityEnabled) {
        mView = v;
        mAccessibilityEnabled = accessibilityEnabled;
    }

    @Override
    public void onAnimationEnd(Animator arg0) {
        updateVisibility(mView, mAccessibilityEnabled);
    }

    public static void updateVisibility(View view, boolean accessibilityEnabled) {
        if (view.getAlpha() < ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != View.INVISIBLE) {
            view.setVisibility(View.INVISIBLE);
        } else if (view.getAlpha() > ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
        if (accessibilityEnabled) {
            view.setImportantForAccessibility(view.getVisibility() == View.VISIBLE
                    ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
    }

    @Override
    public void onAnimationStart(Animator arg0) {
        mView.setVisibility(View.VISIBLE);
    }
}

/** Manages the state transitions for the Workspace. */
public class WorkspaceStateTransitionAnimation {

    public static final String TAG = "WorkspaceStateTransitionAnimation";

    @Thunk final Launcher mLauncher;
    @Thunk final Workspace mWorkspace;

    @Thunk AnimatorSet mStateAnimator;

    @Thunk float mNewScale;

    @Thunk final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();

    @Thunk float mWorkspaceScrimAlpha;
    @Thunk final int mAllAppsTransitionDuration;
    @Thunk final int mOverviewTransitionDuration;
    @Thunk final int mOverlayTransitionDuration;
    public final int mOverviewTransitionTime;
    @Thunk final int mOverlayTransitionTime;

    public WorkspaceStateTransitionAnimation(Launcher launcher, Workspace workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;

        Resources res = launcher.getResources();
        mAllAppsTransitionDuration = res.getInteger(R.integer.config_allAppsTransitionTime);
        mOverviewTransitionDuration = res.getInteger(R.integer.config_overviewTransitionTime);
        mOverlayTransitionDuration = res.getInteger(R.integer.config_overlayRevealTime);
        mOverlayTransitionTime = res.getInteger(R.integer.config_overlayTransitionTime);
        mOverviewTransitionTime = mOverviewTransitionDuration;
        mWorkspaceScrimAlpha = res.getInteger(R.integer.config_workspaceScrimAlpha) / 100f;
    }

    public AnimatorSet getAnimationToState(Workspace.State fromState, Workspace.State toState,
            boolean animated, HashMap<View, Integer> layerViews) {
        mNewScale = 1.0f;
        float finalWorkspaceAlpha = 1.0f;
        float finalHotseatAlpha = 1.0f;
        float finalOverviewPanelAlpha = 0.0f;
        boolean hasScrim = false;

        switch (toState) {
            case NORMAL:
                mNewScale = 1.0f;
                finalWorkspaceAlpha = 1.0f;
                finalHotseatAlpha = 1.0f;
                finalOverviewPanelAlpha = 0.0f;
                hasScrim = false;
                break;
            case SPRING_LOADED:
                mNewScale = mLauncher.getDeviceProfile().workspaceSpringLoadShrinkFactor;
                finalWorkspaceAlpha = 1.0f;
                finalHotseatAlpha = 0.0f;
                finalOverviewPanelAlpha = 0.0f;
                hasScrim = false;
                break;
            case OVERVIEW:
                mNewScale = mWorkspace.getOverviewModeShrinkFactor();
                finalWorkspaceAlpha = 1.0f;
                finalHotseatAlpha = 0.0f;
                finalOverviewPanelAlpha = 1.0f;
                hasScrim = true;
                break;
            case NORMAL_HIDDEN:
                mNewScale = 0.9f;
                finalWorkspaceAlpha = 0.0f;
                finalHotseatAlpha = 0.0f;
                finalOverviewPanelAlpha = 0.0f;
                hasScrim = false;
                break;
            case OVERVIEW_HIDDEN:
                mNewScale = mWorkspace.getOverviewModeShrinkFactor();
                finalWorkspaceAlpha = 0.0f;
                finalHotseatAlpha = 0.0f;
                finalOverviewPanelAlpha = 0.0f;
                hasScrim = false;
                break;
        }

        final int duration = getAnimationDuration(toState);
        resetAnimation();

        if (animated) {
            mStateAnimator = LauncherAnimUtils.createAnimatorSet();
        }

        final float finalBackgroundAlpha = hasScrim ? 1.0f : 0f;
        final int childCount = mWorkspace.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final CellLayout cl = (CellLayout) mWorkspace.getChildAt(i);
            if (animated) {
                float oldBackgroundAlpha = cl.getBackgroundAlpha();
                if (finalBackgroundAlpha != oldBackgroundAlpha) {
                    ValueAnimator bgAnim = ObjectAnimator.ofFloat(cl, "backgroundAlpha",
                            oldBackgroundAlpha, finalBackgroundAlpha);
                    bgAnim.setInterpolator(mZoomInInterpolator);
                    bgAnim.setDuration(duration);
                    mStateAnimator.play(bgAnim);
                }
                ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(cl.getShortcutsAndWidgets(),
                        "alpha", finalWorkspaceAlpha);
                alphaAnim.setDuration(duration);
                alphaAnim.setInterpolator(mZoomInInterpolator);
                mStateAnimator.play(alphaAnim);
            } else {
                cl.setBackgroundAlpha(finalBackgroundAlpha);
                cl.setShortcutAndWidgetAlpha(finalWorkspaceAlpha);
            }
        }

        final ViewGroup overviewPanel = mLauncher.getOverviewPanel();
        if (animated) {
            LauncherViewPropertyAnimator scale = new LauncherViewPropertyAnimator(mWorkspace);
            scale.scaleX(mNewScale)
                .scaleY(mNewScale)
                .setDuration(duration)
                .setInterpolator(mZoomInInterpolator);
            mStateAnimator.play(scale);

            Animator hotseatAlphaAnimation = mWorkspace.createHotseatAlphaAnimator(finalHotseatAlpha);
            hotseatAlphaAnimation.setDuration(duration);
            hotseatAlphaAnimation.setInterpolator(mZoomInInterpolator);
            mStateAnimator.play(hotseatAlphaAnimation);

            ObjectAnimator overviewPanelAlpha = ObjectAnimator.ofFloat(overviewPanel, "alpha",
                    finalOverviewPanelAlpha);
            overviewPanelAlpha.addListener(new AlphaUpdateListener(overviewPanel, true));
            overviewPanelAlpha.setDuration(duration);
            overviewPanelAlpha.setInterpolator(mZoomInInterpolator);
            mStateAnimator.play(overviewPanelAlpha);

            mStateAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mStateAnimator = null;
                }
            });
        } else {
            mWorkspace.setScaleX(mNewScale);
            mWorkspace.setScaleY(mNewScale);
            mWorkspace.setHotseatAlphaAtIndex(finalHotseatAlpha, Workspace.HOTSEAT_STATE_ALPHA_INDEX);
            overviewPanel.setAlpha(finalOverviewPanelAlpha);
            AlphaUpdateListener.updateVisibility(overviewPanel, true);
        }

        return mStateAnimator;
    }

    private void resetAnimation() {
        if (mStateAnimator != null) {
            mStateAnimator.setDuration(0);
            mStateAnimator.cancel();
            mStateAnimator = null;
        }
    }

    private int getAnimationDuration(Workspace.State state) {
        if (state == Workspace.State.NORMAL) {
            return mAllAppsTransitionDuration;
        } else if (state == Workspace.State.OVERVIEW) {
            return mOverviewTransitionDuration;
        } else {
            return mAllAppsTransitionDuration;
        }
    }

    public float getFinalScale() {
        return mNewScale;
    }

    public void snapToPageFromOverView(int whichPage) {
        mWorkspace.snapToPage(whichPage);
    }
}

class ZoomInInterpolator implements TimeInterpolator {
    private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

    public float getInterpolation(float t) {
        return decelerate.getInterpolation(t);
    }
}
