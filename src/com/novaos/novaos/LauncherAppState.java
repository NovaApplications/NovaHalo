/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.novaos.novaos;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.lang.ref.WeakReference;

import com.novaos.novaos.compat.LauncherAppsCompat;
import com.novaos.novaos.compat.UserManagerCompat;
import com.novaos.novaos.dynamicui.ExtractionUtils;
import com.novaos.novaos.shortcuts.DeepShortcutManager;
import com.novaos.novaos.updates.UpdateCheckWorker;
import com.novaos.novaos.util.ConfigMonitor;
import com.novaos.novaos.util.Thunk;

public class LauncherAppState {

    @Thunk
    final LauncherModel mModel;
    private final IconCache mIconCache;
    private final WidgetPreviewLoader mWidgetCache;
    private final DeepShortcutManager mDeepShortcutManager;

    @Thunk
    boolean mWallpaperChangedSinceLastCheck;

    private static WeakReference<LauncherProvider> sLauncherProvider;
    private static Context sContext;

    private static LauncherAppState INSTANCE;

    private InvariantDeviceProfile mInvariantDeviceProfile;

    private Launcher mLauncher;

    public static LauncherAppState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return sContext;
    }

    static void setLauncherProvider(LauncherProvider provider) {
        if (sLauncherProvider != null) {
            Log.w(Launcher.TAG, "setLauncherProvider called twice! old=" +
                    sLauncherProvider.get() + " new=" + provider);
        }
        sLauncherProvider = new WeakReference<>(provider);

        // The content provider exists for the entire duration of the launcher main process and
        // is the first component to get created. Initializing application context here ensures
        // that LauncherAppState always exists in the main process.
        sContext = provider.getContext().getApplicationContext();
    }

    private LauncherAppState() {
        if (sContext == null) {
            throw new IllegalStateException("LauncherAppState inited before app context set");
        }

        Log.v(Launcher.TAG, "LauncherAppState inited");

        Utilities.restorePrefsIfEmpty(sContext);
        mInvariantDeviceProfile = new InvariantDeviceProfile(sContext);
        mIconCache = new IconCache(sContext, mInvariantDeviceProfile);
        mWidgetCache = new WidgetPreviewLoader(sContext, mIconCache);
        mDeepShortcutManager = new DeepShortcutManager(sContext);

        mModel = new LauncherModel(this, mIconCache, new StringSetAppFilter(), mDeepShortcutManager);

        LauncherAppsCompat.getInstance(sContext).addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        // For handling managed profiles
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
        if (Utilities.isNycOrAbove()) {
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);
        }
        // For extracting colors from the wallpaper
        if (Utilities.isNycOrAbove()) {
            // TODO: add a broadcast entry to the manifest for pre-N.
            filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        }

        ContextCompat.registerReceiver(sContext, mModel, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        UserManagerCompat.getInstance(sContext).enableAndResetCache();
        new ConfigMonitor(sContext).register();

        if (Utilities.isNycOrAbove()) {
            ExtractionUtils.startColorExtractionServiceIfNecessary(sContext);
        } else {
            ExtractionUtils.startColorExtractionService(sContext);
        }

        UpdateCheckWorker.schedule(sContext);
        initCrashLogging(sContext);
        Utilities.backupPrefs(sContext);
    }

    private void initCrashLogging(final Context context) {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    java.io.File logFile = new java.io.File(context.getExternalFilesDir(null), "nova_crash_logs.txt");
                    java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(logFile, true));
                    writer.println("--- CRASH LOG ---");
                    writer.println("Date: " + new java.util.Date().toString());
                    writer.println("Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
                    writer.println("Device: " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")");
                    throwable.printStackTrace(writer);
                    writer.println("\n");
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    Log.e("LauncherAppState", "Failed to write crash log", e);
                }
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }

    /**
     * Reloads the workspace items from the DB and re-binds the workspace. This should generally
     * not be called as DB updates are automatically followed by UI update
     */
    public void reloadWorkspace() {
        mModel.resetLoadedState(false, true);
        mModel.startLoaderFromBackground();
    }

    public void reloadAllApps() {
        mModel.resetLoadedState(true, false);
        mModel.startLoaderFromBackground();
    }

    public void reloadAll(boolean showWorkspace) {
        mModel.resetLoadedState(true, true);
        mModel.startLoaderFromBackground();
        mInvariantDeviceProfile.customizationHook(getContext());
        if (mLauncher != null) {
            mLauncher.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mLauncher != null && mLauncher.getHotseat() != null) {
                                mLauncher.getHotseat().refresh();
                            }
                        }
                    }
            );
        }
        if (showWorkspace && mLauncher != null) {
            mLauncher.showWorkspace(true);
        }
    }

    LauncherModel setLauncher(Launcher launcher) {
        sLauncherProvider.get().setLauncherProviderChangeListener(launcher);
        mModel.initialize(launcher);
        return mModel;
    }


    public void setMLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public WidgetPreviewLoader getWidgetCache() {
        return mWidgetCache;
    }

    public DeepShortcutManager getShortcutManager() {
        return mDeepShortcutManager;
    }

    public boolean hasWallpaperChangedSinceLastCheck() {
        boolean result = mWallpaperChangedSinceLastCheck;
        mWallpaperChangedSinceLastCheck = false;
        return result;
    }

    public InvariantDeviceProfile getInvariantDeviceProfile() {
        return mInvariantDeviceProfile;
    }
}
