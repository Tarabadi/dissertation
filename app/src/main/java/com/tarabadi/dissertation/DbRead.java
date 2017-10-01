package com.tarabadi.dissertation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by Matt
 */

public class DbRead extends AsyncTask<SpotReaderDbHelper, Void, ArrayList<Spot>> {


    //interface to enable UI updates
    public interface AsyncReadResponse {
        void readFinish(ArrayList<Spot> dbRecords);
    }

    public AsyncReadResponse delegate = null;

    public DbRead(AsyncReadResponse delegate){
        this.delegate = delegate;
    }

    @Override
    protected ArrayList<Spot> doInBackground(SpotReaderDbHelper... dbs) {
        int count = dbs.length;
        ArrayList<Spot> spots = new ArrayList<Spot>();
        if (count == 1){
            SpotReaderDbHelper mdb = dbs[0];
            SQLiteDatabase db = mdb.getReadableDatabase();
            Cursor cursor = db.query(
                    SpotInfo.SpotEntry.TABLE_NAME,
                    null,
                    SpotInfo.SpotEntry.COLUMN_NAME_TITLE+" IS NOT NULL",
                    null, null, null, null
            );

            while (cursor.moveToNext()){
                long spotID = cursor.getLong(cursor.getColumnIndexOrThrow(SpotInfo.SpotEntry._ID));
                String spotTitle = cursor.getString(cursor.getColumnIndexOrThrow(SpotInfo.SpotEntry.COLUMN_NAME_TITLE));
                String spotDesc = cursor.getString(cursor.getColumnIndexOrThrow(SpotInfo.SpotEntry.COLUMN_NAME_DESC));
                double spotLat = cursor.getDouble(cursor.getColumnIndexOrThrow(SpotInfo.SpotEntry.COLUMN_NAME_LAT));
                double spotLng = cursor.getDouble(cursor.getColumnIndexOrThrow(SpotInfo.SpotEntry.COLUMN_NAME_LONG));
                Uri photoUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(SpotInfo.SpotEntry.COLUMN_NAME_PHOTOURI)));
                spots.add(new Spot(spotID, spotTitle, spotDesc, spotLat, spotLng, photoUri));
            }
            cursor.close();
            db.close();
            return spots;
        }else{
            return null;
        }
    }

    protected void onPostExecute(ArrayList<Spot> spots){
        delegate.readFinish(spots);
    }


}
