package eu.mcomputing.cohave.funfi.logger.tools;

import android.location.Location;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import eu.mcomputing.cohave.funfi.helper.config.Config;

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
public class GeneralUtil {


    public static String getFormattedDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getDefault());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //called without pattern
        return df.format(c.getTime());
    }


    public static String getFormattedDate(long time) {

        return GeneralUtil.getFormattedDateMillis(time * 1000);
    }

    public static String getFormattedDateMillis(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"); //called without pattern
        c.setTimeInMillis(time);
        return df.format(c.getTime());
    }

    public static String getFormattedTime(long time) {
        return getFormattedTimeMillis(time * 1000);
    }

    public static String getFormattedTimeMillis(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getDefault());
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); //called without pattern
        c.setTimeInMillis(time);
        return df.format(c.getTime());
    }


    /**
     * Return 0 if they are same, 1 if first is better, 2 if second is better
     *
     * @param a
     * @param b
     * @return int
     */
    public static int isBetter(Location a, Location b) {
        //if a is null second is primary better
        if (a == null)
            return 2;
        //if b is null first is primary better
        if (b == null)
            return 1;

        //if has same fix time (with different location is impossible to have this)
        if (a.getElapsedRealtimeNanos() == b.getElapsedRealtimeNanos()) {
            if (a.getAccuracy() == b.getAccuracy())
                return 0;
            return a.getAccuracy() < b.getAccuracy() ? 1 : 2;
        }

        Location newer, older;
        int newerI, olderI;
        if (a.getElapsedRealtimeNanos() > b.getElapsedRealtimeNanos()) {
            newer = a;
            newerI = 1;
            older = b;
            olderI = 2;
        } else {
            newer = b;
            newerI = 2;
            older = a;
            olderI = 1;
        }

        //if newer location to max old time has better accuracy we use that
        if (older.getElapsedRealtimeNanos() > newer.getElapsedRealtimeNanos() - Config.Location.LOCATION_MAX_OLD_REPLACE * 1000000
                && newer.getAccuracy() > older.getAccuracy()
                ) {
            return olderI;
        }

        //we return newer no matter if it is better
        return newerI;
    }

    public static boolean isExpired(long timestamp, long expire_time) {
        Calendar c = Calendar.getInstance();
        return c.getTimeInMillis() - timestamp >= expire_time;
    }

    public static boolean isPreciseLocation(Location location, float maxLimit) {
        if (location == null)
            return false;
        if (location.hasAccuracy() == false) {
            return false;
        }

        return location.getAccuracy() <= maxLimit;
    }

}
