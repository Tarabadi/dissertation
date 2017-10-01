package com.tarabadi.dissertation;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

/**
 * Created by Matt
 */

public class DbWrite extends AsyncTask<Void, Void, Long>{


    //interface to enable UI updates etc
    public interface AsyncWriteResponse{
        void writeFinish(long rowID);
    }

    public AsyncWriteResponse delegate = null;

    Spot newSpot;
    SpotReaderDbHelper mDb;

    public DbWrite(AsyncWriteResponse delegate, Spot newSpot, SpotReaderDbHelper mDb){
        this.delegate = delegate;
        this.newSpot = newSpot;
        this.mDb = mDb;
    }

    @Override
    protected Long doInBackground(Void... params) {
        SQLiteDatabase db = mDb.getWritableDatabase();
        ContentValues values = newSpot.getContentValue();
        long newRowId = db.insert(SpotInfo.SpotEntry.TABLE_NAME, null, values);
        db.close();
        return newRowId;
    }

    protected void onPostExecute(Long rowID){
        delegate.writeFinish(rowID);
    }


}
