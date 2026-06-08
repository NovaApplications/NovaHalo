package com.novaos.novahalo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.Toast;

public class UninstallDropTarget extends ButtonDropTarget {

    public UninstallDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UninstallDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Get the hover color
        mHoverColor = getResources().getColor(R.color.uninstall_target_hover_tint);

        setDrawable(R.drawable.ic_uninstall_launcher);
    }

    @Override
    protected boolean supportsDrop(DragSource source, ItemInfo info) {
        return supportsDrop(getContext(), info);
    }

    public static boolean supportsDrop(Context context, Object info) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bundle restrictions = userManager.getUserRestrictions();
        if (restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
                || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false)) {
            return false;
        }

        Pair<ComponentName, UserHandle> componentInfo = getComponentAndUser(info);
        if (componentInfo == null) {
            return false;
        }

        ComponentName cn = componentInfo.first;
        // Check if it's a system app
        try {
            android.content.pm.ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    cn.getPackageName(), 0);
            if ((ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                // System app. Can only uninstall if it's an update.
                return (ai.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            }
            return true;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * @return the component name and user if {@param info} is an AppInfo or an app shortcut.
     */
    private static Pair<ComponentName, UserHandle> getComponentAndUser(Object item) {
        if (item instanceof AppInfo) {
            AppInfo info = (AppInfo) item;
            return Pair.create(info.componentName, info.user);
        } else if (item instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) item;
            ComponentName component = info.getTargetComponent();
            if (info.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION
                    && component != null) {
                return Pair.create(component, info.user);
            }
        }
        return null;
    }

    @Override
    public void onDrop(DragObject d) {
        // Differ item deletion
        if (d.dragSource instanceof DropTargetSource) {
            ((DropTargetSource) d.dragSource).deferCompleteDropAfterUninstallActivity();
        }
        super.onDrop(d);
    }

    @Override
    void completeDrop(final DragObject d) {
        DropTargetResultCallback callback = d.dragSource instanceof DropTargetResultCallback
                ? (DropTargetResultCallback) d.dragSource : null;
        startUninstallActivity(mLauncher, d.dragInfo, callback);
    }

    public static boolean startUninstallActivity(Launcher launcher, ItemInfo info) {
        return startUninstallActivity(launcher, info, null);
    }

    public static boolean startUninstallActivity(
            final Launcher launcher, ItemInfo info, DropTargetResultCallback callback) {
        Pair<ComponentName, UserHandle> componentInfo = getComponentAndUser(info);
        if (componentInfo == null) {
            android.util.Log.e("UninstallDropTarget", "Failed to get component info for " + info);
            return false;
        }
        ComponentName cn = componentInfo.first;
        UserHandle user = componentInfo.second;

        final boolean isUninstallable = supportsDrop(launcher, info);
        android.util.Log.d("UninstallDropTarget", "Starting uninstall for " + cn.getPackageName() + " user=" + user + " isUninstallable=" + isUninstallable);

        if (!isUninstallable) {
            Toast.makeText(launcher, R.string.uninstall_system_app_text, Toast.LENGTH_SHORT).show();
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_DELETE,
                        Uri.fromParts("package", cn.getPackageName(), null))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(Intent.EXTRA_USER, user);
                // Also add the older extra just in case
                intent.putExtra("android.intent.extra.USER_HANDLE", user);
                launcher.startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("UninstallDropTarget", "Failed to start uninstall activity", e);
                // Fallback to older intent
                try {
                    Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                    intent.setData(Uri.parse("package:" + cn.getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_USER, user);
                    intent.putExtra("android.intent.extra.USER_HANDLE", user);
                    launcher.startActivity(intent);
                } catch (Exception e2) {
                    android.util.Log.e("UninstallDropTarget", "Fallback uninstall failed, trying App Details", e2);
                    try {
                        com.novaos.novahalo.compat.LauncherAppsCompat.getInstance(launcher)
                                .showAppDetailsForProfile(cn, user);
                    } catch (Exception e3) {
                        android.util.Log.e("UninstallDropTarget", "App Details fallback failed", e3);
                        return false;
                    }
                }
            }
        }
        if (callback != null) {
            sendUninstallResult(
                    launcher, isUninstallable, componentInfo.first, user, callback);
        }
        return isUninstallable;
    }

    /**
     * Notifies the {@param callback} whether the uninstall was successful or not.
     * <p>
     * Since there is no direct callback for an uninstall request, we check the package existence
     * when the launch resumes next time. This assumes that the uninstall activity will finish only
     * after the task is completed
     */
    protected static void sendUninstallResult(
            final Launcher launcher, boolean activityStarted,
            final ComponentName cn, final UserHandle user,
            final DropTargetResultCallback callback) {
        if (activityStarted) {
            final Runnable checkIfUninstallWasSuccess = new Runnable() {
                @Override
                public void run() {
                    String packageName = cn.getPackageName();
                    boolean uninstallSuccessful = !com.novaos.novahalo.compat.LauncherAppsCompat
                            .getInstance(launcher).isPackageEnabledForProfile(packageName, user);
                    callback.onDragObjectRemoved(uninstallSuccessful);
                }
            };
            launcher.addOnResumeCallback(checkIfUninstallWasSuccess);
        } else {
            callback.onDragObjectRemoved(false);
        }
    }

    public interface DropTargetResultCallback {
        /**
         * A drag operation was complete.
         *
         * @param isRemoved true if the drag object should be removed, false otherwise.
         */
        void onDragObjectRemoved(boolean isRemoved);
    }

    /**
     * Interface defining an object that can provide uninstallable drag objects.
     */
    public interface DropTargetSource extends DropTargetResultCallback {

        /**
         * Indicates that an uninstall request are made and the actual result may come
         * after some time.
         */
        void deferCompleteDropAfterUninstallActivity();
    }
}
