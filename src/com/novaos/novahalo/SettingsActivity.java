package com.novaos.novahalo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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

import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

import com.novaos.novahalo.updates.GitHubUpdateChecker;

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
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
                }
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
                        .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                GitHubUpdateChecker.downloadAndInstall(SettingsActivity.this, url, version);
                            }
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

            Preference aboutPref = findPreference("about");
            if (aboutPref != null) {
                String launcherName = getString(R.string.launcher_name);
                aboutPref.setSummary(launcherName + " Version: " + BuildConfig.VERSION_NAME);
            }

            Preference versionPref = findPreference("version");
            if (versionPref != null) {
                versionPref.setSummary(BuildConfig.VERSION_NAME);
            }

            final PreferenceCategory devCategory = (PreferenceCategory) findPreference("category_developer");
            updateDevOptionsVisibility(devCategory);

            Preference buildPref = findPreference("build");
            if (buildPref != null) {
                String dateStr = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
                String launcherName = getString(R.string.launcher_name);
                buildPref.setSummary(launcherName + "-" + dateStr + "." + String.format(java.util.Locale.US, "%04d", BuildConfig.VERSION_CODE));

                buildPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (Utilities.getPrefs(getActivity()).getBoolean("pref_dev_mode", false)) {
                            return true;
                        }
                        mDevTapCount++;
                        if (mDevTapCount >= 7) {
                            mDevTapCount = 0;
                            showDevModeDialog(devCategory);
                        } else if (mDevTapCount > 2) {
                            Toast.makeText(getActivity(), "You are now " + (7 - mDevTapCount) + " steps away from being a developer.", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
            }
        }

        private void updateDevOptionsVisibility(PreferenceCategory category) {
            if (category != null) {
                boolean isDev = Utilities.getPrefs(getActivity()).getBoolean("pref_dev_mode", false);
                category.setVisible(isDev);
                
                Preference exportPref = findPreference("pref_export_logs");
                if (exportPref != null) {
                    exportPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            exportCrashLogs();
                            return true;
                        }
                    });
                }

                Preference clearPref = findPreference("pref_clear_logs");
                if (clearPref != null) {
                    clearPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            clearCrashLogs();
                            return true;
                        }
                    });
                }
            }
        }

        private void clearCrashLogs() {
            java.io.File logFile = new java.io.File(getActivity().getExternalFilesDir(null), "nova_crash_logs.txt");
            if (logFile.exists() && logFile.delete()) {
                Toast.makeText(getActivity(), "Crash logs cleared.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "No logs to clear.", Toast.LENGTH_SHORT).show();
            }
        }

        private void exportCrashLogs() {
            java.io.File logFile = new java.io.File(getActivity().getExternalFilesDir(null), "nova_crash_logs.txt");
            if (!logFile.exists()) {
                Toast.makeText(getActivity(), "No crash logs found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                    getActivity(), getActivity().getPackageName() + ".fileprovider", logFile));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Send crash logs"));
        }

        private void showDevModeDialog(final PreferenceCategory category) {
            final EditText input = new EditText(getActivity());
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            input.setPadding(padding, padding, padding, padding);
            
            new AlertDialog.Builder(getActivity())
                .setTitle("Developer Mode")
                .setMessage("Enter password to unlock development features:")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if ("development".equals(input.getText().toString())) {
                            Utilities.getPrefs(getActivity()).edit().putBoolean("pref_dev_mode", true).apply();
                            updateDevOptionsVisibility(category);
                            Toast.makeText(getActivity(), "Developer mode enabled!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
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
                    final Context context = getActivity();
                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show();
                    
                    GitHubUpdateChecker.checkForUpdates(context, new GitHubUpdateChecker.UpdateCheckCallback() {
                        @Override
                        public void onUpdateAvailable(final String version, final String downloadUrl) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(context)
                                            .setTitle("Update Available")
                                            .setMessage("A new version (v" + version + ") is available. Would you like to update now?")
                                            .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    GitHubUpdateChecker.downloadAndInstall(context, downloadUrl, version);
                                                }
                                            })
                                            .setNegativeButton("Later", null)
                                            .show();
                                }
                            });
                        }

                        @Override
                        public void onNoUpdate() {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Nova Halo is up to date!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(final String error) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Update check failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                    return true;
                }
            }
            return super.onPreferenceTreeClick(preference);
        }
    }
}
