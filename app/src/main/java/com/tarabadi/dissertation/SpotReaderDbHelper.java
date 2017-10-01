package com.tarabadi.dissertation;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.tarabadi.dissertation.SpotInfo.SQL_CREATE_ENTRIES;
import static com.tarabadi.dissertation.SpotInfo.SQL_DELETE_ENTRIES;

/**
 * Created by Matt
 */

public class SpotReaderDbHelper extends SQLiteOpenHelper {
    //DB version increments if schema changes
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SpotInfo.db";

    public SpotReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        //DB would be used as a cache for online data, so to upgrade simply discard old table and
//        //data and create a new table with new data
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }
}
