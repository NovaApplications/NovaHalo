package com.novaos.novaos;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

/**
 * A simple client to handle Google Feed interaction.
 * In a production launcher, this would connect to the Google app via AIDL.
 */
public class OverlayClient implements Launcher.LauncherOverlay {

    private final Launcher mLauncher;

    public OverlayClient(Launcher launcher) {
        mLauncher = launcher;
    }

    private float mCurrentProgress;

    @Override
    public void onScrollInteractionBegin() {
        mCurrentProgress = 0f;
    }

    @Override
    public void onScrollInteractionEnd() {
        // If the user swiped at least 15% of the screen, launch the feed
        if (mCurrentProgress > 0.15f) {
            launchGoogleApp();
        }
        mCurrentProgress = 0f;
        // Restore launcher alpha
        mLauncher.getWorkspace().setAlpha(1.0f);
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        mCurrentProgress = progress;
        // Fade the launcher slightly to indicate we are swiping to the feed
        mLauncher.getWorkspace().setAlpha(Math.max(0.5f, 1.0f - progress));
    }

    private void launchGoogleApp() {
        try {
            // Use the more modern and reliable intent for the Google Feed
            Intent intent = new Intent("com.google.android.googlequicksearchbox.action.SEARCH_PUBLIC")
                    .setPackage("com.google.android.googlequicksearchbox")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .putExtra("show_voice_search", false)
                    .putExtra("show_search_box", false);
            
            // Try to use a specific transition to make it feel more integrated
            mLauncher.startActivity(intent);
            // After starting the intent, we can also perform a custom override transition
            mLauncher.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
        } catch (Exception e) {
            try {
                // Fallback to the generic feed URI
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://feed"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mLauncher.startActivity(intent);
                mLauncher.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
            } catch (Exception e2) {
                // Fallback to the main search box if everything else fails
                Intent intent = mLauncher.getPackageManager().getLaunchIntentForPackage("com.google.android.googlequicksearchbox");
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mLauncher.startActivity(intent);
                }
            }
        }
    }
}
