package com.android.launcher3.list;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.AppDrawerListAdapter;
import com.android.launcher3.AppsCustomizePagedView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.OverviewSettingsPanel;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.settings.SettingsProvider;

public class SettingsPinnedHeaderAdapter extends PinnedHeaderListAdapter {
    private static final int PARTITION_TAG = 0;
    private static final int POSITION_TAG = 1;
    private static final float ENABLED_ALPHA = 1f;
    private static final float DISABLED_ALPHA = 1f;

    private Launcher mLauncher;
    private Context mContext;

    class SettingsPosition {
        int partition = 0;
        int position = 0;

        SettingsPosition (int partition, int position) {
            this.partition = partition;
            this.position = position;
        }
    }

    public SettingsPinnedHeaderAdapter(Context context) {
        super(context);
        mLauncher = (Launcher) context;
        mContext = context;
    }

    private String[] mHeaders;
    public int mPinnedHeaderCount;

    public void setHeaders(String[] headers) {
        this.mHeaders = headers;
    }

    @Override
    protected View newHeaderView(Context context, int partition, Cursor cursor,
                                 ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.settings_pane_list_header, null);
    }

    @Override
    protected void bindHeaderView(View view, int partition, Cursor cursor) {
        TextView textView = (TextView) view.findViewById(R.id.item_name);
        textView.setText(mHeaders[partition]);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
                           ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.settings_pane_list_item, null);
    }

    @Override
    protected void bindView(View v, int partition, Cursor cursor, int position) {
        TextView text = (TextView)v.findViewById(R.id.item_name);
        // RTL
        Configuration config = mLauncher.getResources().getConfiguration();
        if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            text.setGravity(Gravity.RIGHT);
        }

        String title = cursor.getString(1);
        text.setText(title);

        v.setTag(new SettingsPosition(partition, position));

        Resources res = mLauncher.getResources();


        boolean current = false;
        String state = "";

        switch (partition) {
            case OverviewSettingsPanel.HOME_SETTINGS_POSITION:
                switch (position) {
                    case 0:
                        current = mLauncher.isSearchBarEnabled();
                        state = current ? res.getString(R.string.setting_state_on)
                                : res.getString(R.string.setting_state_off);
                        ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        break;
                    case 1:
                        state = mLauncher.getWorkspaceTransitionEffect();
                        state = mapEffectToValue(state);
                        ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        break;
                    case 2:
                        current = mLauncher.shouldHideWorkspaceIconLables();
                        state = current ? res.getString(R.string.icon_labels_hide)
                                : res.getString(R.string.icon_labels_show);
                        ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        break;
                    case 3:
                        current = SettingsProvider.getBoolean(mContext,
                                SettingsProvider.SETTINGS_UI_HOMESCREEN_SCROLLING_WALLPAPER_SCROLL,
                                R.bool.preferences_interface_homescreen_scrolling_wallpaper_scroll_default);
                        state = current ? res.getString(R.string.setting_state_on)
                                : res.getString(R.string.setting_state_off);
                        ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        break;
                    case 4:
                        updateDynamicGridSizeSettingsItem(v);
                        break;
                    case 5:
                        current = SettingsProvider.getBoolean(mContext,
                                SettingsProvider.SETTINGS_UI_HOMESCREEN_REMOTE_FOLDER,
                                R.bool.preferences_interface_homescreen_remote_folder_default);
                        state = current ? res.getString(R.string.setting_state_on)
                                : res.getString(R.string.setting_state_off);
                        ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        break;
                    default:
                        ((TextView) v.findViewById(R.id.item_state)).setText("");
                }
                break;
            case OverviewSettingsPanel.DRAWER_SETTINGS_POSITION:
                switch (position) {
                    case 0:
                        updateDrawerTypeSettingsItem(v);
                        break;
                    case 1:
                        if (!setDisabled(v)) {
                            state = mLauncher.getAppsCustomizeTransitionEffect();
                            state = mapEffectToValue(state);
                            ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        }
                        break;
                    case 2:
                        if (!setDisabled(v)) {
                            updateDrawerSortSettingsItem(v);
                        }
                        break;
                    case 3:
                        current = SettingsProvider.getBoolean(mContext,
                                SettingsProvider.SETTINGS_UI_DRAWER_HIDE_ICON_LABELS,
                                R.bool.preferences_interface_drawer_hide_icon_labels_default);
                        state = current ? res.getString(R.string.icon_labels_hide)
                                : res.getString(R.string.icon_labels_show);
                        ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        break;
                    default:
                        ((TextView) v.findViewById(R.id.item_state)).setText("");
                }
                break;
            default:
                switch (position) {
                    case 0:
                        current = SettingsProvider.getBoolean(mContext,
                                SettingsProvider.SETTINGS_UI_GENERAL_ICONS_LARGE,
                                R.bool.preferences_interface_general_icons_large_default);
                        state = current ? res.getString(R.string.setting_state_on)
                                : res.getString(R.string.setting_state_off);
                        ((TextView) v.findViewById(R.id.item_state)).setText(state);
                        break;
                    default:
                        ((TextView) v.findViewById(R.id.item_state)).setText("");
                }
        }

        v.setOnClickListener(mSettingsItemListener);
    }

    @Override
    public View getPinnedHeaderView(int viewIndex, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.settings_pane_list_header, parent, false);
        view.setFocusable(false);
        view.setEnabled(false);
        bindHeaderView(view, viewIndex, null);
        return view;
    }

    @Override
    public int getPinnedHeaderCount() {
        return mPinnedHeaderCount;
    }

    public void updateDrawerSortSettingsItem(View v) {
        String state = "";
        switch (mLauncher.getAppsCustomizeContentSortMode()) {
            case Title:
                state = mLauncher.getResources().getString(R.string.sort_mode_title);
                break;
            case LaunchCount:
                state = mLauncher.getResources().getString(
                        R.string.sort_mode_launch_count);
                break;
            case InstallTime:
                state = mLauncher.getResources().getString(
                        R.string.sort_mode_install_time);
                break;
        }
        ((TextView) v.findViewById(R.id.item_state)).setText(state);
    }

    public void updateDrawerTypeSettingsItem(View v) {
        String state = "";
        AppDrawerListAdapter.DrawerType type = mLauncher.getDrawerType();
        switch (type) {
            case Drawer:
                state = mLauncher.getResources().getString(R.string.drawer_type_drawer);
                break;
            case Pager:
                state = mLauncher.getResources().getString(R.string.drawer_type_pager);
                break;
        }
        ((TextView) v.findViewById(R.id.item_state)).setText(state);
    }

    public void updateDynamicGridSizeSettingsItem(View v) {
        DeviceProfile.GridSize gridSize = DeviceProfile.GridSize.getModeForValue(
                SettingsProvider.getIntCustomDefault(mLauncher,
                SettingsProvider.SETTINGS_UI_DYNAMIC_GRID_SIZE, 0));
        String state = "";

        switch (gridSize) {
            case Comfortable:
                state = mLauncher.getResources().getString(R.string.grid_size_comfortable);
                break;
            case Cozy:
                state = mLauncher.getResources().getString(R.string.grid_size_cozy);
                break;
            case Condensed:
                state = mLauncher.getResources().getString(R.string.grid_size_condensed);
                break;
            case Custom:
                int rows = SettingsProvider.getIntCustomDefault(mLauncher,
                        SettingsProvider.SETTINGS_UI_HOMESCREEN_ROWS, 0);
                int columns = SettingsProvider.getIntCustomDefault(mLauncher,
                        SettingsProvider.SETTINGS_UI_HOMESCREEN_COLUMNS, 0);
                state = rows + " " + "\u00d7" + " " + columns;
                break;
        }
        ((TextView) v.findViewById(R.id.item_state)).setText(state);
    }

    private String mapEffectToValue(String effect) {
        final String[] titles = mLauncher.getResources().getStringArray(
                R.array.transition_effect_entries);
        final String[] values = mLauncher.getResources().getStringArray(
                R.array.transition_effect_values);

        int length = values.length;
        for (int i = 0; i < length; i++) {
            if (effect.equals(values[i])) {
                return titles[i];
            }
        }
        return "";
    }

    OnClickListener mSettingsItemListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            String value = ((TextView) v.findViewById(R.id.item_name)).getText().toString();
            Resources res = mLauncher.getResources();

            int partition = ((SettingsPosition) v.getTag()).partition;
            int position = ((SettingsPosition) v.getTag()).position;

            switch (partition) {
                case OverviewSettingsPanel.HOME_SETTINGS_POSITION:
                    switch (position) {
                        case 0:
                            updateSearchBarVisibility(v);
                            mLauncher.setUpdateDynamicGrid(false);
                            break;
                        case 1:
                            mLauncher.onClickTransitionEffectButton(v, false);
                            break;
                        case 2:
                            onIconLabelsBooleanChanged(v,
                                    SettingsProvider.SETTINGS_UI_HOMESCREEN_HIDE_ICON_LABELS,
                                    R.bool.preferences_interface_homescreen_hide_icon_labels_default);
                            mLauncher.setUpdateDynamicGrid(false);
                            break;
                        case 3:
                            onSettingsBooleanChanged(v,
                                    SettingsProvider.SETTINGS_UI_HOMESCREEN_SCROLLING_WALLPAPER_SCROLL,
                                    R.bool.preferences_interface_homescreen_scrolling_wallpaper_scroll_default);
                            mLauncher.setUpdateDynamicGrid(false);
                            break;
                        case 4:
                            mLauncher.onClickDynamicGridSizeButton();
                            break;
                        case 5:
                            boolean newValue = onSettingsBooleanChanged(v,
                                    SettingsProvider.SETTINGS_UI_HOMESCREEN_REMOTE_FOLDER,
                                    R.bool.preferences_interface_homescreen_remote_folder_default);
                            mLauncher.getRemoteFolderManager().onSettingChanged(newValue);
                            break;
                    }
                    break;
                case OverviewSettingsPanel.DRAWER_SETTINGS_POSITION:
                    switch (position) {
                        case 0:
                            onClickDrawerTypeButton();
                            break;
                        case 1:
                            mLauncher.onClickTransitionEffectButton(v, true);

                            break;
                        case 2:
                            onClickSortButton();

                            break;
                        case 3:
                            onIconLabelsBooleanChanged(v,
                                    SettingsProvider.SETTINGS_UI_DRAWER_HIDE_ICON_LABELS,
                                    R.bool.preferences_interface_drawer_hide_icon_labels_default);
                            mLauncher.setUpdateDynamicGrid(false);
                            break;
                    }
                    break;
                default:
                    switch (position) {
                        case 0:
                            onSettingsBooleanChanged(v,
                                    SettingsProvider.SETTINGS_UI_GENERAL_ICONS_LARGE,
                                    R.bool.preferences_interface_general_icons_large_default);
                            mLauncher.setUpdateDynamicGrid(false);
                            break;
                        case 1:
                            Intent intent = new Intent();
                            intent.setClassName(OverviewSettingsPanel.ANDROID_SETTINGS,
                                    OverviewSettingsPanel.ANDROID_PROTECTED_APPS);
                            mLauncher.startActivity(intent);
                            break;
                    }
            }

            View defaultHome = mLauncher.findViewById(R.id.default_home_screen_panel);
            defaultHome.setVisibility(getCursor(0).getCount() > 1 ? View.VISIBLE : View.GONE);
        }
    };

    private void updateSearchBarVisibility(View v) {
        boolean isSearchEnabled = SettingsProvider.getBoolean(mContext,
                SettingsProvider.SETTINGS_UI_HOMESCREEN_SEARCH,
                R.bool.preferences_interface_homescreen_search_default);

        if (!isSearchEnabled) {
            if (!Utilities.searchActivityExists(mContext)) {
                Toast.makeText(mContext, mContext.getString(R.string.search_activity_not_found),
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        onSettingsBooleanChanged(v,
                SettingsProvider.SETTINGS_UI_HOMESCREEN_SEARCH,
                R.bool.preferences_interface_homescreen_search_default);
    }

    private boolean onSettingsBooleanChanged(View v, String key, int res) {
        boolean current = SettingsProvider.getBoolean(
                mContext, key, res);

        // Set new state
        SettingsProvider.putBoolean(mContext, key, !current);
        SettingsProvider.putBoolean(mContext, SettingsProvider.SETTINGS_CHANGED, true);

        String state = current ? mLauncher.getResources().getString(
                R.string.setting_state_off) : mLauncher.getResources().getString(
                R.string.setting_state_on);
        ((TextView) v.findViewById(R.id.item_state)).setText(state);

        return !current;
    }

    private void onIconLabelsBooleanChanged(View v, String key, int res) {
        boolean current = SettingsProvider.getBoolean(
                mContext, key, res);

        // Set new state
        SettingsProvider.putBoolean(mContext, key, !current);
        SettingsProvider.putBoolean(mContext, SettingsProvider.SETTINGS_CHANGED, true);

        String state = current ? mLauncher.getResources().getString(
                R.string.icon_labels_show) : mLauncher.getResources().getString(
                R.string.icon_labels_hide);
        ((TextView) v.findViewById(R.id.item_state)).setText(state);
    }

    private void onClickSortButton() {
        int sort = SettingsProvider.getIntCustomDefault(mLauncher,
                SettingsProvider.SETTINGS_UI_DRAWER_SORT_MODE, 0);

        sort = (sort + 1) % AppsCustomizePagedView.SortMode.values().length;
        mLauncher.getAppsCustomizeContent().setSortMode(
                AppsCustomizePagedView.SortMode.getModeForValue(sort));

        SettingsProvider.putInt(mLauncher, SettingsProvider.SETTINGS_UI_DRAWER_SORT_MODE, sort);

        notifyDataSetChanged();
    }

    private void onClickDrawerTypeButton() {
        int type = SettingsProvider.getInt(mLauncher,
                SettingsProvider.SETTINGS_UI_DRAWER_TYPE,
                R.integer.preferences_interface_drawer_type_default);

        type = (type + 1) % AppDrawerListAdapter.DrawerType.values().length;
        SettingsProvider.putInt(mLauncher, SettingsProvider.SETTINGS_UI_DRAWER_TYPE, type);

        mLauncher.updateDrawerType();

        notifyDataSetChanged();
    }

    private boolean setDisabled(View v) {
        TextView itemState = ((TextView) v.findViewById(R.id.item_state));
        TextView itemName = ((TextView) v.findViewById(R.id.item_name));

        AppDrawerListAdapter.DrawerType type = mLauncher.getDrawerType();

        boolean isDisabled = false;

        switch (type) {
            case Drawer:
                itemState.setAlpha(DISABLED_ALPHA);
                itemState.setText(mLauncher.getResources()
                        .getString(R.string.setting_state_disabled));
                itemName.setAlpha(DISABLED_ALPHA);
                v.setEnabled(false);
                isDisabled = true;
                break;
            case Pager:
                itemState.setAlpha(ENABLED_ALPHA);
                itemName.setAlpha(ENABLED_ALPHA);
                v.setEnabled(true);
                break;
        }

        return isDisabled;
    }
}
