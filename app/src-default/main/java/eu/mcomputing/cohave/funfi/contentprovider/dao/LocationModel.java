package eu.mcomputing.cohave.funfi.contentprovider.dao;


import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.provider.BaseColumns;

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
public class LocationModel {

    public long _id;
    public long sensorTime;
    public long logTime;
    public float accuracy;
    public double altitude;
    public float bearing;
    public double latitude;
    public double longitude;
    public String provider;
    public float speed;
    public long fix;
    public boolean isMock;
    public boolean hasAccuracy;
    public boolean hasAltitude;
    public boolean hasBearing;
    public boolean hasSpeed;
    public int numSatellites;
    public int totalSatellites;
    public float averageSnr;
    public long timeToFix;
    public double battery;

    public LocationModel(Cursor c) {
        String[] cols = c.getColumnNames();

        for (int i = 0; i < cols.length; i++) {
            switch (cols[i]) {
                case Table._id:
                    this._id = c.getLong(i);
                    break;
                case Table.sensorTime:
                    this.sensorTime = c.getLong(i);
                    break;
                case Table.logTime:
                    this.logTime = c.getLong(i);
                    break;
                case Table.accuracy:
                    this.accuracy = c.getFloat(i);
                    break;
                case Table.altitude:
                    this.altitude = c.getDouble(i);
                    break;
                case Table.bearing:
                    this.bearing = c.getFloat(i);
                    break;
                case Table.latitude:
                    this.latitude = c.getDouble(i);
                    break;
                case Table.longitude:
                    this.longitude = c.getDouble(i);
                    break;
                case Table.provider:
                    this.provider = c.getString(i);
                    break;
                case Table.speed:
                    this.speed = c.getFloat(i);
                case Table.fix:
                    this.fix = c.getLong(i);
                    break;
                case Table.isMock:
                    this.isMock = c.getInt(i) == 1;
                    break;
                case Table.hasAccuracy:
                    this.hasAccuracy = c.getInt(i) == 1;
                    break;
                case Table.hasAltitude:
                    this.hasAltitude = c.getInt(i) == 1;
                    break;
                case Table.hasBearing:
                    this.hasBearing = c.getInt(i) == 1;
                    break;
                case Table.hasSpeed:
                    this.hasSpeed = c.getInt(i) == 1;
                    break;
                case Table.numSatellites:
                    this.numSatellites = c.getInt(i);
                    break;
                case Table.totalSatellites:
                    this.totalSatellites = c.getInt(i);
                    break;
                case Table.averageSnr:
                    this.averageSnr = c.getFloat(i);
                    break;
                case Table.timeToFix:
                    this.timeToFix = c.getLong(i);
                    break;
                case Table.battery:
                    this.battery = c.getDouble(i);
                    break;

            }
        }

    }

    public LocationModel(Location location, long attempt_stated, double battery) {
        Calendar c = Calendar.getInstance();
        this.sensorTime = c.getTimeInMillis();
        this.logTime = 0;

        this.accuracy = location.getAccuracy();
        this.altitude = location.getAltitude();
        this.bearing = location.getBearing();
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.provider = location.getProvider();
        this.speed = location.getSpeed();
        this.fix = location.getTime();
        this.isMock = location.isFromMockProvider();
        this.hasAccuracy = location.hasAccuracy();
        this.hasAltitude = location.hasAltitude();
        this.hasBearing = location.hasBearing();
        this.hasSpeed = location.hasSpeed();
        this.numSatellites = -1;
        this.totalSatellites = -1;
        this.averageSnr = -1;
        this.timeToFix = attempt_stated > 0 ? c.getTimeInMillis() - attempt_stated : -1;
        this.battery = battery;
    }


    public LocationModel(Location location, int num_satellites, int total_satellites, float average_snr, long attempt_started, double battery) {
        Calendar c = Calendar.getInstance();
        this.sensorTime = c.getTimeInMillis();
        this.logTime = 0;

        this.accuracy = location.getAccuracy();
        this.altitude = location.getAltitude();
        this.bearing = location.getBearing();
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.provider = location.getProvider();
        this.speed = location.getSpeed();
        this.fix = location.getTime();
        this.isMock = location.isFromMockProvider();
        this.hasAccuracy = location.hasAccuracy();
        this.hasAltitude = location.hasAltitude();
        this.hasBearing = location.hasBearing();
        this.hasSpeed = location.hasSpeed();
        this.numSatellites = num_satellites;
        this.totalSatellites = total_satellites;
        this.averageSnr = average_snr;
        this.timeToFix = attempt_started > 0 ? c.getTimeInMillis() - attempt_started : -1;
        this.battery = battery;
    }


    @Override
    public String toString() {
        return "LocationModel{" +
                "_id=" + _id +
                ", sensorTime=" + sensorTime +
                ", logTime=" + logTime +
                ", accuracy=" + accuracy +
                ", altitude=" + altitude +
                ", bearing=" + bearing +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", provider='" + provider + '\'' +
                ", speed=" + speed +
                ", fix=" + fix +
                ", isMock=" + isMock +
                ", hasAccuracy=" + hasAccuracy +
                ", hasAltitude=" + hasAltitude +
                ", hasBearing=" + hasBearing +
                ", hasSpeed=" + hasSpeed +
                ", numSatellites=" + numSatellites +
                ", totalSatellites=" + totalSatellites +
                ", averageSnr=" + averageSnr +
                ", timeToFix=" + timeToFix +
                ", battery="+ battery+
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LocationModel) {
            LocationModel other = (LocationModel) o;
            return this._id == other._id;
        }

        return false;
    }


    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(Table.sensorTime, sensorTime);
        Calendar c = Calendar.getInstance();
        values.put(Table.logTime, logTime == 0 ? c.getTimeInMillis() : logTime);
        values.put(Table.accuracy, accuracy);
        values.put(Table.altitude, altitude);
        values.put(Table.bearing, bearing);
        values.put(Table.latitude, latitude);

        values.put(Table.longitude, longitude);
        values.put(Table.provider, provider);
        values.put(Table.speed, speed);
        values.put(Table.fix, fix);
        values.put(Table.isMock, isMock);
        values.put(Table.hasAccuracy, hasAccuracy);

        values.put(Table.hasAltitude, hasAltitude);
        values.put(Table.hasBearing, hasBearing);
        values.put(Table.hasSpeed, hasSpeed);
        values.put(Table.numSatellites, numSatellites);
        values.put(Table.totalSatellites, totalSatellites);

        values.put(Table.averageSnr, averageSnr);
        values.put(Table.timeToFix, timeToFix);

        values.put(Table.battery, battery);

        return values;

    }

    public static final class Table {
        public static final String _id = BaseColumns._ID;
        public static final String sensorTime = "sensorTime";
        public static final String logTime = "logTime";
        public static final String accuracy = "accuracy";
        public static final String altitude = "altitude";
        public static final String bearing = "bearing";
        public static final String latitude = "latitude";
        public static final String longitude = "longitude";
        public static final String provider = "provider";
        public static final String speed = "speed";
        public static final String fix = "fix";
        public static final String isMock = "isMock";
        public static final String hasAccuracy = "hasAccuracy";
        public static final String hasAltitude = "hasAltitude";
        public static final String hasBearing = "hasBearing";
        public static final String hasSpeed = "hasSpeed";
        public static final String numSatellites = "numSatellites";
        public static final String totalSatellites = "totalSatellites";
        public static final String averageSnr = "averageSnr";
        public static final String timeToFix = "timeToFix";
        public static final String battery = "battery";

        public static final String TABLE_NAME = "location";
        public static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                + Table._id + " INTEGER PRIMARY KEY,"
                + Table.sensorTime + " INTEGER,"
                + Table.logTime + " INTEGER,"

                + Table.accuracy + " REAL,"
                + Table.altitude + " REAL,"
                + Table.bearing + " REAL,"
                + Table.latitude + " REAL,"
                + Table.longitude + " REAL,"
                + Table.provider + " VARCHAR,"
                + Table.speed + " REAL,"
                + Table.fix + " INTEGER,"

                + Table.isMock + " INTEGER,"
                + Table.hasAccuracy + " INTEGER,"
                + Table.hasAltitude + " INTEGER,"
                + Table.hasBearing + " INTEGER,"
                + Table.hasSpeed + " INTEGER,"

                + Table.numSatellites + " INTEGER,"
                + Table.totalSatellites + " INTEGER,"
                + Table.averageSnr + " REAL,"
                + Table.timeToFix + " INTEGER,"
                + Table.battery + " REAL"
                + "); "
                + "CREATE UNIQUE INDEX " + Table.sensorTime + "_index ON " + Table.TABLE_NAME + " (" + Table.sensorTime + ");"
                + "CREATE INDEX " + Table.accuracy + "_index ON " + Table.TABLE_NAME + " (" + Table.accuracy + ");"
                + "CREATE INDEX " + Table.latitude + "_index ON " + Table.TABLE_NAME + " (" + Table.latitude + ");"
                + "CREATE INDEX " + Table.longitude + "_index ON " + Table.TABLE_NAME + " (" + Table.longitude + ");";

    }


}
