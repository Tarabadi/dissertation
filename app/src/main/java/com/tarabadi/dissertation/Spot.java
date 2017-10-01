package com.tarabadi.dissertation;

import android.content.ContentValues;
import android.location.Location;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Matt
 */

public class Spot {
        //id of row in db
        public final long id;

        public final String title;
        public final String desc;
        public final double lat;
        public final double lng;
        public final Uri pic;

        int distance;

        Spot(long id, String title, String desc, double lat, double lng, Uri pic) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.lat = lat;
            this.lng = lng;
            this.pic = pic;
        }

        public void distanceCalc(LatLng current){
            float[] results = new float[1];
            Location.distanceBetween(
                    current.latitude,
                    current.longitude,
                    lat, lng,
                    results
            );

            distance = (int) (results[0]);
        }

        public ContentValues getContentValue(){
            ContentValues values = new ContentValues();
            values.put(SpotInfo.SpotEntry.COLUMN_NAME_TITLE, title);
            values.put(SpotInfo.SpotEntry.COLUMN_NAME_DESC, desc);
            values.put(SpotInfo.SpotEntry.COLUMN_NAME_LAT, lat);
            values.put(SpotInfo.SpotEntry.COLUMN_NAME_LONG, lng);
            values.put(SpotInfo.SpotEntry.COLUMN_NAME_PHOTOURI, pic.toString());
            return values;
        }
}

