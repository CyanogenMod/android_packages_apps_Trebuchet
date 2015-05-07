package com.android.launcher3.stats.internal.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.launcher3.stats.internal.model.TrackingEvent;
import com.android.launcher3.stats.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *     Helper for accessing the database
 * </pre>
 *
 * @see {@link SQLiteOpenHelper}
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Constants
    private static final String TAG = DatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "events";
    private static final int DATABASE_VERSION = 1;

    // Instance
    private static DatabaseHelper sInstance = null;

    /**
     * Constructor
     *
     * @param context {@link Context}
     * @return {@link DatabaseHelper}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public static DatabaseHelper createInstance(Context context) throws IllegalArgumentException {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context);
        }
        return sInstance;
    }

    /**
     * Constructor
     *
     * @param context {@link Context}
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Write an event to the database
     *
     * @param trackingEvent {@link TrackingEvent}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public void writeEvent(TrackingEvent trackingEvent)
            throws IllegalArgumentException {
        if (trackingEvent == null) {
            throw new IllegalArgumentException("'trackingEvent' cannot be null!");
        }
        Logger.logd(TAG, "Event written to database: " + trackingEvent);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = trackingEvent.toContentValues();
        db.insert(TrackingEventContract.EVENT_TABLE_NAME, null, contentValues);
        db.close();
    }

    /**
     * Get a list of tracking events
     *
     * @param instanceId {@link Integer}
     * @return {@link List}
     * @throws IllegalArgumentException {@link IllegalArgumentException}
     */
    public List<TrackingEvent> getTrackingEventsByCategory(int instanceId,
                                TrackingEvent.Category category) throws IllegalArgumentException {
        if (category == null) {
            throw new IllegalArgumentException("'category' cannot be null!");
        }

        List<TrackingEvent> eventList = new ArrayList<TrackingEvent>();

        // Get a writable database
        SQLiteDatabase db = getWritableDatabase();

        // Update unclaimed items for this instance
        ContentValues contentValues = new ContentValues();
        contentValues.put(TrackingEventContract.EVENT_COLUMN_INSTANCE, instanceId);
        String whereClause = TrackingEventContract.EVENT_COLUMN_INSTANCE + " IS NULL AND "
                + TrackingEventContract.EVENT_COLUMN_CATEGORY + " = ? ";
        String[] whereArgs = new String[] {
               category.name(),
        };
        int cnt = db.update(TrackingEventContract.EVENT_TABLE_NAME, contentValues, whereClause,
                whereArgs);

        // Short circuit empty update
        if (cnt < 1) {
            return eventList;
        }

        // Select all tagged items
        String selection = TrackingEventContract.EVENT_COLUMN_CATEGORY + " = ? AND "
                + TrackingEventContract.EVENT_COLUMN_INSTANCE + " = ? ";
        String[] selectionArgs = new String[]{
                category.name(),
                String.valueOf(instanceId),
        };
        Cursor c = db.query(TrackingEventContract.EVENT_TABLE_NAME, null, selection, selectionArgs,
                null, null, null);

        // Build return list
        while (c != null && c.getCount() > 0 && c.moveToNext()) {
            eventList.add(new TrackingEvent(c));
        }

        db.close();

        return eventList;
    }

    /**
     * Deletes events related to the instance
     *
     * @param instanceId {@link Integer}
     * @return {@link Integer}
     */
    public int deleteEventsByInstanceId(int instanceId) {
        SQLiteDatabase db = getWritableDatabase();
        String whereClause = TrackingEventContract.EVENT_COLUMN_INSTANCE + " = ?";
        String[] whereArgs = new String[]{
                String.valueOf(instanceId),
        };
        int cnt = db.delete(TrackingEventContract.EVENT_TABLE_NAME, whereClause, whereArgs);
        db.close();
        return cnt;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TrackingEventContract.CREATE_EVENT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // [NOTE][MSB]: This will lose data, need to make sure this is handled if/when database
        // schema changes

        // db.execSQL("DROP TABLE IF EXISTS " + TrackingEventContract.EVENT_TABLE_NAME);
        // onCreate(db);

    }

}
