package eu.mcomputing.cohave.funfi.contentprovider.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

/*
 The MIT License (MIT)

 Copyright (c) 2015 Maros Cavojsky (www.mpage.sk), Martin Molnar, mComputing (www.mcomputig.eu)

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
public class PlaceVoteModel
{
    public long _id;
    public double latitude;
    public double longitude;
    public long poi_id;
    public String answer;

    public PlaceVoteModel(long _id, double latitude, double longitude, long poi_id, String answer)
    {
        this._id = _id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.poi_id = poi_id;
        this.answer = answer;
    }

    public PlaceVoteModel(Cursor c){
        String[] cols = c.getColumnNames();

        for (int i=0; i<cols.length; i++){
            switch (cols[i]){
                case Table._ID:
                    this._id = c.getLong(i);
                    break;
                case Table.latitude:
                    this.latitude = c.getDouble(i);
                    break;
                case Table.longitude:
                    this.longitude = c.getDouble(i);
                    break;
                case Table.poi_id:
                    this.poi_id = c.getLong(i);
                    break;
                case Table.answer:
                    this.answer = c.getString(i);
            }
        }

    }

    public static String[] getListAttr(){
        return new String[]{
                Table._ID,
                Table.latitude,
                Table.longitude,
                Table.poi_id,
                Table.answer
        };
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(Table._ID,this._id);
        values.put(Table.latitude,latitude);
        values.put(Table.longitude,longitude);
        values.put(Table.poi_id,poi_id);
        values.put(Table.answer,answer);
        return values;
    }

    @Override
    public String toString() {
        return "WifiStaticModel{" +
                "_id=" + _id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", poi_id=" + poi_id +
                ", answer="+answer +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WifiModel){
            WifiLocationModel other = (WifiLocationModel)o;
            return this._id==other._id;
        }

        return false;
    }

    public static final class Table {
        public static final String _ID = BaseColumns._ID;
        public static final String latitude = "latitude";
        public static final String longitude = "longitude";
        public static final String poi_id="poi_id";
        public static final String answer="answer";

        public static final String TABLE_NAME = "placeVote";
        public static final String CREATE_SQL =
                "CREATE TABLE " + TABLE_NAME
                        + "(" + Table._ID + " INTEGER PRIMARY KEY, "
                        + Table.latitude + " REAL,"
                        + Table.longitude + " REAL,"
                        + Table.poi_id + " INTEGER NOT NULL,"
                        + Table.answer + " TEXT"
                        + ");";

    }
}
