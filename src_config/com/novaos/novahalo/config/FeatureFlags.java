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

package com.novaos.novahalo.config;

import android.content.Context;

import com.novaos.novahalo.Utilities;

/**
 * Defines a set of flags used to control various launcher behaviors
 */
public final class FeatureFlags {

    private static final String KEY_PREF_LIGHT_STATUS_BAR = "pref_lightStatusBar";
    private static final String KEY_PREF_PINCH_TO_OVERVIEW = "pref_pinchToOverview";
    private static final String KEY_PREF_SWIPE_DOWN_ACTION = "pref_swipeDownAction";
    private static final String KEY_PREF_ICON_SHAPE = "pref_iconShape";
    private static final String KEY_PREF_TASKBAR_TRANSPARENCY = "pref_taskbarTransparency";
    private static final String KEY_PREF_HOTSEAT_EXTRACTED_COLORS = "pref_hotseatShouldUseExtractedColors";
    private static final String KEY_PREF_HAPTIC_FEEDBACK = "pref_enableHapticFeedback";
    private static final String KEY_PREF_KEEP_SCROLL_STATE = "pref_keepScrollState";
    private static final String KEY_FULL_WIDTH_SEARCHBAR = "pref_fullWidthSearchbar";
    private static final String KEY_SHOW_PIXEL_BAR = "pref_showPixelBar";
    public static final String KEY_SHOW_VOICE_SEARCH_BUTTON = "pref_showMic";

    private FeatureFlags() {
    }

    // Returns the action to perform on swipe down gesture on the first workspace.
    public static String swipeDownAction(Context context) {
        return Utilities.getPrefs(context).getString(KEY_PREF_SWIPE_DOWN_ACTION, "search");
    }

    public static String iconShape(Context context) {
        return Utilities.getPrefs(context).getString(KEY_PREF_ICON_SHAPE, "system");
    }

    public static float taskbarTransparency(Context context) {
        return Float.parseFloat(Utilities.getPrefs(context).getString(KEY_PREF_TASKBAR_TRANSPARENCY, "1.0"));
    }

    public static boolean pinchToOverview(Context context) {
        return Utilities.getPrefs(context).getBoolean(KEY_PREF_PINCH_TO_OVERVIEW, true);
    }

    // When enabled the status bar may show dark icons based on the top of the wallpaper.
    public static boolean lightStatusBar(Context context) {
        return Utilities.getPrefs(context).getBoolean(KEY_PREF_LIGHT_STATUS_BAR, false);
    }

    public static boolean hotseatShouldUseExtractedColors(Context context) {
        return Utilities.getPrefs(context).getBoolean(KEY_PREF_HOTSEAT_EXTRACTED_COLORS, true);
    }

    public static boolean enableHapticFeedback(Context context) {
        return Utilities.getPrefs(context).getBoolean(KEY_PREF_HAPTIC_FEEDBACK, false);
    }

    public static boolean keepScrollState(Context context) {
        return Utilities.getPrefs(context).getBoolean(KEY_PREF_KEEP_SCROLL_STATE, false);
    }

    public static boolean useFullWidthSearchbar(Context context) {
        return Utilities.getPrefs(context).getBoolean(KEY_FULL_WIDTH_SEARCHBAR, false);
    }

    public static boolean showVoiceSearchButton(Context context) {
        return false;
    }

    public static boolean showPixelBar(Context context) {
        return false;
    }
}
