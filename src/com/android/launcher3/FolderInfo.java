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

package com.android.launcher3;

import android.content.ContentValues;
import android.content.Context;

import com.android.launcher3.compat.UserHandleCompat;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a folder containing shortcuts or apps.
 */
public class FolderInfo extends ItemInfo {
    public static final int REMOTE_SUBTYPE = 1;

    /**
     * Whether this folder has been opened
     */
    boolean opened;
    int subType;

    /**
     * The apps and shortcuts and hidden status
     */
    ArrayList<ShortcutInfo> contents = new ArrayList<ShortcutInfo>();
    Boolean hidden = false;

    ArrayList<FolderListener> listeners = new ArrayList<FolderListener>();

    FolderInfo() {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_FOLDER;
        user = UserHandleCompat.myUserHandle();
    }

    /**
     * Add an app or shortcut
     *
     * @param item
     */
    public void add(ShortcutInfo item) {
        contents.add(item);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAdd(item);
        }
        itemsChanged();
    }

    /**
     * Remove an app or shortcut. Does not change the DB.
     *
     * @param item
     */
    public void remove(ShortcutInfo item) {
        contents.remove(item);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onRemove(item);
        }
        itemsChanged();
    }

    /**
     * Remove all apps and shortcuts. Does not change the DB unless
     * LauncherModel.deleteFolderContentsFromDatabase(Context, FolderInfo) is called first.
     */
    public void removeAll() {
        contents.clear();
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onRemoveAll();
        }
        itemsChanged();
    }

    /**
     * Remove all supplied shortcuts. Does not change the DB unless
     * LauncherModel.deleteFolderContentsFromDatabase(Context, FolderInfo) is called first.
     * @param items the shortcuts to remove.
     */
    public void removeAll(ArrayList<ShortcutInfo> items) {
        contents.removeAll(items);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onRemoveAll(items);
        }
        itemsChanged();
    }

    /**
     * @return true if this info represents a remote folder, false otherwise
     */
    public boolean isRemote() {
        return (subType & REMOTE_SUBTYPE) != 0;
    }

    /**
     * Set flag indicating whether this folder is remote
     * @param remote true if folder is remote, false otherwise
     */
    public void setRemote(final boolean remote) {
        if (remote) {
            subType |= REMOTE_SUBTYPE;
        } else {
            subType &= ~REMOTE_SUBTYPE;
        }
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onTitleChanged(title);
        }
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        values.put(LauncherSettings.Favorites.TITLE, title.toString());
        values.put(LauncherSettings.Favorites.HIDDEN, hidden ? 1 : 0);
        values.put(LauncherSettings.BaseLauncherColumns.SUBTYPE, subType);
    }

    void addListener(FolderListener listener) {
        listeners.add(listener);
    }

    void removeListener(FolderListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    void itemsChanged() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onItemsChanged();
        }
    }

    @Override
    void unbind() {
        super.unbind();
        listeners.clear();
    }

    interface FolderListener {
        void onAdd(ShortcutInfo item);
        void onRemove(ShortcutInfo item);
        void onRemoveAll();
        void onRemoveAll(ArrayList<ShortcutInfo> items);
        void onTitleChanged(CharSequence title);
        void onItemsChanged();
    }

    @Override
    public String toString() {
        return "FolderInfo(id=" + this.id + " type=" + this.itemType + " subtype=" + this.subType
                + " container=" + this.container + " screen=" + screenId
                + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX
                + " spanY=" + spanY + " dropPos=" + Arrays.toString(dropPos) + ")";
    }
}
