package eu.mcomputing.cohave.funfi.logger.sensor;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Bundle;

import org.acra.ACRA;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.LocationModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiStaticModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.logger.tools.GeneralUtil;


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
public class LocationInfo {

    private static LocationManager locationManager;
    private static LocationListener gpsListener, networkListener, passiveListener;
    private static GpsStatus.Listener satelliteListener;
    
    private static Location currentLocation;
    private static Long currentLocationUpdate = 0L;
    private static Long startGPSat = 0L;

    private static Map<String, Long> sensorLastUpdate = new HashMap<>();
    private static Map<String, Integer> sensorLastLocationFailCycles = new HashMap<>();


    private Context context;
    
    public static class Status {
        public static int NETWORK_TYPE = Config.Network.NO_NETWORK;
        public static boolean CONNECTION_ENABLED = false;
        public static boolean ROAMING_ENABLED = false;
                
        public static int GPS_PROVIDER_STATUS = Config.Location.LOCATION_PROVIDER_STATUS_NOT_CHECKED;
        public static int NETWORK_PROVIDER_STATUS = Config.Location.LOCATION_PROVIDER_STATUS_NOT_CHECKED;
        public static int PASSIVE_PROVIDER_STATUS = Config.Location.LOCATION_PROVIDER_STATUS_NOT_CHECKED;

        public static boolean GPS_SENSING_STATUS = false;
        public static boolean NETWORK_SENSING_STATUS = false;
        public static boolean PASSIVE_SENSING_STATUS = false;

        public static Location LAST_LOCATION = null;
        public static int TIME_TO_FIRST_GPS_FIX = -1;
        public static Long TIME_OF_LAST_GPS_FIX = -1L;
        public static int NUM_OF_SATELLITES = -1;
        public static int TOTAL_NUM_OF_SATELLITES = -1;
        public static float SATELLITES_AVERAGE_SNR = -1;
        public static long GPS_LOCATION_FIX_ATTEMPT_STARTED = -1L;
        public static long TIME_OF_LAST_NETWORK_FIX = -1L;
        public static long NETWORK_LOCATION_FIX_ATTEMPT_STARTED = -1L;

        public static void setSensingStatus(final String provider, boolean status) {
            switch (provider) {
                case LocationManager.GPS_PROVIDER:
                    GPS_SENSING_STATUS = status;
                    return;
                case LocationManager.NETWORK_PROVIDER:
                    NETWORK_SENSING_STATUS = status;
                    return;
                case LocationManager.PASSIVE_PROVIDER:
                    PASSIVE_SENSING_STATUS = status;
                    return;
            }
        }

        public static int isProviderEnabled(LocationManager locationManager, final String provider) {
            switch (provider) {
                case LocationManager.GPS_PROVIDER:
                    GPS_PROVIDER_STATUS = locationManager.isProviderEnabled(provider) ?
                            Config.Location.LOCATION_PROVIDER_STATUS_ENABLED : Config.Location.LOCATION_PROVIDER_STATUS_NOT_ENABLED;
                    return GPS_PROVIDER_STATUS;
                case LocationManager.NETWORK_PROVIDER:
                    NETWORK_PROVIDER_STATUS = locationManager.isProviderEnabled(provider) ?
                            Config.Location.LOCATION_PROVIDER_STATUS_ENABLED : Config.Location.LOCATION_PROVIDER_STATUS_NOT_ENABLED;
                    return NETWORK_PROVIDER_STATUS;
                case LocationManager.PASSIVE_PROVIDER:
                    PASSIVE_PROVIDER_STATUS = locationManager.isProviderEnabled(provider) ?
                            Config.Location.LOCATION_PROVIDER_STATUS_ENABLED : Config.Location.LOCATION_PROVIDER_STATUS_NOT_ENABLED;
                    return PASSIVE_PROVIDER_STATUS;
            }
            return Config.Location.LOCATION_PROVIDER_STATUS_NOT_CHECKED;
        }

        public static void checkProviders(LocationManager locationManager) {
            isProviderEnabled(locationManager, LocationManager.GPS_PROVIDER);
            isProviderEnabled(locationManager, LocationManager.NETWORK_PROVIDER);
            isProviderEnabled(locationManager, LocationManager.PASSIVE_PROVIDER);
        }

        public static int getProviderStatus(final String provider) {
            switch (provider) {
                case LocationManager.GPS_PROVIDER:
                    return GPS_PROVIDER_STATUS;
                case LocationManager.NETWORK_PROVIDER:
                    return NETWORK_PROVIDER_STATUS;
                case LocationManager.PASSIVE_PROVIDER:
                    return PASSIVE_PROVIDER_STATUS;
            }
            return Config.Location.LOCATION_PROVIDER_STATUS_NOT_ENABLED;
        }

        /**
         * Check whether the device is connected, and if so, whether the connection
         * is wifi or mobile (it could be something else).
         */
        public static void checkNetworkStatus(Context context) {
            int internet_status = Config.Network.NO_NETWORK;
            boolean roaming_status = false;

            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

            if (activeInfo != null && activeInfo.isConnected() && activeInfo.isAvailable()) {
                boolean wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
                boolean mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;

                if (wifiConnected) {
                    internet_status = Config.Network.WIFI_NETWORK;
                    MyLog.log("LocationInfo.Status", "Sensor: " + "Wifi connected");
                } else if (mobileConnected) {
                    internet_status = Config.Network.MOBILE_NETWORK;
                    MyLog.log("LocationInfo.Status", "Sensor: " + "Mobile connected");
                }
                CONNECTION_ENABLED=true;
                roaming_status = activeInfo.isRoaming();
            } else {
                internet_status = Config.Network.NO_NETWORK;
                CONNECTION_ENABLED = false;
                MyLog.log("LocationInfo.Status", "Sensor: " + "No internet connection");
            }

            NETWORK_TYPE = internet_status;
            ROAMING_ENABLED = roaming_status;
        }

    }

    public LocationInfo(Context context) {
        this.context = context;
    }

    //------------------ FUNCTIONS ---------------//

    /**
     * Start sensing for location. In first lines method check location providers and network status.
     * Then if WiFi is connected disable GPS if running and start Network location. If is not WiFi
     * connection then start GPS sensing if available.
     */
    public void senseLocation() {

        Status.checkProviders(getLocationManager());
        Status.checkNetworkStatus(this.context);

        // compare avalible wifis with static wifis in local database
       WifiInformation wifiInformation = new WifiInformation(context);
       List<ScanResult> results = wifiInformation.getAvalibleWifiNetworks();
       if (results.size() > 0)
            if (compareWithStaticNetworks(results))
                return;


        //if connected to wifi network disable GPS and get only network location
        if (
                Status.NETWORK_PROVIDER_STATUS == Config.Location.LOCATION_PROVIDER_STATUS_ENABLED &&
                Status.NETWORK_TYPE == Config.Network.WIFI_NETWORK && Status.CONNECTION_ENABLED
        ){
            if (Status.GPS_SENSING_STATUS) {
                removeListener(LocationManager.GPS_PROVIDER);
                MyLog.log(getClass(), "LocationSense: GPS paused due to WiFi connection");
            }

            senseNetworkLocation();
            MyLog.log(getClass(), "LocationSense: sensing network location");
            return;
        }

        MyLog.log(getClass(), "LocationSense: sensing Gps location");
        senseGPSLocation();

        if (Status.GPS_SENSING_STATUS == false) {
            MyLog.log(getClass(),"LocationSense: sensing network location, gps is off");
            senseNetworkLocation();
        }
    }

    // compares scanned wifis with local database records
    private boolean compareWithStaticNetworks(List<ScanResult> wifiList)
    {
        for (ScanResult wifi : wifiList) {
            Cursor cursor = context.getContentResolver().query(
                    WifiContentProvider.WIFI_STATIC_CONTENT_URI,
                    new String[]{"bssid","latitude","longitude"},
                    "bssid=?",
                    new String[]{wifi.BSSID},
                    WifiStaticModel.Table._ID
            );

            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                Location loc = new Location(Config.STATIC_PROVIDER_NAME);
                loc.setLatitude(cursor.getDouble(1));
                loc.setLongitude(cursor.getDouble(2));

                newLocation(loc, Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED);
                return true;
            }
        }
        return false;
    }

    public static Location getCurrentLocation() {
        return currentLocation;
    }

    public static Long getCurrentLocationUpdate() {
        return currentLocationUpdate;
    }

    /**
     * Stop Location updates and set listeners to null. This does not stop alarm.
     *
     * @param context
     */
    public static void stopLocationUpdates(Context context) {
        MyLog.log("LocationInfo", "Stopping Location updates");
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        removeListener(LocationManager.GPS_PROVIDER, null, locationManager);
        removeListener(LocationManager.NETWORK_PROVIDER, null, locationManager);
        removeListener(LocationManager.PASSIVE_PROVIDER, null, locationManager);

        MyLog.log("LocationInfo", "Location updates stopped");
    }

    //-------------------------------------------//

    /**
     * Save location data to database, log wifi ssids to location time
     * @param data
     */
    private void logData(LocationModel data) {
        MyLog.log(getClass(), "saving location data : " + data.toString());
        Uri uri = context.getContentResolver().insert(WifiContentProvider.LOCATION_CONTENT_URI, data.getContentValues());
        long id = -1;
        if (uri != null ) {
            id = Long.parseLong(uri.getPathSegments().get(WifiContentProvider.DATA_ITEM));

            MyLog.log(getClass(),"saving success "+ id+ ": scanning wifi");
            WifiInformation wifiInformation = new WifiInformation(context);
            boolean wifiStatus = wifiInformation.scanWifiLocation(data.sensorTime);
            if (wifiStatus){
                if (data.provider.equals(LocationManager.GPS_PROVIDER)) {
                    saveSenseStatus(Config.SenseStatus.sensing_gps_wifi);
                }else if (data.provider.equals(LocationManager.NETWORK_PROVIDER)){
                    saveSenseStatus(Config.SenseStatus.sensing_agps_wifi);
                }else{
                    saveSenseStatus(Config.SenseStatus.not_sensing);
                }
            }else{
                if (data.provider.equals(LocationManager.GPS_PROVIDER)) {
                    saveSenseStatus(Config.SenseStatus.sensing_gps);
                }else if (data.provider.equals(LocationManager.NETWORK_PROVIDER)){
                    saveSenseStatus(Config.SenseStatus.sensing_agps);
                }else{
                    saveSenseStatus(Config.SenseStatus.not_sensing);
                }
            }
        }

        MyLog.log(getClass(), "saved location " + (uri == null ? "failed" : id));
    }

    private void saveSenseStatus(String status){
        Calendar c = Calendar.getInstance();

        SharedPreferences sharedPref = context.getSharedPreferences(Config.SHP.key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Config.SHP.sensing_status,status);
        editor.putLong(Config.SHP.sensing_status_time, c.getTimeInMillis());
        editor.commit();
    }

    /**
     * Get instance of location manager
     *
     * @return
     */
    private LocationManager getLocationManager() {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
        return locationManager;
    }

    /**
     * Set last sensor update
     *
     * @param provider
     * @param update
     */
    private static void setSensorLastUpdate(final String provider, Long update) {
        sensorLastUpdate.put(provider, update);
    }

    /**
     * Set sensor Last location fail cycles
     *
     * @param provider
     * @param cycles
     */
    private static void setSensorLastLocationFailCycles(final String provider, int cycles) {
        sensorLastLocationFailCycles.put(provider, cycles);
    }

    /**
     * Increment cycles by one
     *
     * @param provider
     */
    private static void incrementSensorLastLocationFailCycles(final String provider) {
        if (provider.compareTo(LocationManager.GPS_PROVIDER) == 0) return;

        int cycles = getSensorLastLocationFailCycles(provider);
        setSensorLastLocationFailCycles(provider, cycles + 1);
    }

    /**
     * Set current location into variable
     * @param mCurrentLocation
     */
    private static void setCurrentLocation(Location mCurrentLocation) {
        Calendar c = Calendar.getInstance();
        currentLocation = mCurrentLocation;
        currentLocationUpdate = c.getTimeInMillis();

        MyLog.log("LocationInfo", " loc " + currentLocation.getProvider() + " : " + GeneralUtil.getFormattedDateMillis(currentLocation.getTime()));
    }

    /**
     * Reset current location. But it does not stop sensing ! IT only set currentLocation = null and currentLocationUpdate = 0 and setSensorLastUpdate = 0
     */
    private static void resetCurrentLocation() {
        if (currentLocation != null)
            setSensorLastUpdate(currentLocation.getProvider(), 0L);

        LocationInfo.currentLocation = null;
        LocationInfo.currentLocationUpdate = 0L;
    }

    /**
     * Get last sensor update for provider, if not exists than 0 is returned
     * @param provider
     * @return
     */
    private static Long getSensorLastUpdate(final String provider) {
        if (sensorLastUpdate.containsKey(provider) == false)
            return 0L;

        return sensorLastUpdate.get(provider);
    }

    /**
     * Get last sensor location fail cycles for provider, if not exists than 0 is returned
     * @param provider
     * @return
     */
    private static int getSensorLastLocationFailCycles(final String provider) {
        if (sensorLastLocationFailCycles.containsKey(provider) == false) {
            return 0;
        }

        return sensorLastLocationFailCycles.get(provider);
    }

    /**
     * Return location Listener for provider or null if not exists
     * @param provider
     * @return
     */
    private static LocationListener getLocationListener(String provider) {

        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                return gpsListener;
            case LocationManager.NETWORK_PROVIDER:
                return networkListener;
            case LocationManager.PASSIVE_PROVIDER:
                return passiveListener;
        }
        return null;
    }

    /**
     * Return Gps Status listener
     * @return
     */
    private static GpsStatus.Listener getSatelliteListener() {
        return satelliteListener;
    }


    private static boolean getLocationListenerStatus(String provider) {
        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                return gpsListener != null;
            case LocationManager.NETWORK_PROVIDER:
                return networkListener != null;
            case LocationManager.PASSIVE_PROVIDER:
                return passiveListener != null;
            case Config.Location.SATELLITE_PROVIDER:
                return satelliteListener != null;
        }
        return false;
    }

    /**
     * Set location listener for provider and reset FailCycles and UpdateTime, set Sensing Status
     *
     * @param provider
     * @param locationListener
     */
    private static void setLocationListener(String provider, LocationListener locationListener) {
        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                gpsListener = locationListener;
                break;
            case LocationManager.NETWORK_PROVIDER:
                networkListener = locationListener;
                break;
            case LocationManager.PASSIVE_PROVIDER:
                passiveListener = locationListener;
                break;
            default:
                return;
        }

        setSensorLastUpdate(provider, 0L);
        Status.setSensingStatus(provider, locationListener != null);

    }

    private static void setLocationListener(String provider, LocationListener locationListener, GpsStatus.Listener listener) {
        MyLog.log("LocationInfo","Adding listener for "+provider);
        setLocationListener(provider, locationListener);
        if (provider.compareTo(LocationManager.GPS_PROVIDER) == 0) {
            satelliteListener = listener;
        }
    }

    /**
     * Remove updates from location provider, listener can be null to use current
     *
     * @param provider
     * @param listener
     * @param locationManager
     */
    private static void removeListener(final String provider, LocationListener listener, LocationManager locationManager) {
        resetCurrentLocation();
        MyLog.log("LocationInfo","Removing listener for "+provider);

        setSensorLastUpdate(provider, 0L);

        Status.setSensingStatus(provider, false);

        if (locationManager == null) {
            return;
        }
        if (listener == null) {
            listener = getLocationListener(provider);
            if (listener == null)
                return;
        }

        locationManager.removeUpdates(listener);

        if (provider.compareTo(LocationManager.GPS_PROVIDER) == 0) {
            Status.GPS_LOCATION_FIX_ATTEMPT_STARTED = -1;

            GpsStatus.Listener satellite = getSatelliteListener();
            if (satellite != null) {
                locationManager.removeGpsStatusListener(getSatelliteListener());
            }
            setLocationListener(provider, null, null);
            //Status.GPS_SATELLITE_STATUS = "";


        } else if (provider.compareTo(LocationManager.NETWORK_PROVIDER) == 0) {
            Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED = -1L;
            setLocationListener(provider, null);

        } else {
            setLocationListener(provider, null);
        }

    }

    /**
     * Remove updates from location provider, listener can be null to use current
     *
     * @param provider
     * @param listener
     */
    private void removeListener(final String provider, LocationListener listener) {
        removeListener(provider, listener, getLocationManager());
    }

    /**
     * Remove call remove Listener function if listener is broken (is null)
     *
     * @param provider
     * @param listener
     */
    private void removeIfBrokenListener(final String provider, LocationListener listener) {
        LocationListener listn = getLocationListener(provider);
        if (listn == null) {
            removeListener(provider, listener);
        }
    }

    private void removeListener(final String provider) {
        removeListener(provider, null);
    }

    /**
     * Get last known location for fast fix, call before Status.isProviderEnabled(getLocationManager(),provider);
     * if specified provider is not enabled than null is returned.
     * if specified provider is enabled and if last known location is null than it increment fail cycles,
     * otherwise reset cycles to 0 and last sensor update time and return last known location
     *
     * @param provider
     * @return
     */
    private Location getLastKnownLocation(final String provider) {

        if (Status.getProviderStatus(provider) != Config.Location.LOCATION_PROVIDER_STATUS_ENABLED)
            return null;

        Location lastKnownLocation = getLocationManager().getLastKnownLocation(provider);
        if (lastKnownLocation == null) {
            incrementSensorLastLocationFailCycles(provider);
        } else {
            setSensorLastLocationFailCycles(provider, 0);
            setSensorLastUpdate(provider, lastKnownLocation.getTime());
        }

        return lastKnownLocation;
    }


    /**
     * List for network location with defined minTime and minDistance
     *
     * @param minTime
     * @param minDistance
     */
    private void listenForNetworkLocation(long minTime, float minDistance) {
        if (Status.NETWORK_PROVIDER_STATUS != Config.Location.LOCATION_PROVIDER_STATUS_ENABLED) {
            MyLog.log(getClass(), "Location provider " + LocationManager.NETWORK_PROVIDER + " is not enabled");
            return;
        }

        final String provider = LocationManager.NETWORK_PROVIDER;
        LocationListener locationListener = getLocationListener(provider);

        //if locationListener is not null we are sensing
        Status.setSensingStatus(provider, locationListener != null);

        MyLog.log(getClass(), "LocationListener " + provider + " is " + (locationListener == null ? "null" : "not null"));

        //if listening is still in progress from previous alarm call
        if (locationListener != null) {
            //avoid to create parallel listeners
            return;
        }

        Calendar c = Calendar.getInstance();

        Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED = c.getTimeInMillis();

        try {
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        Status.TIME_OF_LAST_NETWORK_FIX = location.getTime();

                        MyLog.log(getClass(), provider + " Location  " + location.toString());
                        //we do not log location here but in passive listener
                        newLocation(location, Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED);

                        Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED = -1L;
                      //  removeListener(provider, null);
                        removeIfBrokenListener(provider,this);
                    } else {
                        MyLog.log(getClass(), provider + " Location  null");
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                    MyLog.log(getClass(), "Location provider " + provider + " status " + status);
                    //removeIfBrokenListener(provider,this);
                }

                public void onProviderEnabled(String provider) {
                    MyLog.log(getClass(), "Location provider " + provider + " enabled");
                    //removeIfBrokenListener(provider, this);
                    removeListener(provider, null);
                    Status.checkProviders(getLocationManager());
                }

                public void onProviderDisabled(String provider) {
                    MyLog.log(getClass(), "Location provider " + provider + " disabled");
                    //removeIfBrokenListener(provider, this);
                    removeListener(provider, null);
                    Status.checkProviders(getLocationManager());
                }
            };

            // Register the listener with the Location Manager to receive location updates
            getLocationManager().requestLocationUpdates(
                    provider,
                    minTime,
                    minDistance,
                    locationListener
            );


            setLocationListener(provider, locationListener);

        } catch (IllegalArgumentException e) {
            //if provider is null
            ACRA.getErrorReporter().handleSilentException(e);
            e.printStackTrace();
        }

    }

    /**
     * Listen for GPS location
     */
    private void listenForGPSLocation() {
        if (Status.GPS_PROVIDER_STATUS != Config.Location.LOCATION_PROVIDER_STATUS_ENABLED) {
            MyLog.log(getClass(), "Location provider " + LocationManager.GPS_PROVIDER + " is not enabled");
            return;
        }

        final String provider = LocationManager.GPS_PROVIDER;
        LocationListener locationListener = getLocationListener(provider);

        //if locationListener is not null we are sensing
        Status.setSensingStatus(provider, locationListener != null);

        MyLog.log(getClass(), "LocationListener " + provider + " is " + (locationListener == null ? "null" : "not null"));

        //if listening is still in progress from previous alarm call
        if (locationListener != null) {
            MyLog.log(getClass(), "GPS : we are already listenning");
            //avoid to create parallel listeners
            return;
        }


        try {
            //create listener for passive provider which will be responsible for collecting location data
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        MyLog.log(getClass(), provider + " Location  " + location.toString());
                        //we do not log location here but in passive listener
                        if (location.isFromMockProvider()) {
                            Calendar c = Calendar.getInstance();
                            Status.TIME_OF_LAST_GPS_FIX = c.getTimeInMillis();//for debug
                        }
                        newLocation(location, Status.TIME_OF_LAST_GPS_FIX, Status.GPS_LOCATION_FIX_ATTEMPT_STARTED,
                                Status.SATELLITES_AVERAGE_SNR, Status.TOTAL_NUM_OF_SATELLITES, Status.NUM_OF_SATELLITES);
                        removeIfBrokenListener(provider, this);
                    } else {
                        MyLog.log(getClass(), provider + " Location  null");
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                    MyLog.log(getClass(), "Location provider " + provider + " status " + status);
                    removeIfBrokenListener(provider, this);
                }

                public void onProviderEnabled(String provider) {
                    MyLog.log(getClass(), "Location provider " + provider + " enabled");
                    removeIfBrokenListener(provider, this);
                    Status.checkProviders(getLocationManager());
                }

                public void onProviderDisabled(String provider) {
                    MyLog.log(getClass(), "Location provider " + provider + " disabled");
                    removeIfBrokenListener(provider, this);
                    removeListener(provider, null);
                    Status.checkProviders(getLocationManager());
                }
            };

            // Register the listener with the Location Manager to receive location updates
            getLocationManager().requestLocationUpdates(
                    provider,
                    Config.Location.LOCATION_SENSE_MIN_INTERVAL_GPS,
                    Config.Location.LOCATION_SENSE_MIN_DISTANCE_INTERVAL_GPS,
                    locationListener
            );

            GpsStatus.Listener satellite = listenForSatellites();
            if (satellite == null) {
                throw new IllegalArgumentException("GPS Satellite listener is null.");
            }

            setLocationListener(provider, locationListener, satellite);

        } catch (IllegalArgumentException e) {
            ACRA.getErrorReporter().handleSilentException(e);
            e.printStackTrace();
        }

    }

    /**
     * Sense for GPS Satellites info only works when GPS listener is on
     *
     * @return
     */
    private GpsStatus.Listener listenForSatellites() {
        if (getSatelliteListener() != null) {
            getLocationManager().removeGpsStatusListener(getSatelliteListener());
        }

        GpsStatus.Listener listener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                MyLog.log(getClass(), "GPS-STATUS event " + event);
                String text = "Event " + event + " : " + GeneralUtil.getFormattedDate() + "\n";
                GpsStatus gpsStatus = getLocationManager().getGpsStatus(null);

                Status.TOTAL_NUM_OF_SATELLITES = -1;
                Status.NUM_OF_SATELLITES = -1;
                Status.SATELLITES_AVERAGE_SNR = -1;

                switch (event) {
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        Status.TIME_TO_FIRST_GPS_FIX = gpsStatus.getTimeToFirstFix();
                        MyLog.log(getClass(), "GPS-STATUS gps time to first fix " + gpsStatus.getTimeToFirstFix());
                        text += "GPS-STATUS time to first fix " + gpsStatus.getTimeToFirstFix() + "\n";
                        break;

                    case GpsStatus.GPS_EVENT_STARTED:
                        MyLog.log(getClass(), "GPS-STATUS gps started");
                        Status.GPS_PROVIDER_STATUS = Config.Location.LOCATION_PROVIDER_STATUS_ACTIVE;
                        Status.TIME_TO_FIRST_GPS_FIX = -1;
                        Status.GPS_LOCATION_FIX_ATTEMPT_STARTED = -1;
                        text += "GPS started\n";
                        break;

                    case GpsStatus.GPS_EVENT_STOPPED:
                        MyLog.log(getClass(), "GPS-STATUS gps stopped");
                        Status.GPS_PROVIDER_STATUS = Config.Location.LOCATION_PROVIDER_STATUS_INACTIVE;
                        Status.TIME_TO_FIRST_GPS_FIX = -1;
                        Status.GPS_LOCATION_FIX_ATTEMPT_STARTED = -1;
                        text += "GPS stopped\n";
                        break;

                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
                        int total = 0;
                        int used = 0;
                        float snr_sum = 0;
                        String info = "";
                        for (GpsSatellite satellite : satellites) {
                            //MyLog.log(getClass(),"GPS-STATUS: Prn: "+ satellite.getPrn()+", Az: "+satellite.getAzimuth()+ ", El: "+ satellite.getElevation()+ ", Snr: "+ satellite.getSnr());
                            if (satellite.usedInFix() && satellite.getSnr() > 0) {
                                used++;
                                snr_sum += satellite.getSnr();
                            }
                            info += satellite.usedInFix() ? "F" : "-";
                            info += satellite.hasAlmanac() ? "A" : "-";
                            info += satellite.hasEphemeris() ? "E" : "-";
                            info += "|";

                            info += " Prn: " + satellite.getPrn() + ", Snr: " + satellite.getSnr() + ", Az: " + satellite.getAzimuth() + ", El: " + satellite.getElevation() + "\n";

                            if (satellite.getSnr() > 0)
                                total++;
                        }
                        Calendar c = Calendar.getInstance();
                        if (used > 0) {

                            Status.TIME_OF_LAST_GPS_FIX = c.getTimeInMillis();
                            Status.SATELLITES_AVERAGE_SNR = snr_sum / used;
                            Status.GPS_LOCATION_FIX_ATTEMPT_STARTED = -1;
                        } else if (Status.GPS_LOCATION_FIX_ATTEMPT_STARTED == -1) {
                            Status.GPS_LOCATION_FIX_ATTEMPT_STARTED = c.getTimeInMillis();
                        }

                        Status.TOTAL_NUM_OF_SATELLITES = total;
                        Status.NUM_OF_SATELLITES = used;

                        MyLog.log(getClass(), "lastfix " + (Status.TIME_OF_LAST_GPS_FIX <= 0 ? Status.TIME_OF_LAST_GPS_FIX : GeneralUtil.getFormattedDateMillis(Status.TIME_OF_LAST_GPS_FIX)));
                        MyLog.log(getClass(), "GPS-STATUS num satelites " + used + "/" + total);

                        text += "Num satelites " + used + "/" + total + "\n";
                        text += info;
                        text += "\n F-used in Fix, A - Almanac, E - Ephemeris\n";
                        text += "SNR - the higher the better\n";

                        break;
                }

                MyLog.log(getClass(),"Satellite status "+text);

            }
        };

        getLocationManager().addGpsStatusListener(listener);

        return listener;
    }

    private int newLocation(Location location, long attempt_started) {
        return newLocation(location, -1, attempt_started, -1, -1, -1);
    }

    /**
     * Update last stored location from provider if new location is new or better
     *
     * @param location
     */
    private int newLocation(Location location, long lastFix, long attempt_started, float average_snr, int total_satellites, int num_satellites) {

        if (location == null) {
            MyLog.log(getClass(), "new Location is null");
            return -1;
        }

//        if (LocationUtil.isExpiredLocation(location, Config.LOCATION_MAX_OLD_MILLIS)) {
//            sendNotification(nTag + " location expired " + GeneralUtil.getFormattedDateMillis(location.getTime()), nId);
//            MyLog.log(getClass(), "new " + provider + " Location expired " + GeneralUtil.getFormattedDateMillis(location.getTime()));
//            resetCurrentLocation();
//            return 1;
//        }

        setSensorLastUpdate(location.getProvider(), location.getTime());

        Location current = getCurrentLocation();
        int compare = GeneralUtil.isBetter(location, current);
        setCurrentLocation(compare == 2 ? current : location);

        MyLog.log(getClass(), "new Location  " + location.getProvider() + " " + (compare == 2 ? "worse" : "better|same") + " - " +
                        (location == null ? "null " : location.getTime() + ": " + location.getAccuracy())
                        + " <-> curr: " + (current == null ? "null " : current.getTime() + ": " + current.getAccuracy())
        );


        LocationModel data = null;

        double battery = MyBattery.getBatteryLevel(context);

        current= getCurrentLocation();
        switch (current.getProvider()) {
            case LocationManager.GPS_PROVIDER:
                data = new LocationModel(current, num_satellites, total_satellites, average_snr, attempt_started,battery);
                break;
            case LocationManager.NETWORK_PROVIDER:
                data = new LocationModel(getCurrentLocation(), attempt_started,battery);
                break;
            case Config.STATIC_PROVIDER_NAME:
                data = new LocationModel(getCurrentLocation(), attempt_started,battery);
                break;
        }

        logData(data);

        return 6;
    }

    /**
     * Sense for location updates from Network provider.
     */
    private void senseNetworkLocation() {
        Status.checkNetworkStatus(this.context);
        Status.isProviderEnabled(getLocationManager(), LocationManager.NETWORK_PROVIDER);
        if (Status.NETWORK_PROVIDER_STATUS != Config.Location.LOCATION_PROVIDER_STATUS_ENABLED) {
            MyLog.log(getClass(), "Location provider " + LocationManager.NETWORK_PROVIDER + " is not enabled");
            removeListener(LocationManager.NETWORK_PROVIDER, null);
            //provider is not enabled we can not set listener
            return;
        }

        if (Status.NETWORK_SENSING_STATUS) {
            MyLog.log(getClass(), "N: Network is sensing");
            if (Status.NETWORK_TYPE != Config.Network.WIFI_NETWORK || Status.CONNECTION_ENABLED == false) {
                removeListener(LocationManager.NETWORK_PROVIDER);
                return;
            }
        }

        MyLog.log(getClass(), "Network location fix attempt started: " + Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED);

//        if (Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED > 0
//                && GeneralUtil.isExpired(Status.NETWORK_LOCATION_FIX_ATTEMPT_STARTED, Config.Location.LOCATION_NETWORK_MAX_ATTEMPT_WAIT)) {
//
//            removeListener(LocationManager.NETWORK_PROVIDER, null);
//            return;
//        }

        if (Status.NETWORK_TYPE == Config.Network.MOBILE_NETWORK || Status.CONNECTION_ENABLED == true) {
            if (Status.GPS_SENSING_STATUS == false) {
                if (Status.NETWORK_TYPE == Config.Network.WIFI_NETWORK && Status.CONNECTION_ENABLED) {
                    MyLog.log(getClass(), "N: WiFi network connected listenning for location");
                    listenForNetworkLocation(Config.Location.LOCATION_SENSE_MIN_INTERVAL_NETWORK, Config.Location.LOCATION_SENSE_MIN_DISTANCE_INTERVAL_NETWORK);
                    return;
                } else {
                    MyLog.log(getClass(), "N: WiFi network not connected listenning for onetime location");
                    //one time listen for network location
                    listenForNetworkLocation(0, 0);
                }
                return;
            } else {
                MyLog.log(getClass(), "N: GPS is already sensing");
            }
        } else {
            MyLog.log(getClass(), "N: no network removing listener");
            removeListener(LocationManager.NETWORK_PROVIDER, null);
            return;
        }

    }

    /**
     * Sense for location updates from GPS provider.
     */
    private void senseGPSLocation() {
        Status.checkNetworkStatus(this.context);
        Status.isProviderEnabled(getLocationManager(), LocationManager.GPS_PROVIDER);
        if (Status.GPS_PROVIDER_STATUS != Config.Location.LOCATION_PROVIDER_STATUS_ENABLED) {
            MyLog.log(getClass(), "Location provider " + LocationManager.GPS_PROVIDER + " is not enabled");
            removeListener(LocationManager.GPS_PROVIDER, null);
            return;
        }

        if (Status.NETWORK_SENSING_STATUS == true) {
            if (Status.NETWORK_TYPE == Config.Network.WIFI_NETWORK && Status.CONNECTION_ENABLED) {
                MyLog.log(getClass(), "GPS: Network sensing already.");
                return;
            } else {
                removeListener(LocationManager.NETWORK_PROVIDER);
            }
        }

        Calendar c = Calendar.getInstance();
        if (Status.GPS_SENSING_STATUS == true) {
            MyLog.log(getClass(), "GPS: " + Status.GPS_LOCATION_FIX_ATTEMPT_STARTED + ", " + Status.TIME_TO_FIRST_GPS_FIX + ", " + GeneralUtil.getFormattedDateMillis(startGPSat));
            //it was not fix yet
            if (Status.GPS_LOCATION_FIX_ATTEMPT_STARTED > 0) {
                if (Status.TIME_TO_FIRST_GPS_FIX > 0 && GeneralUtil.isExpired(Status.GPS_LOCATION_FIX_ATTEMPT_STARTED, Config.Location.LOCATION_GPS_MAX_ATTEMPT_WAIT)) {
                    startGPSat = c.getTimeInMillis() + Config.Location.LOCATION_STILL_SLEEP_TIME;
                    MyLog.log(getClass(), "GPS: next fix failed. retry at " + GeneralUtil.getFormattedTimeMillis(startGPSat));
                    removeListener(LocationManager.GPS_PROVIDER, null);

                } else if (Status.TIME_TO_FIRST_GPS_FIX < 0 && GeneralUtil.isExpired(Status.GPS_LOCATION_FIX_ATTEMPT_STARTED, Config.Location.LOCATION_KILL_GPS_AFTER)) {
                    startGPSat = c.getTimeInMillis() + Config.Location.LOCATION_STILL_SLEEP_TIME;
                    MyLog.log(getClass(), "GPS: first fix failed. retry at " + GeneralUtil.getFormattedTimeMillis(startGPSat));
                    removeListener(LocationManager.GPS_PROVIDER, null);
                } else {
                    MyLog.log(getClass(),"GPS: GPS is listening.");
                }
                //it was fix
            } else {
                if (GeneralUtil.isExpired(Status.TIME_OF_LAST_GPS_FIX, 300000)) {
                    startGPSat = 0L;
                    removeListener(LocationManager.GPS_PROVIDER, null);
                    MyLog.log(getClass(),"GPS: expired last fix at " + GeneralUtil.getFormattedTimeMillis(Status.TIME_OF_LAST_GPS_FIX));
                } else {
                    MyLog.log(getClass(),"GPS: last fix at " + GeneralUtil.getFormattedTimeMillis(Status.TIME_OF_LAST_GPS_FIX));
                }
            }
        } else {
            if (startGPSat == 0 || c.getTimeInMillis() > startGPSat) {
                startGPSat = 0L;
                MyLog.log(getClass(),"GPS: GPS is starting listening.");
                removeListener(LocationManager.NETWORK_PROVIDER, null);
                listenForGPSLocation();
            } else {
                MyLog.log(getClass(),"GPS: fix failed. retry at " + GeneralUtil.getFormattedTimeMillis(startGPSat));
            }
        }
    }

}
