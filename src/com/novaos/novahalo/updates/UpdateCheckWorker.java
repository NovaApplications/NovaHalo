package com.novaos.novahalo.updates;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import com.novaos.novahalo.R;

public class UpdateCheckWorker extends Worker {

    private static final String CHANNEL_ID = "nova_halo_updates";
    private static final int NOTIFICATION_ID = 1001;

    public UpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest updateCheckRequest =
                new PeriodicWorkRequest.Builder(UpdateCheckWorker.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(1, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update_check",
                ExistingPeriodicWorkPolicy.KEEP,
                updateCheckRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        final Context context = getApplicationContext();
        
        GitHubUpdateChecker.checkForUpdates(context, new GitHubUpdateChecker.UpdateCheckCallback() {
            @Override
            public void onUpdateAvailable(String version, String downloadUrl) {
                showUpdateNotification(context, version, downloadUrl);
            }

            @Override
            public void onNoUpdate() {
                // Do nothing
            }

            @Override
            public void onError(String error) {
                // Log error
            }
        });

        return Result.success();
    }

    private void showUpdateNotification(Context context, String version, String downloadUrl) {
        createNotificationChannel(context);

        // We can't easily trigger download from background notification without a custom pending intent
        // So we'll just open the settings or a direct link for now, or let them click to download.
        // Actually, we can use an action to trigger download.
        
        Intent intent = new Intent(context, com.novaos.novahalo.SettingsActivity.class);
        intent.putExtra("trigger_update", true);
        intent.putExtra("update_url", downloadUrl);
        intent.putExtra("update_version", version);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_home)
                .setContentTitle("Update Available: v" + version)
                .setContentText("A new version of " + context.getString(R.string.launcher_name) + " is available on GitHub.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Permission might be missing on Android 13+
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "App Updates";
            String description = "Notifications for " + context.getString(R.string.launcher_name) + " app updates";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
