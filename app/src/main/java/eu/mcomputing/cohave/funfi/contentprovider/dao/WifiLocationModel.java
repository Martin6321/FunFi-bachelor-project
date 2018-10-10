package eu.mcomputing.cohave.funfi.contentprovider.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.wifi.WifiManager;
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
public class WifiLocationModel implements Serializable {
    private static final int channels = 10; //DO NOT CHANGE !! signal levels logs can not be compared to new logs with different num of channels

    public long _id;
    public long sensor_time;
    public long log_time;
    public String bssid;
    public long location_sensorTime;

    public int RSSI; //received signal strength indicator of the current 802.11 network, in dBm.
    public int signalLevel; //based on RSSI and 10 channels

    public void setSignalLevel() {
        this.signalLevel = WifiManager.calculateSignalLevel(RSSI, channels);
    }

    public long timestamp; //timestamp in microseconds (since boot) when this result was last seen.

    public WifiLocationModel(String bssid, long location_sensorTime, int RSSI, long timestamp){
        Calendar c = Calendar.getInstance();

        this.sensor_time = c.getTimeInMillis();
        this.log_time = 0;
        this.RSSI = RSSI;
        setSignalLevel();
        this.timestamp = timestamp;
        this.bssid=bssid;
        this.location_sensorTime=location_sensorTime;

    }

//    public WifiLocationModel(long _id, long sensor_time, long log_time, long wifiNetwork_id, long location_id, int RSSI, int signalLevel, long timestamp) {
//        this._id = _id;
//        this.sensor_time = sensor_time;
//        this.log_time = log_time;
//        this.wifiNetwork_id = wifiNetwork_id;
//        this.location_id = location_id;
//        this.RSSI = RSSI;
//        setSignalLevel();
//        this.timestamp = timestamp;
//    }

    public WifiLocationModel(Cursor c){
        String[] cols = c.getColumnNames();

        for (int i=0; i<cols.length; i++){
            switch (cols[i]){
                case Table._ID:
                    this._id = c.getLong(i);
                    break;
                case Table.sensor_time:
                    this.sensor_time = c.getLong(i);
                    break;
                case Table.log_time:
                    this.log_time = c.getLong(i);
                    break;
                case Table.RSSI:
                    this.RSSI = c.getInt(i);
                    break;
                case Table.signalLevel:
                    this.signalLevel = c.getInt(i);
                    break;
                case Table.timestamp:
                    this.timestamp = c.getLong(i);
                    break;
                case Table.BSSID:
                    this.bssid = c.getString(i);
                    break;
                case Table.location_sensorTime:
                    this.location_sensorTime=c.getLong(i);
                    break;
            }
        }

    }

    public static String[] getListAttr(){
        return new String[]{
                Table._ID,
                Table.sensor_time,
                Table.log_time,
                Table.RSSI,
                Table.signalLevel,
                Table.timestamp,
                Table.BSSID,
                Table.location_sensorTime
        };
    }

    public ContentValues getContentValues() {
        Calendar c = Calendar.getInstance();
        ContentValues values = new ContentValues();
        values.put(Table.sensor_time,sensor_time);
        values.put(Table.log_time,log_time==0 ? c.getTimeInMillis() : log_time);
        values.put(Table.RSSI,RSSI);
        values.put(Table.signalLevel,signalLevel);
        values.put(Table.timestamp,timestamp);
        values.put(Table.BSSID,bssid);
        values.put(Table.location_sensorTime,location_sensorTime);
        return values;
    }

    @Override
    public String toString() {
        return "WifiLocationModel{" +
                "_id=" + _id +
                ", Bssid="+bssid+
                ", location_sensorTime="+location_sensorTime+
                ", sensor_time=" + sensor_time +
                ", log_time=" + log_time +
                ", RSSI=" + RSSI +
                ", signalLevel=" + signalLevel +
                ", timestamp=" + timestamp +
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
        public static final String location_sensorTime = "location_sensorTime";
        public static final String sensor_time = "sensor_time";
        public static final String log_time = "log_time";
        public static final String RSSI = "RSSI";
        public static final String signalLevel = "signalLevel";
        public static final String timestamp = "timestamp";
        public static final String BSSID = "bssid";

        public static final String TABLE_NAME = "wifiLocation";
        public static final String CREATE_SQL =
                "CREATE TABLE " + TABLE_NAME
                        + "(" + Table._ID + " INTEGER PRIMARY KEY, "
                        + Table.sensor_time + " INTEGER NOT NULL,"
                        + Table.log_time + " INTEGER NOT NULL,"
                        + Table.BSSID + " TEXT NOT NULL,"
                        + Table.location_sensorTime + " INTEGER NOT NULL,"
                        + Table.RSSI + " INTEGER NOT NULL,"
                        + Table.signalLevel + " INTEGER NOT NULL,"
                        + Table.timestamp + " INTEGER NOT NULL"
                        + "); "
                        + "CREATE UNIQUE INDEX wifiLocationRow_index ON "+Table.TABLE_NAME+" ("+Table.location_sensorTime+","+Table.BSSID+","+Table.sensor_time+");"
                        + "CREATE INDEX "+ Table.BSSID +"_index ON "+ Table.TABLE_NAME+" ("+ Table.BSSID +");"
                        + "CREATE INDEX "+ Table.location_sensorTime +"_index ON "+ Table.TABLE_NAME+" ("+ Table.location_sensorTime +");";

    }
}
