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
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        mCurrentProgress = progress;
    }

    private void launchGoogleApp() {
        try {
            // Intent to launch the Google Feed specifically if possible, 
            // otherwise fall back to the main Google app.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://feed"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mLauncher.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = mLauncher.getPackageManager().getLaunchIntentForPackage("com.google.android.googlequicksearchbox");
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mLauncher.startActivity(intent);
                }
            } catch (Exception e2) {
                // Google app not found
            }
        }
    }
}
