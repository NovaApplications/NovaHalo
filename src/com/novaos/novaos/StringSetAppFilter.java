package com.novaos.novaos;

import android.content.ComponentName;
import android.content.Context;

public class StringSetAppFilter implements AppFilter {
    @Override
    public boolean shouldShowApp(ComponentName app, Context context) {
        if (app != null && app.getPackageName().equals(context.getPackageName())) {
            // Hide the main Launcher icon from the app drawer to avoid redundancy,
            // but keep the SettingsActivity visible as requested.
            return app.getClassName().equals("com.novaos.novaos.SettingsActivity");
        }
        return true;
    }
}
