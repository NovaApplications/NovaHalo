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

package com.novaos.novaos.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.novaos.novaos.FolderInfo;
import com.novaos.novaos.ItemInfo;
import com.novaos.novaos.LauncherAppState;
import com.novaos.novaos.LauncherFiles;
import com.novaos.novaos.LauncherModel;
import com.novaos.novaos.MainThreadExecutor;
import com.novaos.novaos.R;
import com.novaos.novaos.ShortcutInfo;
import com.novaos.novaos.Utilities;
import com.novaos.novaos.compat.LauncherActivityInfoCompat;
import com.novaos.novaos.compat.UserManagerCompat;
import com.novaos.novaos.shortcuts.ShortcutInfoCompat;

/**
 * Handles addition of app shortcuts for managed profiles.
 * Methods of class should only be called on {@link LauncherModel#sWorkerThread}.
 */
public class ManagedProfileHeuristic {

    /**
     * Maintain a set of packages installed per user.
     */
    private static final String INSTALLED_PACKAGES_PREFIX = "installed_packages_for_user_";

    private static final String USER_FOLDER_ID_PREFIX = "user_folder_";

    /**
     * Duration (in milliseconds) for which app shortcuts will be added to work folder.
     */
    private static final long AUTO_ADD_TO_FOLDER_DURATION = 8 * 60 * 60 * 1000;

    public static ManagedProfileHeuristic get(Context context, UserHandle user) {
        if (!Utilities.myUserHandle().equals(user)) {
            return new ManagedProfileHeuristic(context, user);
        }
        return null;
    }

    private final Context mContext;
    private final LauncherModel mModel;
    private final UserHandle mUser;
    private final UserManagerCompat mUserManager;
    private final SharedPreferences mPrefs;

    private ManagedProfileHeuristic(Context context, UserHandle user) {
        mContext = context;
        mUser = user;
        mModel = LauncherAppState.getInstance().getModel();
        mUserManager = UserManagerCompat.getInstance(context);
        mPrefs = context.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY,
                Context.MODE_PRIVATE);
    }

    public void processPackageRemoved(String[] packages) {
        ManagedProfilePackageHandler handler = new ManagedProfilePackageHandler();
        for (String pkg : packages) {
            handler.onPackageRemoved(pkg, mUser);
        }
    }

    public void processPackageAdd(String[] packages) {
        ManagedProfilePackageHandler handler = new ManagedProfilePackageHandler();
        for (String pkg : packages) {
            handler.onPackageAdded(pkg, mUser);
        }
    }

    public void processUserApps(List<LauncherActivityInfoCompat> apps) {
        String folderIdKey = USER_FOLDER_ID_PREFIX + mUserManager.getSerialNumberForUser(mUser);
        boolean folderMissing = true;
        if (mPrefs.contains(folderIdKey)) {
            long folderId = mPrefs.getLong(folderIdKey, 0);
            if (mModel.findFolderById(folderId) != null) {
                folderMissing = false;
            }
        }

        if (folderMissing) {
            // If the folder is missing, process all apps to ensure they are added to the new folder
            ArrayList<CachedPackageTracker.LauncherActivityInstallInfo> allApps = new ArrayList<>();
            for (LauncherActivityInfoCompat info : apps) {
                allApps.add(new CachedPackageTracker.LauncherActivityInstallInfo(info, info.getFirstInstallTime()));
            }
            new ManagedProfilePackageHandler().onLauncherAppsAdded(allApps, mUser, true);
        } else {
            new ManagedProfilePackageHandler().processUserApps(apps, mUser);
        }
    }

    private class ManagedProfilePackageHandler extends CachedPackageTracker {

        private ManagedProfilePackageHandler() {
            super(mContext, LauncherFiles.MANAGED_USER_PREFERENCES_KEY);
        }

        protected void onLauncherAppsAdded(
                List<LauncherActivityInstallInfo> apps, UserHandle user, boolean userAppsExisted) {
            ArrayList<ShortcutInfo> workFolderApps = new ArrayList<>();
            ArrayList<ShortcutInfo> homescreenApps = new ArrayList<>();

            int count = apps.size();
            long folderCreationTime =
                    mUserManager.getUserCreationTime(user) + AUTO_ADD_TO_FOLDER_DURATION;

            for (int i = 0; i < count; i++) {
                LauncherActivityInstallInfo info = apps.get(i);

                ShortcutInfo si = new ShortcutInfo(info.info, mContext);
                workFolderApps.add(si);
            }

            finalizeWorkFolder(user, workFolderApps, homescreenApps);

            // Never add work profile shortcuts to the homescreen directly.
            // They belong in the work folder or the app drawer.
            homescreenApps.clear();

            if (userAppsExisted && !homescreenApps.isEmpty()) {
                mModel.addAndBindAddedWorkspaceItems(mContext, homescreenApps);
            }
        }

        @Override
        protected void onLauncherPackageRemoved() {
        }

        /**
         * Adds and binds shortcuts marked to be added to the work folder.
         */
        private void finalizeWorkFolder(
                UserHandle user, final ArrayList<ShortcutInfo> workFolderApps,
                ArrayList<ShortcutInfo> homescreenApps) {
            if (workFolderApps.isEmpty()) {
                return;
            }
            // Try to get a work folder.
            String folderIdKey = USER_FOLDER_ID_PREFIX + mUserManager.getSerialNumberForUser(user);
            FolderInfo workFolder = null;
            if (mPrefs.contains(folderIdKey)) {
                long folderId = mPrefs.getLong(folderIdKey, 0);
                workFolder = mModel.findFolderById(folderId);

                if (workFolder != null && !workFolder.hasOption(FolderInfo.FLAG_WORK_FOLDER)) {
                    workFolder = null;
                }
            }

            if (workFolder != null) {
                final FolderInfo finalWorkFolder = workFolder;
                saveWorkFolderShortcuts(workFolder.id, workFolder.contents.size(), workFolderApps);

                // FolderInfo could already be bound. We need to add shortcuts on the UI thread.
                new MainThreadExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        for (ShortcutInfo info : workFolderApps) {
                            finalWorkFolder.add(info, false);
                        }
                    }
                });
            } else {
                // Create a new folder.
                final FolderInfo newWorkFolder = new FolderInfo();
                newWorkFolder.title = mContext.getText(R.string.work_folder_name);
                newWorkFolder.setOption(FolderInfo.FLAG_WORK_FOLDER, true, null);

                // Add all shortcuts before adding it to the UI, as an empty folder might get deleted.
                for (ShortcutInfo info : workFolderApps) {
                    newWorkFolder.add(info, false);
                }

                // Add the item to home screen and DB. This also generates an item id synchronously.
                ArrayList<ItemInfo> itemList = new ArrayList<>(1);
                itemList.add(newWorkFolder);
                mModel.addAndBindAddedWorkspaceItems(mContext, itemList);
                mPrefs.edit().putLong(folderIdKey, newWorkFolder.id).apply();

                saveWorkFolderShortcuts(newWorkFolder.id, 0, workFolderApps);
            }
        }

        @Override
        public void onShortcutsChanged(String packageName, List<ShortcutInfoCompat> shortcuts,
                                       UserHandle user) {
            // Do nothing
        }
    }

    /**
     * Add work folder shortcuts to the DB.
     */
    private void saveWorkFolderShortcuts(
            long workFolderId, int startingRank, ArrayList<ShortcutInfo> workFolderApps) {
        for (ItemInfo info : workFolderApps) {
            info.rank = startingRank++;
            LauncherModel.addItemToDatabase(mContext, info, workFolderId, 0, 0, 0);
        }
    }


    /**
     * Verifies that entries corresponding to {@param users} exist and removes all invalid entries.
     */
    public static void processAllUsers(List<UserHandle> users, Context context) {
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        HashSet<String> validKeys = new HashSet<>();
        for (UserHandle user : users) {
            addAllUserKeys(userManager.getSerialNumberForUser(user), validKeys);
        }

        SharedPreferences prefs = context.getSharedPreferences(
                LauncherFiles.MANAGED_USER_PREFERENCES_KEY,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (!validKeys.contains(key)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    private static void addAllUserKeys(long userSerial, HashSet<String> keysOut) {
        keysOut.add(INSTALLED_PACKAGES_PREFIX + userSerial);
        keysOut.add(USER_FOLDER_ID_PREFIX + userSerial);
    }

}
