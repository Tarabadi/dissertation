package com.tarabadi.dissertation;

import android.provider.BaseColumns;

/**
 * Created by Matt
 */

public final class SpotInfo {

    //To prevent someone from accidentally instantiating the class
    //make constructor private.
    private SpotInfo() {}

    /*Inner class defining table contents*/
    public static class SpotEntry implements BaseColumns {
        public static final String TABLE_NAME = "list_of_spots";
        public static final String COLUMN_NAME_PHOTOURI = "pictograph";
        public static final String COLUMN_NAME_LAT = "latitude";
        public static final String COLUMN_NAME_LONG = "longitude";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESC = "description";

    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SpotEntry.TABLE_NAME + " (" +
                    SpotEntry._ID + " INTEGER PRIMARY KEY, "+
                    SpotEntry.COLUMN_NAME_TITLE + " TEXT, " +
                    SpotEntry.COLUMN_NAME_DESC + " TEXT, " +
                    SpotEntry.COLUMN_NAME_LAT + " REAL, " +
                    SpotEntry.COLUMN_NAME_LONG + " REAL, " +
                    SpotEntry.COLUMN_NAME_PHOTOURI + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SpotEntry.TABLE_NAME;


}


