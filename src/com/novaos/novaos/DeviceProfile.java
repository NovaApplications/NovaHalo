/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import java.util.ArrayList;

public class DeviceProfile {

    public interface LauncherLayoutChangeListener {
        void onLauncherLayoutChanged();
    }

    public final InvariantDeviceProfile inv;

    // Device properties
    public final boolean isTablet;
    public final boolean isLargeTablet;
    public final boolean isPhone;

    // Device properties in current orientation
    public final int widthPx;
    public final int heightPx;
    public int availableWidthPx;
    public int availableHeightPx;

    public int numColumns;
    public int numRows;
    public int numHotseatIcons;
    /**
     * The maximum amount of left/right workspace padding as a percentage of the screen width.
     * To be clear, this means that up to 7% of the screen width can be used as left padding, and
     * 7% of the screen width can be used as right padding.
     */
    private static final float MAX_HORIZONTAL_PADDING_PERCENT = 0.14f;

    // Overview mode
    private final int overviewModeMinIconZoneHeightPx;
    private final int overviewModeMaxIconZoneHeightPx;
    private final int overviewModeBarItemWidthPx;
    private final int overviewModeBarSpacerWidthPx;
    private final float overviewModeIconZoneRatio;

    // Workspace
    private int desiredWorkspaceLeftRightMarginPx;
    public final int edgeMarginPx;
    public final Rect defaultWidgetPadding;
    private final int defaultPageSpacingPx;
    private final int topWorkspacePadding;
    private float dragViewScale;
    public float workspaceSpringLoadShrinkFactor;
    public final int workspaceSpringLoadedBottomSpace;

    // Page indicator
    private final int pageIndicatorHeightPx;

    // Workspace icons
    public int iconSizePx;
    public int iconTextSizePx;
    public int iconDrawablePaddingPx;
    public int iconDrawablePaddingOriginalPx;

    public int cellWidthPx;
    public int cellHeightPx;

    // Folder
    public int folderBackgroundOffset;
    public int folderIconSizePx;
    public int folderIconPreviewPadding;
    public int folderCellWidthPx;
    public int folderCellHeightPx;
    public int folderChildDrawablePaddingPx;

    // Hotseat
    public int hotseatCellWidthPx;
    public int hotseatCellHeightPx;
    public int hotseatIconSizePx;
    private int hotseatBarHeightPx;
    private int hotseatBarTopPaddingPx;

    // All apps
    public int allAppsButtonVisualSize;
    public int allAppsIconSizePx;
    public int allAppsIconDrawablePaddingPx;
    public float allAppsIconTextSizePx;

    // Drop Target
    public int dropTargetBarSizePx;

    // Insets
    private Rect mInsets = new Rect();

    // Listeners
    private ArrayList<LauncherLayoutChangeListener> mListeners = new ArrayList<>();

    public DeviceProfile(Context context, InvariantDeviceProfile inv,
                         Point minSize, Point maxSize,
                         int width, int height) {

        this.inv = inv;

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Constants from resources
        isTablet = res.getBoolean(R.bool.is_tablet);
        isLargeTablet = res.getBoolean(R.bool.is_large_tablet);
        isPhone = !isTablet && !isLargeTablet;

        // Some more constants
        ComponentName cn = new ComponentName(context.getPackageName(),
                this.getClass().getName());
        defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null);
        edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        desiredWorkspaceLeftRightMarginPx = edgeMarginPx;
        pageIndicatorHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_height);
        defaultPageSpacingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        topWorkspacePadding =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_top_padding);
        overviewModeMinIconZoneHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_min_icon_zone_height);
        overviewModeMaxIconZoneHeightPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_max_icon_zone_height);
        overviewModeBarItemWidthPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_item_width);
        overviewModeBarSpacerWidthPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_spacer_width);
        overviewModeIconZoneRatio =
                res.getInteger(R.integer.config_dynamic_grid_overview_icon_zone_percentage) / 100f;
        iconDrawablePaddingOriginalPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        dropTargetBarSizePx = res.getDimensionPixelSize(R.dimen.dynamic_grid_drop_target_size);
        workspaceSpringLoadedBottomSpace =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_min_spring_loaded_space);
        hotseatBarHeightPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_height);
        hotseatBarTopPaddingPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_top_padding);

        // Determine sizes.
        widthPx = width;
        heightPx = height;
        availableWidthPx = width;
        availableHeightPx = height;

        numRows = inv.numRows;
        numColumns = inv.numColumns;
        numHotseatIcons = inv.numHotseatIcons;

        // Calculate the remaining vars
        updateAvailableDimensions(dm, res);
        computeAllAppsButtonSize(context);
    }

    public void addLauncherLayoutChangedListener(LauncherLayoutChangeListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeLauncherLayoutChangedListener(LauncherLayoutChangeListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    /**
     * Determine the exact visual footprint of the all apps button, taking into account scaling
     * and internal padding of the drawable.
     */
    private void computeAllAppsButtonSize(Context context) {
        Resources res = context.getResources();
        float padding = res.getInteger(R.integer.config_allAppsButtonPaddingPercent) / 100f;
        allAppsButtonVisualSize = (int) (hotseatIconSizePx * (1 - padding)) - context.getResources()
                .getDimensionPixelSize(R.dimen.all_apps_button_scale_down);
    }

    private void updateAvailableDimensions(DisplayMetrics dm, Resources res) {
        // Check to see if the icons fit in the new available height.  If not, then we need to
        // shrink the icon size.
        float scale = 1f;
        int drawablePadding = iconDrawablePaddingOriginalPx;
        updateIconSize(1f, drawablePadding, res, dm);
        float usedHeight = (cellHeightPx * numRows);

        int maxHeight = (availableHeightPx - getTotalWorkspacePadding().y);
        if (usedHeight > maxHeight && usedHeight > 0) {
            scale = maxHeight / usedHeight;
            drawablePadding = 0;
        }
        updateIconSize(scale, drawablePadding, res, dm);
    }

    private void updateIconSize(float scale, int drawablePadding, Resources res,
                                DisplayMetrics dm) {
        iconSizePx = (int) (Utilities.pxFromDp(inv.iconSize, dm) * scale);
        iconTextSizePx = (int) (Utilities.pxFromSp(inv.iconTextSize, dm) * scale);
        iconDrawablePaddingPx = drawablePadding;
        hotseatIconSizePx = (int) (Utilities.pxFromDp(inv.hotseatIconSize, dm) * scale);
        allAppsIconSizePx = iconSizePx;
        allAppsIconDrawablePaddingPx = iconDrawablePaddingPx;
        allAppsIconTextSizePx = iconTextSizePx;

        // Calculate cell sizes to fill the available width
        cellWidthPx = availableWidthPx / numColumns;
        int textHeight = Utilities.calculateTextHeight(iconTextSizePx);
        
        int padding = iconDrawablePaddingPx;
        if (!isPhone) {
            padding = (int) (padding * 1.5f);
        }
        
        cellHeightPx = iconSizePx + padding + textHeight;
        if (!isPhone) {
            cellHeightPx += Utilities.pxFromDp(12, dm);
        }

        dragViewScale = iconSizePx;

        // Hotseat
        hotseatCellWidthPx = iconSizePx;
        hotseatCellHeightPx = iconSizePx;

        int expectedWorkspaceHeight = Math.max(1, availableHeightPx - hotseatBarHeightPx
                - pageIndicatorHeightPx - topWorkspacePadding);
        float minRequiredHeight = dropTargetBarSizePx + workspaceSpringLoadedBottomSpace;
        workspaceSpringLoadShrinkFactor = Math.min(
                res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f,
                1 - (minRequiredHeight / expectedWorkspaceHeight));

        // Folder cell
        int cellPaddingX = res.getDimensionPixelSize(R.dimen.folder_cell_x_padding);
        int cellPaddingY = res.getDimensionPixelSize(R.dimen.folder_cell_y_padding);
        final int folderChildTextSize =
                Utilities.calculateTextHeight(res.getDimension(R.dimen.folder_child_text_size));

        final int folderBottomPanelSize =
                res.getDimensionPixelSize(R.dimen.folder_label_padding_top)
                        + res.getDimensionPixelSize(R.dimen.folder_label_padding_bottom)
                        + Utilities.calculateTextHeight(res.getDimension(R.dimen.folder_label_text_size));

        // Don't let the folder get too close to the edges of the screen.
        try {
            folderCellWidthPx = Math.min(iconSizePx + 2 * cellPaddingX,
                    (availableWidthPx - 4 * edgeMarginPx) / Math.max(1, inv.numFolderColumns));
            folderCellHeightPx = Math.min(iconSizePx + 3 * cellPaddingY + folderChildTextSize,
                    (availableHeightPx - 4 * edgeMarginPx - folderBottomPanelSize) / Math.max(1, inv.numFolderRows));
        } catch (ArithmeticException e) {
            Log.e("DeviceProfile", "Divide by zero! availableWidthPx=" + availableWidthPx + " edgeMarginPx=" + edgeMarginPx + " numFolderColumns=" + inv.numFolderColumns);
            throw e;
        }
        folderChildDrawablePaddingPx = Math.max(0,
                (folderCellHeightPx - iconSizePx - folderChildTextSize) / 3);

        // Folder icon
        folderBackgroundOffset = -edgeMarginPx;
        folderIconSizePx = iconSizePx + 2 * -folderBackgroundOffset;
        folderIconPreviewPadding = res.getDimensionPixelSize(R.dimen.folder_preview_padding);
    }

    public void updateInsets(Rect insets) {
        mInsets.set(insets);
    }

    public Point getCellSize() {
        Point result = new Point();
        // Since we are only concerned with the overall padding, layout direction does
        // not matter.
        Point padding = getTotalWorkspacePadding();
        result.x = calculateCellWidth(availableWidthPx - padding.x, numColumns);
        result.y = calculateCellHeight(availableHeightPx - padding.y, numRows);
        return result;
    }

    public Point getTotalWorkspacePadding() {
        Rect padding = getWorkspacePadding(null);
        return new Point(padding.left + padding.right, padding.top + padding.bottom);
    }

    /**
     * Returns the workspace padding in the specified orientation.
     * Note that it assumes that while in verticalBarLayout, the nav bar is on the right, as such
     * this value is not reliable.
     * Use {@link #getTotalWorkspacePadding()} instead.
     */
    public Rect getWorkspacePadding(Rect recycle) {
        Rect padding = recycle == null ? new Rect() : recycle;
        int paddingBottom = hotseatBarHeightPx + pageIndicatorHeightPx;

        // Simple phone-style padding that fits the screen
        padding.set(edgeMarginPx,
                topWorkspacePadding,
                edgeMarginPx,
                paddingBottom);

        return padding;
    }

    /**
     * @return the bounds for which the open folders should be contained within
     */
    public Rect getAbsoluteOpenFolderBounds() {
        // Folders should only appear below the drop target bar and above the hotseat
        return new Rect(mInsets.left,
                mInsets.top + dropTargetBarSizePx + edgeMarginPx,
                mInsets.left + availableWidthPx,
                mInsets.top + availableHeightPx - hotseatBarHeightPx - pageIndicatorHeightPx -
                        edgeMarginPx);
    }

    private int getWorkspacePageSpacing() {
        // We want the pages spaced such that there is no
        // overhang of the previous / next page into the current page viewport.
        // We assume symmetrical padding in portrait mode.
        return Math.max(defaultPageSpacingPx, getWorkspacePadding(null).left + 1);
    }

    public static int calculateCellWidth(int width, int countX) {
        return width / Math.max(1, countX);
    }

    public static int calculateCellHeight(int height, int countY) {
        return height / Math.max(1, countY);
    }

    boolean shouldFadeAdjacentWorkspaceScreens() {
        return isLargeTablet;
    }

    private int getVisibleChildCount(ViewGroup parent) {
        int visibleChildren = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getVisibility() != View.GONE) {
                visibleChildren++;
            }
        }
        return visibleChildren;
    }

    public void layout(Launcher launcher, boolean notifyListeners) {
        FrameLayout.LayoutParams lp;

        Resources res = launcher.getResources();
        boolean isLandscape = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        // Dynamic grid adjustment for tablets
        if (isTablet || isLargeTablet) {
            // Adjust rows/columns for tablet landscape/portrait
            if (isLandscape) {
                numColumns = inv.numColumns + 2; // Add 2 columns in landscape for tablets
                numRows = Math.max(1, inv.numRows - 1); // Maybe one less row to fit search bar/dock better
            } else {
                numColumns = inv.numColumns;
                numRows = inv.numRows;
            }
            numHotseatIcons = numColumns; // Match hotseat to columns
        } else {
            // For phones, stick to invariant profile
            numColumns = inv.numColumns;
            numRows = inv.numRows;
            numHotseatIcons = inv.numHotseatIcons;
        }

        // Recalculate cell dimensions for the current screen size and orientation
        int currentWidth = isLandscape ? Math.max(widthPx, heightPx) : Math.min(widthPx, heightPx);
        int currentHeight = isLandscape ? Math.min(widthPx, heightPx) : Math.max(widthPx, heightPx);

        // Update available dimensions for the current orientation
        availableWidthPx = currentWidth - mInsets.left - mInsets.right;
        availableHeightPx = currentHeight - mInsets.top - mInsets.bottom;
        
        // Update icon and text sizes for the new grid if it changed
        updateIconSize(1f, iconDrawablePaddingOriginalPx, res, res.getDisplayMetrics());

        // Cell width should be the width of the screen divided by columns, minus small edge margins
        cellWidthPx = (availableWidthPx - (2 * edgeMarginPx)) / numColumns;
        
        // Layout the search bar space
        Point searchBarBounds = getSearchBarDimensForWidgetOpts(availableWidthPx);
        View searchBar = launcher.getDropTargetBar();
        lp = (FrameLayout.LayoutParams) searchBar.getLayoutParams();
        lp.width = searchBarBounds.x;
        lp.height = searchBarBounds.y;
        lp.topMargin = mInsets.top + edgeMarginPx;
        searchBar.setLayoutParams(lp);

        // Layout the workspace
        PagedView workspace = (PagedView) launcher.findViewById(R.id.workspace);
        
        // Ensure CellLayouts are updated with the current column count and dimensions
        for (int i = 0; i < workspace.getChildCount(); i++) {
            View child = workspace.getChildAt(i);
            if (child instanceof CellLayout) {
                CellLayout cl = (CellLayout) child;
                cl.setGridSize(numColumns, numRows);
                cl.setCellDimensions(cellWidthPx, cellHeightPx);
            }
        }

        Rect workspacePadding = getWorkspacePadding(null);
        workspace.setPadding(workspacePadding.left, workspacePadding.top, workspacePadding.right,
                workspacePadding.bottom);
        workspace.setPageSpacing(getWorkspacePageSpacing());

        lp.topMargin = mInsets.top + workspacePadding.top;

        // Layout the hotseat
        Hotseat hotseat = (Hotseat) launcher.findViewById(R.id.hotseat);
        lp = (FrameLayout.LayoutParams) hotseat.getLayoutParams();
        
        // Align hotseat with workspace
        float workspaceCellWidth = (float) (availableWidthPx - 2 * edgeMarginPx) / numColumns;
        float hotseatCellWidth = (float) (availableWidthPx - 2 * edgeMarginPx) / numHotseatIcons;
        int hotseatAdjustment = Math.round((workspaceCellWidth - hotseatCellWidth) / 2);

        lp.gravity = Gravity.BOTTOM;
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = hotseatBarHeightPx + mInsets.bottom;
        hotseat.getLayout().setPadding(hotseatAdjustment + workspacePadding.left,
                hotseatBarTopPaddingPx, hotseatAdjustment + workspacePadding.right,
                mInsets.bottom);
        hotseat.setLayoutParams(lp);

        // Layout the page indicators
        View pageIndicator = launcher.findViewById(R.id.page_indicator);
        if (pageIndicator != null) {
            lp = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
            // Put the page indicators above the hotseat
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            lp.height = pageIndicatorHeightPx;
            lp.bottomMargin = hotseatBarHeightPx + mInsets.bottom;
            pageIndicator.setLayoutParams(lp);
        }

        // Layout the Overview Mode
        ViewGroup overviewMode = launcher.getOverviewPanel();
        if (overviewMode != null) {
            lp = (FrameLayout.LayoutParams) overviewMode.getLayoutParams();
            lp.gravity = Gravity.START | Gravity.BOTTOM;

            int visibleChildCount = getVisibleChildCount(overviewMode);
            int totalItemWidth = visibleChildCount * overviewModeBarItemWidthPx;
            int maxWidth = totalItemWidth + (visibleChildCount - 1) * overviewModeBarSpacerWidthPx;

            lp.width = Math.min(availableWidthPx, maxWidth);
            lp.height = getOverviewModeButtonBarHeight(availableHeightPx);
            // Center the overview buttons on the workspace page
            lp.leftMargin = workspacePadding.left + (availableWidthPx -
                    workspacePadding.left - workspacePadding.right - lp.width) / 2;
            overviewMode.setLayoutParams(lp);
        }

        if (notifyListeners) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onLauncherLayoutChanged();
            }
        }
    }

    public Point getSearchBarDimensForWidgetOpts(int availableWidth) {
        int gap = desiredWorkspaceLeftRightMarginPx - defaultWidgetPadding.right;
        return new Point(availableWidth - 2 * gap, dropTargetBarSizePx);
    }

    int getOverviewModeButtonBarHeight() {
        return getOverviewModeButtonBarHeight(availableHeightPx);
    }

    int getOverviewModeButtonBarHeight(int availableHeight) {
        int zoneHeight = (int) (overviewModeIconZoneRatio * availableHeight);
        zoneHeight = Math.min(overviewModeMaxIconZoneHeightPx,
                Math.max(overviewModeMinIconZoneHeightPx, zoneHeight));
        return zoneHeight;
    }

    private int getCurrentWidth() {
        return Math.min(widthPx, heightPx);
    }

    private int getCurrentHeight() {
        return Math.max(widthPx, heightPx);
    }


    /**
     * @return the left/right paddings for all containers.
     */
    public final int[] getContainerPadding() {
        if (isPhone) {
            return new int[]{0, 0};
        } else {
            // Optimized padding for tablets to center content better
            int padding = (int) (availableWidthPx * 0.05f);
            return new int[]{padding, padding};
        }
    }
}
