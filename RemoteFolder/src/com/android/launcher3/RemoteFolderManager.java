package com.android.launcher3;

/**
 * Manages adding and removing the remote folder from the workspace.
 */
public class RemoteFolderManager {

    public RemoteFolderManager(final Launcher launcher) { }

    public void setRemoteFolder(final FolderIcon remoteFolder) { }

    /**
     * Called when Launcher finishes binding items from the model.
     */
    public void bindFinished() { }

    /**
     * Called when the setting for remote folder is updated.
     * @param newValue the new setting for remote folder
     */
    public void onSettingChanged(final boolean newValue) { }

    /**
     * Called when the remote folder is dropped into the delete area on the workspace.
     */
    public void onFolderDeleted() { }
}
