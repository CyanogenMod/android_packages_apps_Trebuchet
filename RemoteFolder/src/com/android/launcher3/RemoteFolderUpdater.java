package com.android.launcher3;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import java.util.List;
import android.util.Log;

public class RemoteFolderUpdater {

    private static final String TAG = "RemoteFolderUpdater";

    private static final Object sLock = new Object();
    private static RemoteFolderUpdater sInstance;

    public interface RemoteFolderUpdateListener {
        void onSuccess(List<RemoteFolderInfo> remoteFolderInfoList);
        void onFailure(String error);
    }

    public static RemoteFolderUpdater getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new RemoteFolderUpdater();
            }

            return sInstance;
        }
    }

    private RemoteFolderUpdater() { }

    /**
     * Requests data needed by remote folders.
     * @param context
     * @param size
     * @param listener
     */
    public synchronized void requestSync(Context context, final int size, final RemoteFolderUpdateListener listener) {
        if (listener != null) {
            listener.onFailure("RemoteFolderUpdater may not have been properly setup");
        }
    }

    /**
     * Register a callback to track clicks on our individual Remote Folder items. Make sure the
     * intent associated with each item has a unique ID.
     *
     * @param view The individual item the user may click (or just clicked)
     * @param intent The intent associated with the ShortcutInfo that belongs to our view
     */
    public void registerViewForInteraction(View view, Intent intent) {
        Log.e(TAG, "Couldn't register view for user interaction, RemoteFolderUpdater may not have been properly setup");
    }

    /**
     * Holds important information that the launcher will need for each item in the remote folder.
     */
    public class RemoteFolderInfo {

        public void setRecommendationData(View view) {
            return;
        }

        public String getTitle() {
            return null;
        }

        public Bitmap getIcon() {
            return null;
        }

        public String getIconUrl() {
            return null;
        }

        public Intent getIntent() {
            return null;
        }
    }

}
