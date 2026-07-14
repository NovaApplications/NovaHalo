package com.novaos.novaos;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.novaos.novaos.updates.GitHubUpdateChecker;

/**
 * Settings activity for Launcher.
 */
public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        // Display the fragment as the main content.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, new LauncherSettingsFragment())
                    .commit();
        }

        // Handle Back/Up button visibility dynamically
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
            }
        });

        // Initialize Up button visibility
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
        }

        handleUpdateIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleUpdateIntent(intent);
    }

    private void handleUpdateIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("trigger_update", false)) {
            final String url = intent.getStringExtra("update_url");
            final String version = intent.getStringExtra("update_version");
            if (url != null && version != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Update Available")
                        .setMessage("A new version (v" + version + ") is available. Would you like to update now?")
                        .setPositiveButton("Update", (dialog, which) -> {
                            GitHubUpdateChecker.downloadAndInstall(SettingsActivity.this, url, version);
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return true;
        }
        finish(); // Final back press exits settings
        return true;
    }

    @Override
    public boolean onPreferenceStartScreen(@NonNull PreferenceFragmentCompat caller, PreferenceScreen pref) {
        // Instantiate a new fragment instance
        LauncherSettingsFragment fragment = new LauncherSettingsFragment();
        
        // Pass the key of the clicked PreferenceScreen as the root key argument
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(args);
        
        // Replace current fragment and add to back stack to handle system back navigation
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(pref.getKey())
                .commit();
                
        return true;
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragmentCompat {

        private int mDevTapCount = 0;

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_preferences, rootKey);

            if (rootKey == null) {
                // Main screen
                final Preference devScreen = findPreference("category_developer");
                updateDevOptionsVisibility(devScreen);
            } else if (rootKey.equals("pref_screen_about")) {
                // About screen
                Preference aboutPref = findPreference("about");
                if (aboutPref != null) {
                    String launcherName = getString(R.string.launcher_name);
                    aboutPref.setSummary(launcherName + " Version: " + BuildConfig.VERSION_NAME);
                }

                Preference versionPref = findPreference("version");
                if (versionPref != null) {
                    versionPref.setSummary(BuildConfig.VERSION_NAME);
                }

                Preference buildPref = findPreference("build");
                if (buildPref != null) {
                    buildPref.setSummary(BuildConfig.BUILD_NUMBER);

                    buildPref.setOnPreferenceClickListener(preference -> {
                        Context context = getContext();
                        if (context != null && Utilities.getPrefs(context).getBoolean("pref_dev_mode", false)) {
                            return true;
                        }
                        mDevTapCount++;
                        if (mDevTapCount >= 7) {
                            mDevTapCount = 0;
                            showDevModeDialog(null); // No need to refresh immediately here
                        } else if (mDevTapCount > 2) {
                            Context c = getContext();
                            if (c != null) {
                                Toast.makeText(c, "You are now " + (7 - mDevTapCount) + " steps away from being a developer.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        return true;
                    });
                }
            } else if (rootKey.equals("category_developer")) {
                // Developer screen
                updateDevOptionsVisibility(null);
                
                Preference devModePref = findPreference("pref_dev_mode");
                if (devModePref != null) {
                    devModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (newValue instanceof Boolean && !((Boolean) newValue)) {
                            // Developer mode turned off, maybe show a toast or exit the screen
                            Toast.makeText(getContext(), "Developer mode disabled.", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    });
                }
            }

            // Listen for shape changes to reload the launcher
            Preference iconShapePref = findPreference("pref_iconShape");
            if (iconShapePref != null) {
                iconShapePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    reloadLauncher();
                    return true;
                });
            }

            Preference folderShapePref = findPreference("pref_folderShape");
            if (folderShapePref != null) {
                folderShapePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    reloadLauncher();
                    return true;
                });
            }

            Preference pixelBarPref = findPreference("pref_showPixelBar");
            if (pixelBarPref != null) {
                pixelBarPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    reloadLauncher();
                    return true;
                });
            }

            if (rootKey != null && rootKey.equals("pref_screen_at_a_glance")) {
                Preference.OnPreferenceChangeListener reloadListener = (preference, newValue) -> {
                    reloadLauncher();
                    return true;
                };
                Preference showDate = findPreference("pref_pixel_bar_show_date");
                if (showDate != null) showDate.setOnPreferenceChangeListener(reloadListener);
                Preference showTime = findPreference("pref_pixel_bar_show_time");
                if (showTime != null) showTime.setOnPreferenceChangeListener(reloadListener);
                Preference showBattery = findPreference("pref_pixel_bar_show_battery");
                if (showBattery != null) showBattery.setOnPreferenceChangeListener(reloadListener);
                Preference showMusic = findPreference("pref_pixel_bar_show_music");
                if (showMusic != null) showMusic.setOnPreferenceChangeListener(reloadListener);
            }

            if (rootKey != null && rootKey.equals("pref_screen_notifications")) {
                Preference notificationDotsPref = findPreference("pref_notification_dots");
                if (notificationDotsPref != null) {
                    notificationDotsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (newValue instanceof Boolean && (Boolean) newValue) {
                            if (!isNotificationServiceEnabled()) {
                                showNotificationAccessDialog();
                                return false;
                            }
                        }
                        return true;
                    });
                }

                Preference dotTypePref = findPreference("pref_notification_dot_type");
                Preference dotColorPref = findPreference("pref_notification_dot_color");
                if (dotTypePref != null && dotColorPref != null) {
                    dotColorPref.setVisible("dot".equals(Utilities.getPrefs(getContext()).getString("pref_notification_dot_type", "dot")));
                    dotTypePref.setOnPreferenceChangeListener((preference, newValue) -> {
                        dotColorPref.setVisible("dot".equals(newValue));
                        return true;
                    });
                }
            }
        }

        private boolean isNotificationServiceEnabled() {
            String pkgName = getContext().getPackageName();
            final String flat = android.provider.Settings.Secure.getString(getContext().getContentResolver(),
                    "enabled_notification_listeners");
            if (!android.text.TextUtils.isEmpty(flat)) {
                final String[] names = flat.split(":");
                for (String name : names) {
                    final ComponentName cn = ComponentName.unflattenFromString(name);
                    if (cn != null) {
                        if (android.text.TextUtils.equals(pkgName, cn.getPackageName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void showNotificationAccessDialog() {
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Notification Access Required")
                    .setMessage("NovaOS needs notification access to show dots on icons. Please enable it in the next screen.")
                    .setPositiveButton("Settings", (dialog, which) -> {
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void reloadLauncher() {
            LauncherAppState.getInstance().reloadAll(true);
        }

        private void updateDevOptionsVisibility(Preference category) {
            if (category != null) {
                // This is called for the main screen to hide/show the "Developer" entry
                boolean isDev = Utilities.getPrefs(getActivity()).getBoolean("pref_dev_mode", false);
                category.setVisible(isDev);
            }
            
            // This part handles the buttons inside the developer screen
            Preference exportPref = findPreference("pref_export_logs");
            if (exportPref != null) {
                exportPref.setOnPreferenceClickListener(preference -> {
                    exportCrashLogs();
                    return true;
                });
            }

            Preference clearPref = findPreference("pref_clear_logs");
            if (clearPref != null) {
                clearPref.setOnPreferenceClickListener(preference -> {
                    clearCrashLogs();
                    return true;
                });
            }
        }

        private void clearCrashLogs() {
            Context context = getContext();
            if (context == null) return;
            java.io.File logFile = new java.io.File(context.getExternalFilesDir(null), "nova_crash_logs.txt");
            if (logFile.exists() && logFile.delete()) {
                Toast.makeText(context, "Crash logs cleared.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "No logs to clear.", Toast.LENGTH_SHORT).show();
            }
        }

        private void exportCrashLogs() {
            Context context = getContext();
            if (context == null) return;
            java.io.File logFile = new java.io.File(context.getExternalFilesDir(null), "nova_crash_logs.txt");
            if (!logFile.exists()) {
                Toast.makeText(context, "No crash logs found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                    context, context.getPackageName() + ".fileprovider", logFile));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Send crash logs"));
        }

        private void showDevModeDialog(final Preference category) {
            final EditText input = new EditText(getActivity());
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            input.setPadding(padding, padding, padding, padding);
            
            new AlertDialog.Builder(getActivity())
                .setTitle("Developer Mode")
                .setMessage("Enter password to unlock development features:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    Context context = getContext();
                    if (context != null && "development".equals(input.getText().toString())) {
                        Utilities.getPrefs(context).edit().putBoolean("pref_dev_mode", true).apply();
                        if (category != null) {
                            updateDevOptionsVisibility(category);
                        }
                        Toast.makeText(context, "Developer mode enabled!", Toast.LENGTH_SHORT).show();
                    } else if (context != null) {
                        Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        @Override
        public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            androidx.recyclerview.widget.RecyclerView recyclerView = getListView();
            if (recyclerView != null && getContext() != null) {
                recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                        getContext(), androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (getPreferenceScreen() != null && getActivity() != null) {
                getActivity().setTitle(getPreferenceScreen().getTitle());
            }
        }

        @Override
        public boolean onPreferenceTreeClick(@NonNull Preference preference) {
            String key = preference.getKey();
            if (key != null) {
                if (key.equals("about")) {
                    return true;
                } else if (key.equals("check_for_update") && getActivity() != null) {
                    final Context context = getContext();
                    if (context == null) return true;
                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show();
                    
                    GitHubUpdateChecker.checkForUpdates(context, new GitHubUpdateChecker.UpdateCheckCallback() {
                        @Override
                        public void onUpdateAvailable(@NonNull final String version, @NonNull final String downloadUrl) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                Context c = getContext();
                                if (c == null) return;
                                new AlertDialog.Builder(c)
                                        .setTitle("Update Available")
                                        .setMessage("A new version (v" + version + ") is available. Would you like to update now?")
                                        .setPositiveButton("Update", (dialog, which) -> GitHubUpdateChecker.downloadAndInstall(c, downloadUrl, version))
                                        .setNegativeButton("Later", null)
                                        .show();
                            });
                        }

                        @Override
                        public void onNoUpdate() {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                Context c = getContext();
                                if (c != null) {
                                    String name = getString(R.string.launcher_name);
                                    Toast.makeText(c, name + " is up to date!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull final String error) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                Context c = getContext();
                                if (c != null) {
                                    Toast.makeText(c, "Update check failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                    return true;
                } else if (key.equals("whats_new") && getActivity() != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://novaapplications.github.io/NovaHalo/#releases")));
                    return true;
                } else if (key.equals("reset_launcher") && getActivity() != null) {
                    showResetConfirmationDialog();
                    return true;
                }
            }
            return super.onPreferenceTreeClick(preference);
        }

        private void showResetConfirmationDialog() {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.reset_launcher_title)
                    .setMessage(R.string.reset_launcher_confirmation)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> resetLauncher())
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }

        private void resetLauncher() {
            Context context = getContext();
            if (context == null) return;

            // Clear SharedPreferences
            Utilities.getPrefs(context).edit().clear().commit();

            // Clear Database
            context.getContentResolver().delete(LauncherSettings.Favorites.CONTENT_URI, null, null);

            // Restart Launcher
            Toast.makeText(context, "Launcher reset. Restarting...", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(context, Launcher.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            
            if (getActivity() != null) {
                getActivity().finish();
            }
            
            // Still kill the process after a short delay to ensure everything is cleared, 
            // but the new activity will start in a clean state.
            new Handler().postDelayed(() -> android.os.Process.killProcess(android.os.Process.myPid()), 500);
        }
    }
}
