package com.novaos.novahalo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

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
                            showDevModeDialog();
                        } else if (mDevTapCount > 2) {
                            Toast.makeText(getActivity(), "You are now " + (7 - mDevTapCount) + " steps away from being a developer.", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
            }
        }

        private void showDevModeDialog() {
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
                    String packageName = getActivity().getPackageName();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                    }
                    return true;
                }
            }
            return super.onPreferenceTreeClick(preference);
        }
    }
}
