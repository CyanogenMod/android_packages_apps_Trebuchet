package com.android.launcher3.stats.internal.db;

import android.provider.BaseColumns;

/**
 * <pre>
 *     Table contract definition
 * </pre>
 *
 * @see {@link BaseColumns}
 */
public class TrackingEventContract implements BaseColumns {

    // Constants
    public static final String EVENT_TABLE_NAME = "event";

    // Columns
    public static final String EVENT_COLUMN_CATEGORY = "category";
    public static final String EVENT_COLUMN_METADATA = "metadata";
    public static final String EVENT_COLUMN_INSTANCE = "instance";

    // SQL
    public static final String CREATE_EVENT_TABLE = "CREATE TABLE " + EVENT_TABLE_NAME
            + " ( "
            + " `" + _ID + "` INTEGER PRIMARY KEY AUTOINCREMENT, "
            + " `" + EVENT_COLUMN_CATEGORY + "` TEXT, "
            + " `" + EVENT_COLUMN_METADATA + "` TEXT, "
            + " `" + EVENT_COLUMN_INSTANCE + "` INTEGER "
            + ");";

}
