package eu.mcomputing.cohave.funfi.helper.config;

import eu.mcomputing.cohave.funfi.helper.config.LocalConfig;
import eu.mcomputing.cohave.helper.crypt.LocalCryptHelper;

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
public class Config {
    //---------- CONFIG & KEYS ---------------------//
    public static String TAG = "FunFiapp";
    public static String appVersion = "10.9.5";
    public static String appId = "eu.mcomputing.cohave.funfi";
    public static String appName = "funfi";
    public static String app = appName + " " + appVersion;
    public static String apikey = LocalConfig.apikey;
    public static String googleKey = LocalConfig.googleKey;

    public static final int wifiSyncLimit = 10;
    public static final int wifiSyncInterval = 1 * 60 * 60;//seconds - 1 hour

    public static final String STATIC_PROVIDER_NAME = "static";

    public static final class URL {
        public static final String SERVER_PATH = LocalConfig.URL.SERVER_PATH;
        public static final String WIFI_FEED = LocalConfig.URL.WIFI_FEED;
        public static final String WIFI_UPLOAD = LocalConfig.URL.WIFI_UPLOAD;
        public static final String LOCATION_UPLOAD = LocalConfig.URL.LOCATION_UPLOAD;
        public static final String NETWORK_UPLOAD = LocalConfig.URL.NETWORK_UPLOAD;
        public static final String NETWORK_LOCATION_UPLOAD = LocalConfig.URL.NETWORK_LOCATION_UPLOAD;
        public static final String SCORE_GET_URL = LocalConfig.URL.SCORE_GET_URL;
        public static final String LEADERBOARD_GET_URL = LocalConfig.URL.LEADERBOARD_GET_URL;
        public static final String WIFI_SYNC_FEED_URL = LocalConfig.URL.WIFI_SYNC_FEED_URL;
        public static final String WIFI_INIT_DOWNLOAD = LocalConfig.URL.WIFI_INIT_DOWNLOAD;
        public static final String WIFI_STATIC_URL = LocalConfig.URL.WIFI_STATIC_URL;
        public static final String PLACE_VOTE_URL = LocalConfig.URL.PLACE_VOTE_URL;
    }

    public static final class SHP {
        public static final String key = "user-data";
        public static final String last_sync = "last_sync";

        public static final String score_location = "score_location" ;
        public static final String score_wifi_location = "score_wifi_location";
        public static final String score_wifi_network = "score_wifi_network";
        public static final String score_my_rating = "score_my_rating";
        public static final String score_other_rating = "score_other_rating";

        public static final String sensing_status = "sensing_status";
        public static final String sensing_status_time = "sensing_status_time";

        public static final String score_set_list = "score_set_list";
    }

    public static final class SenseStatus {
        public static final String not_sensing = "Neaktívne";
        public static final String sensing_gps = "Len lokácie [GPS]";
        public static final String sensing_agps = "Len lokácie [A-GPS]";
        public static final String sensing_gps_wifi = "WiFi & lokácie [GPS]";
        public static final String sensing_agps_wifi = "WiFi & lokácie [A-GPS]";
    }

    public static final class Database {
        public static final String DATABASE_NAME = "FUNFI_DB";
        public static final int DATABASE_VERSION = 9;
    }

    public static final class Network {
        public static final int NO_NETWORK = 0;
        public static final int MOBILE_NETWORK = 1;
        public static final int WIFI_NETWORK = 2;
    }

    public static final class Location {
        public static final int LOCATION_PROVIDER_STATUS_INACTIVE = -2;
        public static final int LOCATION_PROVIDER_STATUS_NOT_CHECKED = -1;
        public static final int LOCATION_PROVIDER_STATUS_NOT_ENABLED = 0;
        public static final int LOCATION_PROVIDER_STATUS_ENABLED = 1;
        public static final int LOCATION_PROVIDER_STATUS_ACTIVE = 2;

        public static final String SATELLITE_PROVIDER = "satellite";

        public static final long LOCATION_SENSE_MIN_INTERVAL_GPS = 2 * 1000;//minimum time interval between location updates, in milliseconds
        public static final float LOCATION_SENSE_MIN_DISTANCE_INTERVAL_GPS = 10; //minimum distance between location updates, in meters

        public static final long LOCATION_SENSE_MIN_INTERVAL_NETWORK = 30 * 1000;//minimum time interval between location updates, in milliseconds
        public static final float LOCATION_SENSE_MIN_DISTANCE_INTERVAL_NETWORK = 20; //minimum distance between location updates, in meters

        public static final long LOCATION_MAX_OLD_REPLACE = 60 * 1000;

        public static long LOCATION_KILL_GPS_AFTER = 1 * 60 * 1000; // turn off sensor if there are not new date for long time
        public static long LOCATION_STILL_SLEEP_TIME = 5 * 60 * 1000; // duration of not sensing if user was still
        public static long LOCATION_GPS_MAX_ATTEMPT_WAIT = 60 * 1000;
        public static long LOCATION_NETWORK_MAX_ATTEMPT_WAIT = 60 * 1000;
    }

    public static final class Wifi {
        public static final int minWifiStrength = 2;
    }

    public static final class Alarm {
        public static final long ALARM_SENSING_INTERVAL = 200 * 1000; /// !
        public static final int ALARM_SENSING_ID = 1;
        public static final int REFRESH_ALARM_SENSING_ID = 2;
    }
}
