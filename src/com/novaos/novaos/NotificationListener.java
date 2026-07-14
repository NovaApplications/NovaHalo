package com.novaos.novaos;

import android.app.Notification;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.View;

import com.novaos.novaos.MainThreadExecutor;

import java.util.HashMap;
import java.util.Map;

public class NotificationListener extends NotificationListenerService {
    private final static Map<String, Integer> NOTIFICATION_COUNTS = new HashMap<>();

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        update(false);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        update(true);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        update(true);
    }

    public static int getNotificationCount(String packageName) {
        Integer count = NOTIFICATION_COUNTS.get(packageName);
        return count != null ? count : 0;
    }

    private void update(boolean reload) {
        NOTIFICATION_COUNTS.clear();
        for (StatusBarNotification sbn : getActiveNotifications()) {
            String pkg = sbn.getPackageName();
            if (sbn.isClearable() && sbn.getNotification().priority > Notification.PRIORITY_LOW) {
                int count = getNotificationCount(pkg);
                NOTIFICATION_COUNTS.put(pkg, count + 1);
            }
        }
        if (reload) {
            new com.novaos.novaos.MainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Launcher launcher = Launcher.getLauncher(getApplicationContext());
                    if (launcher != null && launcher.getWorkspace() != null) {
                        launcher.getWorkspace().mapOverItems(false, new Workspace.ItemOperator() {
                            @Override
                            public boolean evaluate(ItemInfo info, View view) {
                                if (view instanceof BubbleTextView) {
                                    view.invalidate();
                                }
                                return false;
                            }
                        });
                    }
                }
            });
        }
    }
}
