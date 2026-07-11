package com.novaos.novaos.updates;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import com.novaos.novaos.BuildConfig;
import com.novaos.novaos.R;

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
                    connection.setRequestProperty("User-Agent", "NovaOS-Launcher");
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(response.toString());
                        String latestVersion = json.getString("tag_name").trim().replace("v", "");
                        
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
                        connection.disconnect();
                    } else {
                        // Log error response for debugging
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        reader.close();
                        Log.e(TAG, "Server error " + connection.getResponseCode() + ": " + errorResponse.toString());

                        if (connection.getResponseCode() == 404) {
                            checkForAllReleases(context, callback);
                        } else {
                            callback.onError("Server error: " + connection.getResponseCode());
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Update check failed", e);
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private static void checkForAllReleases(final Context context, final UpdateCheckCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/" + GITHUB_REPO + "/releases");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", "NovaOS-Launcher");

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        JSONArray releases = new JSONArray(response.toString());
                        if (releases.length() > 0) {
                            // Take the first release in the list (usually the newest created)
                            JSONObject json = releases.getJSONObject(0);
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
                            callback.onNoUpdate();
                        }
                    } else {
                        callback.onError("Server error: " + connection.getResponseCode());
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Full release check failed", e);
                    callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private static boolean isNewerVersion(String latest, String current) {
        if (latest == null || current == null) return false;

        // Clean up versions (remove 'v' prefix if present)
        String v1 = latest.toLowerCase().replace("v", "").trim();
        String v2 = current.toLowerCase().replace("v", "").trim();

        // If versions are identical, no update
        if (v1.equals(v2)) return false;

        // Extract base version and beta number
        // Expected format: "1.0.0 beta 7" or "1.0.0"
        String base1 = v1.split("beta")[0].split("-")[0].trim();
        String base2 = v2.split("beta")[0].split("-")[0].trim();

        String[] parts1 = base1.split("\\.");
        String[] parts2 = base2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? parseSafeInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseSafeInt(parts2[i]) : 0;

            if (p1 > p2) return true;
            if (p1 < p2) return false;
        }

        // Base versions are equal, check for beta status
        boolean isBeta1 = v1.contains("beta");
        boolean isBeta2 = v2.contains("beta");

        if (isBeta1 && !isBeta2) {
            // "1.0.0 beta 7" vs "1.0.0" -> latest is beta, current is stable. Stable is newer.
            return false;
        }
        if (!isBeta1 && isBeta2) {
            // "1.0.0" vs "1.0.0 beta 7" -> latest is stable, current is beta. Stable is newer.
            return true;
        }
        if (isBeta1 && isBeta2) {
            // Both are betas, compare beta numbers
            int betaNum1 = parseBetaNumber(v1);
            int betaNum2 = parseBetaNumber(v2);
            return betaNum1 > betaNum2;
        }

        return false;
    }

    private static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseBetaNumber(String version) {
        try {
            String[] parts = version.split("beta");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1].replaceAll("[^0-9]", "").trim());
            }
        } catch (Exception e) {
            // Fallback
        }
        return 0;
    }

    public static void downloadAndInstall(final Context context, String downloadUrl, String version) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                String name = context.getString(R.string.launcher_name);
                Toast.makeText(context, "Please enable 'Install unknown apps' for " + name + " and try again.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        String name = context.getString(R.string.launcher_name);
        request.setTitle(name + " Update " + version);
        request.setDescription("Downloading update...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        
        File destination = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "NovaOS-" + version + ".apk");
        if (destination.exists()) destination.delete();
        request.setDestinationUri(Uri.fromFile(destination));

        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == id) {
                    installApk(context, destination);
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

    private static void installApk(Context context, File apkFile) {
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file not found: " + apkFile.getAbsolutePath());
            return;
        }

        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }
}
