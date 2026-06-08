package com.novaos.novahalo.settings;

import android.content.SharedPreferences;

import com.novaos.novahalo.Launcher;
import com.novaos.novahalo.LauncherAppState;
import com.novaos.novahalo.Utilities;
import com.novaos.novahalo.dragndrop.DragLayer;
import com.novaos.novahalo.dynamicui.ExtractedColors;

public class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_PREF_LIGHT_STATUS_BAR = "pref_lightStatusBar";
    private static final String KEY_PREF_PINCH_TO_OVERVIEW = "pref_pinchToOverview";
    private static final String KEY_PREF_SWIPE_DOWN_ACTION = "pref_swipeDownAction";
    private static final String KEY_PREF_HOTSEAT_EXTRACTED_COLORS = "pref_hotseatShouldUseExtractedColors";
    private static final String KEY_PREF_HAPTIC_FEEDBACK = "pref_enableHapticFeedback";
    private static final String KEY_PREF_KEEP_SCROLL_STATE = "pref_keepScrollState";
    private static final String KEY_FULL_WIDTH_SEARCHBAR = "pref_fullWidthSearchbar";
    private static final String KEY_SHOW_PIXEL_BAR = "pref_showPixelBar";
    private static final String KEY_SHOW_VOICE_SEARCH_BUTTON = "pref_showMic";
    private static final String KEY_PREF_ALL_APPS_OPACITY = "pref_allAppsOpacitySB";
    private static final String KEY_PREF_SHOW_HIDDEN_APPS = "pref_showHidden";
    private static Settings instance;
    private Launcher mLauncher;

    private Settings(Launcher launcher) {
        mLauncher = launcher;
        SharedPreferences prefs = Utilities.getPrefs(launcher);
        prefs.registerOnSharedPreferenceChangeListener(this);
        init(prefs);
    }

    public static void init(Launcher launcher) {
        instance = new Settings(launcher);
    }

    public static Settings getInstance() {
        return instance;
    }

    private void init(SharedPreferences prefs) {
        applyAllAppsOpacity(prefs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.startsWith("pref_")) {
            switch (key) {
                case KEY_PREF_LIGHT_STATUS_BAR:
                    mLauncher.activateLightStatusBar(false);
                    break;
                case KEY_PREF_PINCH_TO_OVERVIEW:
                    DragLayer dragLayer = mLauncher.getDragLayer();
                    dragLayer.onAccessibilityStateChanged(dragLayer.mIsAccesibilityEnabled);
                    break;
                case KEY_PREF_SWIPE_DOWN_ACTION:
                    mLauncher.getWorkspace().initPullDownAction();
                    break;
                case KEY_PREF_HOTSEAT_EXTRACTED_COLORS:
                    ExtractedColors ec = mLauncher.getExtractedColors();
                    mLauncher.getHotseat().updateColor(ec, true);
                    mLauncher.getWorkspace().getPageIndicator().updateColor(ec);
                    break;
                case KEY_PREF_HAPTIC_FEEDBACK:
                    mLauncher.getWorkspace().setHapticFeedbackEnabled(prefs.getBoolean(key, false));
                    break;
                case KEY_PREF_ALL_APPS_OPACITY:
                    applyAllAppsOpacity(prefs);
                    break;
                case KEY_PREF_SHOW_HIDDEN_APPS:
                    LauncherAppState.getInstance().reloadAllApps();
                    break;
                case KEY_PREF_KEEP_SCROLL_STATE:
                case KEY_SHOW_VOICE_SEARCH_BUTTON:
                    // Ignoring those as we do not need to apply anything special
                    break;
                default:
                    LauncherAppState.getInstance().reloadAll(false);
            }
        }
    }

    private void applyAllAppsOpacity(SharedPreferences prefs) {
        int tmp = (int) (prefs.getFloat(KEY_PREF_ALL_APPS_OPACITY, 1f) * 255);
        mLauncher.getAllAppsController().setAllAppsAlpha(tmp);
    }
}
