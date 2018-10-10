package eu.mcomputing.cohave.funfi.contentprovider.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

import java.io.Serializable;
import java.util.Calendar;

/*
 The MIT License (MIT)

 Copyright (c) 2015 Maros Cavojsky (www.mpage.sk), mComputing (www.mcomputig.eu)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class WifiModel implements Serializable {
    public long _id;
    public String ssid;
    public String bssid;
    public long found_at;
    public String found_by;
    public long pos_score = 0;
    public long neg_score = 0;
    public float factor = 1;
    public int rating = WIFI_NOLIKE;
    public int sync = WifiModel.WIFI_SYNC_OK;//user not created any change
    public long rating_time = 0;

    public static final int WIFI_LIKE = 2;
    public static final int WIFI_DISLIKE = 1;
    public static final int WIFI_NOLIKE = 0;

    public static final int WIFI_SYNC_OK = 0;
    public static final int WIFI_SYNC_REQ = 1;
    public static final int WIFI_SYNC_FAIL = -1;

    public WifiModel(long _id, String ssid, String bssid, long found_at, String found_by, long pos_score, long neg_score, float factor, int rating, int sync) {
        this._id = _id;
        this.ssid = ssid;
        this.bssid = bssid;
        this.found_at = found_at;
        this.found_by = found_by;
        this.pos_score = pos_score;
        this.neg_score = neg_score;
        this.factor = factor;
        this.rating = rating;
        this.sync = sync;
        this.rating_time = 0;
    }

    public WifiModel(Cursor c) {
        String[] cols = c.getColumnNames();

        for (int i = 0; i < cols.length; i++) {
            switch (cols[i]) {
                case Table._ID:
                    this._id = c.getLong(i);
                    break;
                case Table.SSID:
                    this.ssid = c.getString(i);
                    break;
                case Table.BSSID:
                    this.bssid = c.getString(i);
                    break;
                case Table.FOUND_AT:
                    this.found_at = c.getLong(i);
                    break;
                case Table.FOUND_BY:
                    this.found_by = c.getString(i);
                    break;
                case Table.POS_SCORE:
                    this.pos_score = c.getLong(i);
                    break;
                case Table.NEG_SCORE:
                    this.neg_score = c.getLong(i);
                    break;
                case Table.FACTOR:
                    this.factor = c.getFloat(i);
                    break;
                case Table.RATING:
                    this.rating = c.getInt(i);
                    break;
                case Table.SYNC:
                    this.sync = c.getInt(i);
                case Table.RATING_TIME:
                    this.rating_time = c.getLong(i);
            }
        }

    }

    public static ContentValues getContentValuesFromArr(String[] data) {
        ContentValues values = new ContentValues();
        values.put(Table._ID, Long.parseLong(data[0]));
        values.put(Table.SSID, data[1]);
        values.put(Table.BSSID, data[2]);
        values.put(Table.FOUND_AT, Long.parseLong(data[3]));
        values.put(Table.FOUND_BY, data[4]);
        values.put(Table.POS_SCORE, Long.parseLong(data[5]));
        values.put(Table.NEG_SCORE, Long.parseLong(data[6]));
        values.put(Table.FACTOR, Float.parseFloat(data[7]));
        values.put(Table.RATING, Integer.parseInt(data[8]));
        values.put(Table.SYNC, WIFI_SYNC_OK);
        values.put(Table.RATING_TIME, Long.parseLong(data[9]));
        return values;
    }

    public static String[] getListAttr() {
        return new String[]{
                Table._ID,
                Table.SSID,
                Table.BSSID,
                Table.FOUND_AT,
                Table.FOUND_BY,
                Table.POS_SCORE,
                Table.NEG_SCORE,
                Table.FACTOR,
                Table.RATING,
                Table.SYNC,
                Table.RATING_TIME
        };
    }


    public ContentValues getContentValues() {
        Calendar c = Calendar.getInstance();
        ContentValues values = new ContentValues();
        values.put(Table._ID, _id);
        values.put(Table.SSID, ssid);
        values.put(Table.BSSID, bssid);
        values.put(Table.FOUND_AT, found_at);
        values.put(Table.FOUND_BY, found_by);
        values.put(Table.POS_SCORE, pos_score);
        values.put(Table.NEG_SCORE, neg_score);
        values.put(Table.FACTOR, factor);
        values.put(Table.RATING, rating);
        values.put(Table.SYNC, sync);
        values.put(Table.RATING_TIME, rating_time == 0 ? c.getTimeInMillis() / 1000 : rating_time);
        return values;
    }




    public static ContentValues getLikeContentValues(int type) {
        ContentValues values = new ContentValues();
        values.put(Table.RATING, type);
        values.put(Table.SYNC, WIFI_SYNC_REQ);
        return values;
    }

    public static ContentValues getSyncContentValues(int type) {
        ContentValues values = new ContentValues();
        values.put(Table.SYNC, type);
        return values;
    }

    public static ContentValues getSyncUpdateValues(int[] param) {
        ContentValues values = new ContentValues();
        values.put(Table.POS_SCORE, param[0]);
        values.put(Table.NEG_SCORE, param[1]);
        values.put(Table.FACTOR, param[2]);
        return values;
    }


    @Override
    public String toString() {
        return "WifiModel{" +
                "pos_score=" + pos_score +
                ", neg_score=" + neg_score +
                ", factor=" + factor +
                ", found_by='" + found_by + '\'' +
                ", _id=" + _id +
                ", ssid='" + ssid + '\'' +
                ", bssid='" + bssid + '\'' +
                ", found_at=" + found_at +
                ", rating=" + rating +
                ", sync=" + sync +
                ", rating_time=" + rating_time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WifiModel) {
            WifiModel other = (WifiModel) o;
            return this._id == other._id;
        }

        return false;
    }

    public static final class Table {
        public static final String _ID = BaseColumns._ID;
        public static final String SSID = "ssid";
        public static final String BSSID = "bssid";
        public static final String FOUND_BY = "found_by"; //datetime when network was found
        public static final String FOUND_AT = "found_at"; //user that found the network first
        public static final String POS_SCORE = "pos_score"; // positive score for network
        public static final String NEG_SCORE = "neg_score"; // negative score for network
        public static final String FACTOR = "factor"; //order priority
        public static final String RATING = "rating"; // current user rating -1 none, 0 - bad, 1- good
        public static final String SYNC = "sync"; //if record was changed
        public static final String RATING_TIME = "rating_time"; //when user done rating

        public static final String TABLE_NAME = "wifi";
        public static final String CREATE_SQL =
                "CREATE TABLE " + TABLE_NAME
                        + "(" + _ID + " INTEGER PRIMARY KEY, "
                        + BSSID + " TEXT NOT NULL,"
                        + SSID + " TEXT NOT NULL,"
                        + FOUND_AT + " INTEGER NOT NULL,"
                        + FOUND_BY + " TEXT NOT NULL,"
                        + POS_SCORE + " INTEGER NOT NULL,"
                        + NEG_SCORE + " INTEGER NOT NULL,"
                        + FACTOR + " REAL NOT NULL,"
                        + SYNC + " INTEGER NOT NULL DEFAULT " + WIFI_SYNC_OK + ", "
                        + RATING + " INTEGER NOT NULL DEFAULT " + WIFI_NOLIKE + ", "
                        + RATING_TIME + " INTEGER NOT NULL"
                        + "); "
                        + "CREATE UNIQUE INDEX " + WifiModel.Table.BSSID + "_unique_index ON " + WifiModel.Table.TABLE_NAME + " (" + WifiModel.Table.BSSID + ");"
                        + "CREATE INDEX wifi_order_index ON " + WifiModel.Table.TABLE_NAME + " (" + Table.POS_SCORE + " - " + Table.NEG_SCORE+ ");"
                        + "CREATE INDEX " + WifiModel.Table.SSID + "_index ON " + WifiModel.Table.TABLE_NAME + " (" + WifiModel.Table.SSID + ");"
                        + "CREATE INDEX " + WifiModel.Table.SYNC + "_index ON " + WifiModel.Table.TABLE_NAME + " (" + WifiModel.Table.SYNC + ");"
                        + "CREATE INDEX " + WifiModel.Table.RATING + "_index ON " + WifiModel.Table.TABLE_NAME + " (" + WifiModel.Table.RATING + ");"
                        + "CREATE INDEX " + WifiModel.Table.POS_SCORE + "_index ON " + WifiModel.Table.TABLE_NAME + " (" + WifiModel.Table.POS_SCORE + ");"
                        + "CREATE INDEX " + WifiModel.Table.NEG_SCORE + "_index ON " + WifiModel.Table.TABLE_NAME + " (" + WifiModel.Table.NEG_SCORE + ");";



    }
}
