package eu.mcomputing.cohave.funfi.contentprovider.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

import org.json.JSONException;
import org.json.JSONObject;

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
public class WifiNetworkModel {
    public long _id;
    public long remote_id;
    public String BSSID; // six-byte MAC address: XX:XX:XX:XX:XX:XX
    public String SSID;
    public int frequency;
    public String capabilities;
    public long logTime=0;

    public WifiNetworkModel(long _id, long remote_id, String BSSID, String SSID, int frequency, String capabilities) {
        this._id = _id;
        this.remote_id = remote_id;
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.frequency = frequency;
        this.capabilities = capabilities;
    }

    public WifiNetworkModel(long remote_id, String BSSID, String SSID, int frequency, String capabilities) {
        this.remote_id = remote_id;
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.frequency = frequency;
        this.capabilities = capabilities;
        this.logTime = 0;
    }

    public WifiNetworkModel(Cursor c){
        String[] cols = c.getColumnNames();

        for (int i=0; i<cols.length; i++){
            switch (cols[i]){
                case Table._ID:
                    this._id = c.getLong(i);
                    break;
                case Table.REMOTE_ID:
                    this.remote_id = c.getLong(i);
                    break;
                case Table.SSID:
                    this.SSID = c.getString(i);
                    break;
                case Table.BSSID:
                    this.BSSID = c.getString(i);
                    break;
                case Table.FREQUENCY:
                    this.frequency = c.getInt(i);
                    break;
                case Table.CAPABILITIES:
                    this.capabilities = c.getString(i);
                    break;
                case Table.LOG_TIME:
                    this.logTime = c.getLong(i);
                    break;
            }
        }
    }

    public static String[] getListAttr(){
        return new String[]{
                Table._ID,
                Table.REMOTE_ID,
                Table.SSID,
                Table.BSSID,
                Table.FREQUENCY,
                Table.CAPABILITIES,
                Table.LOG_TIME
        };
    }

    public ContentValues getContentValues(){
        Calendar c = Calendar.getInstance();
        ContentValues values = new ContentValues();
        values.put(Table.REMOTE_ID,remote_id);
        values.put(Table.SSID,SSID);
        values.put(Table.BSSID,BSSID);
        values.put(Table.FREQUENCY,frequency);
        values.put(Table.CAPABILITIES,capabilities);
        values.put(Table.LOG_TIME,logTime==0 ? c.getTimeInMillis() : logTime);
        return values;
    }

    public static ContentValues getUploadedContentValues(long remote_id){
        ContentValues values = new ContentValues();
        values.put(Table.REMOTE_ID,remote_id);
        return values;
    }

    @Override
    public String toString() {
        return "WifiNetworkModel{" +
                "_id=" + _id +
                "location_remote_id="+ remote_id+
                "logTime="+logTime+
                ", BSSID='" + BSSID + '\'' +
                ", SSID='" + SSID + '\'' +
                ", frequency=" + frequency +
                ", capabilities='" + capabilities + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WifiModel){
            WifiNetworkModel other = (WifiNetworkModel)o;
            return this._id==other._id;
        }

        return false;
    }

    public static final class Table {
        public static final String _ID = BaseColumns._ID;
        public static final String REMOTE_ID =  "location_remote_id";
        public static final String SSID = "ssid";
        public static final String BSSID = "bssid";
        public static final String FREQUENCY = "frequency";
        public static final String CAPABILITIES = "capabilities";
        public static final String LOG_TIME = "logTime";

        public static final String TABLE_NAME = "wifiNetwork";
        public static final String CREATE_SQL =
                "CREATE TABLE " + TABLE_NAME
                        + "(" + _ID + " INTEGER PRIMARY KEY, "
                        + REMOTE_ID+ " INTEGER NOT NULL,"
                        + LOG_TIME+ " INTEGER NOT NULL,"
                        + BSSID + " TEXT NOT NULL,"
                        + SSID + " TEXT NOT NULL,"
                        + FREQUENCY + " INTEGER NOT NULL,"
                        + CAPABILITIES + " TEXT NOT NULL"
                        + "); "
                        + "CREATE UNIQUE INDEX "+ BSSID+"_uniqe_index ON "+ TABLE_NAME+" ("+ BSSID+");"
                        + "CREATE INDEX "+ SSID+"_index ON "+ TABLE_NAME+" ("+ SSID+");"
                        + "CREATE INDEX "+ LOG_TIME+"_index ON "+ TABLE_NAME+" ("+ LOG_TIME+");"
                        + "CREATE INDEX "+ REMOTE_ID+"_index ON "+ TABLE_NAME+" ("+ REMOTE_ID+");";
    }
}
