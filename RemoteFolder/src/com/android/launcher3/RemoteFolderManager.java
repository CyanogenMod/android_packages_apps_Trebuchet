package com.android.launcher3;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AlphabeticalAppsList;


import java.util.ArrayList;
import java.util.List;

/**
 * Manages adding and removing the remote folder from the workspace.
 */
public class RemoteFolderManager {

    public RemoteFolderManager(final Launcher launcher) { }

    /**
     * Called when launcher receives a non-initial {@link Launcher#onCreate(Bundle)} call.
     * @param launcher new launcher activity.
     */
    public void onRecreateLauncher(final Launcher launcher) { }

    /**
     * Called when Launcher's views are loaded and ready.
     */
    public void onSetupViews() { }

    /**
     * Create a remote folder view.
     * @param icon folder icon view on the workspace.
     * @return a view for the remote folder.
     */
    public Folder createRemoteFolder(final FolderIcon icon, ViewGroup root) { return null; }

    /**
     * Get a drawable for the supplied item in the folder icon preview.
     * @param items list of views in the folder.
     * @param position index of icon to retreive.
     * @return an icon to draw in the folder preview.
     */
    public Drawable getFolderIconDrawable(final ArrayList<View> items,
                                          final int position) { return null; }

    /**
     * Called when Launcher finishes binding items from the model.
     */
    public void bindFinished() { }

    /**
     * Called when a setting for remote folder is updated.
     */
    public void onSettingChanged() { }

    /**
     * Called when the remote folder is dropped into the delete area on the workspace.
     */
    public void onFolderDeleted() { }

    /**
     * Called when the app drawer is opened.
     */
    public void onAppDrawerOpened() { }

    /**
     * Called when the app drawer is reloaded.
     */
    public void onReloadAppDrawer() { }

    /**
     * Called when the app drawer is measured.
     * @param numAppsPerRow the number of apps the drawer will show in a row.
     */
    public void onMeasureDrawer(int numAppsPerRow) { }

    /**
     * Called when new apps are added to launcher.
     * @param apps list of added apps.
     */
    public void onBindAddApps(ArrayList<AppInfo> apps) { }

    /**
     * Called when the info icon is clicked
     */
    public void onInfoIconClicked() { }

    /**
     * Called when the grid size for launcher is updated.
     */
    public void onGridSizeChanged() { }

    /**
     * Change the appearance of FolderIcon for our RemoteFolder by adding a badge
     * @param icon the FolderIcon to update
     * @return a FolderIcon with an added ImageView
     */
    public static FolderIcon addBadgeToFolderIcon(FolderIcon icon) {
        return icon;
    }

    /**
     * Called when adapter items for predicted apps are updated.
     * @param items current list of built adapter items.
     * @param fastScrollInfo fast scroller info for this section.
     * @param sectionInfo info about apps in this section.
     * @param position current position of item to be built into the adapter.
     * @return the new position to start from for next adapter items.
     */
    public int onUpdateAdapterItems(final List<AlphabeticalAppsList.AdapterItem> items,
                                     final AlphabeticalAppsList.FastScrollSectionInfo fastScrollInfo,
                                     final AlphabeticalAppsList.SectionInfo sectionInfo,
                                    int position) { return position; }

    /**
     * Called when a view holder is created for a remote app.
     * @param holder remote view holder.
     * @param viewType specific type of view holder.
     */
    public void onCreateViewHolder(final AllAppsGridAdapter.ViewHolder holder, final int viewType) { }

    /**
     * Called when a view holder is bound for a remote app.
     * @param holder remote view holder.
     * @param item info for this app.
     */
    public void onBindViewHolder(final AllAppsGridAdapter.ViewHolder holder, final AppInfo item) { }

    /**
     * Populate home settings list with additional values as needed.
     * @param values list of settings strings.
     * @param context application context.
     */
    public static void onInitializeHomeSettings(final ArrayList<String> values,
                                                   final Context context) { }

    /**
     * Populate drawer settings list with additional values as needed.
     * @param values list of settings strings.
     * @param context application context.
     */
    public static void onInitializeDrawerSettings(final ArrayList<String> values,
                                                   final Context context) { }
}
