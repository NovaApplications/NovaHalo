package com.novaos.novahalo.updates;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import androidx.core.content.FileProvider;

import com.novaos.novahalo.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubUpdateChecker {

    private static final String TAG = "GitHubUpdateChecker";
    private static final String GITHUB_REPO = "NovaApplications/NovaHalo";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    public interface UpdateCheckCallback {
        void onUpdateAvailable(String version, String downloadUrl);
        void onNoUpdate();
        void onError(String error);
    }

    public static void checkForUpdates(final Context context, final UpdateCheckCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(response.toString());
                        String latestVersion = json.getString("tag_name").replace("v", "");
                        
                        if (isNewerVersion(latestVersion, BuildConfig.VERSION_NAME)) {
                            JSONArray assets = json.getJSONArray("assets");
                            String downloadUrl = null;
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                if (asset.getString("name").endsWith(".apk")) {
                                    downloadUrl = asset.getString("browser_download_url");
                                    break;
                                }
                            }
                            if (downloadUrl != null) {
                                callback.onUpdateAvailable(latestVersion, downloadUrl);
                            } else {
                                callback.onNoUpdate();
                            }
                        } else {
                            callback.onNoUpdate();
                        }
                    } else {
                        callback.onError("Server error: " + connection.getResponseCode());
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Update check failed", e);
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private static boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }
        return false;
    }

    public static void downloadAndInstall(final Context context, String downloadUrl, String version) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("Nova Halo Update " + version);
        request.setDescription("Downloading update...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NovaHalo-" + version + ".apk");

        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == id) {
                    installApk(context, downloadId);
                    context.unregisterReceiver(this);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private static void installApk(Context context, long downloadId) {
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(downloadId));
        if (cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (statusIndex != -1) {
                int status = cursor.getInt(statusIndex);
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    if (uriIndex != -1) {
                        String uriString = cursor.getString(uriIndex);
                        Uri apkUri = Uri.parse(uriString);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            String path = apkUri.getPath();
                            if (path != null) {
                                File downloadFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                        new File(path).getName());
                                apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", downloadFile);
                            }
                        }

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(intent);
                    }
                }
            }
        }
        cursor.close();
    }
}
