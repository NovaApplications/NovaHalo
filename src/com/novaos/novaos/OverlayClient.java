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

    @Override
    public void onScrollInteractionBegin() {
        // Prepare for scroll
    }

    @Override
    public void onScrollInteractionEnd() {
        // End scroll
    }

    @Override
    public void onScrollChange(float progress, boolean rtl) {
        // If the user swipes far enough (e.g. 80%), launch the Google app
        if (progress > 0.8f) {
            launchGoogleApp();
        }
    }

    private void launchGoogleApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage("com.google.android.googlequicksearchbox");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mLauncher.startActivity(intent);
        } catch (Exception e) {
            // Google app not found
        }
    }
}
