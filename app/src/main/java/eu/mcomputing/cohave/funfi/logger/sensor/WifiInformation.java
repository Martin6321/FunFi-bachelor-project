package eu.mcomputing.cohave.funfi.logger.sensor;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.List;

import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiLocationModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiNetworkModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiStaticModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;


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
public class WifiInformation {
    //----------
    WifiManager mainWifiObj;
    WifiScanReceiver wifiReciever;
    //-----------
    Context context;
    long location_sensorTime;
    String email;

    public WifiInformation(Context context) {
        this.context = context;
    }

    public boolean scanWifiLocation(long location_sensorTime) {
        this.location_sensorTime = location_sensorTime;
        return scanWifi();
    }

    private boolean scanWifi() {
        MyLog.log(getClass(),"scanning wifi");
        WifiManager wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            MyLog.log(getClass(),"wifi enable starting scan");
            mainWifiObj = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            wifiReciever = new WifiScanReceiver();
            mainWifiObj.startScan();
            context.registerReceiver(wifiReciever, new IntentFilter(
                            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            );
            return true;
        } else {
            MyLog.log(getClass(), "{error: 'Wifi not enabled'}");
            return false;
        }
    }




    // returns list of avalible wifi networks
    List<ScanResult> getAvalibleWifiNetworks()
    {
        MyLog.log(getClass(),"scanning wifi");
        WifiManager wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            MyLog.log(getClass(),"wifi enable starting scan");
            mainWifiObj = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            mainWifiObj.startScan();
            return mainWifiObj.getScanResults();
        } else {
            MyLog.log(getClass(), "{error: 'Wifi not enabled'}");
            return new ArrayList<ScanResult>();
        }
    }


    private void logWifi(List<ScanResult> wifiList) {
        if (location_sensorTime < 1) {
            MyLog.log(getClass(), "WifiInformation: invalid Location sensor time " + location_sensorTime + " !!!");
            return;
        }

        for (ScanResult wifi : wifiList) {

            long lastseen = SystemClock.elapsedRealtime() - wifi.timestamp/1000;
            WifiLocationModel wifiLocationModel = new WifiLocationModel(wifi.BSSID, location_sensorTime, wifi.level, lastseen);

            long local_wifiNetwork_id = -1;

            if (wifiLocationModel.signalLevel <= Config.Wifi.minWifiStrength) {
                continue; //do not log week signal wifi networks
            }

            //if it not such BSSID is in wifi rating table try in network table
            Cursor cNetwork = context.getContentResolver().query(
                    WifiContentProvider.WIFI_NETWORK_CONTENT_URI,
                    new String[]{WifiNetworkModel.Table._ID},WifiNetworkModel.Table.BSSID+"=?",
                    new String[]{wifi.BSSID},null);
            cNetwork.moveToFirst();

            if (!cNetwork.isAfterLast() && cNetwork.getCount()==1) {
                MyLog.log(getClass(),"wifi id found in network table ");
                //if it was one record, get remote id
                WifiNetworkModel wifiNetworkModel = new WifiNetworkModel(cNetwork);
                saveData(wifiLocationModel);
                cNetwork.close();
                continue;
            } else if (cNetwork.getCount()==0){
                //if not even in network table was found BSSID so insert
                MyLog.log(Config.TAG, "wifi id not found saving new wifi network!!");

                WifiNetworkModel wifiNetworkModel = new WifiNetworkModel(0,wifi.BSSID,wifi.SSID,wifi.frequency,wifi.capabilities);
                Uri uri = context.getContentResolver().insert(
                        WifiContentProvider.WIFI_NETWORK_CONTENT_URI,wifiNetworkModel.getContentValues());
                if (uri == null){
                    MyLog.log(getClass(),"saving new network not successful");
                    ACRA.getErrorReporter().handleSilentException(new Exception("Unable to save wifi network "+ wifiNetworkModel.toString()));
                    cNetwork.close();
                    continue;
                }else{
                    MyLog.log(getClass(),"new network saved");
                    //get local id of current inserted row
                    local_wifiNetwork_id = Long.parseLong(uri.getPathSegments().get(WifiContentProvider.DATA_ITEM));
                    saveData(wifiLocationModel);
                    cNetwork.close();
                    continue;
                }
            }else {
                cNetwork.close();
            }

        }
    }

    public void saveData(WifiLocationModel wifiLocationModel){

        MyLog.log(getClass(),"saving wifilocation "+wifiLocationModel.toString());

        Uri uri = context.getContentResolver().insert(WifiContentProvider.WIFI_LOCATION_CONTENT_URI, wifiLocationModel.getContentValues());
        if (uri == null){
            MyLog.log(getClass(),"saving wifilocation not successful");
            ACRA.getErrorReporter().handleSilentException(new Exception("Unable to save wifi location "+wifiLocationModel.toString()));
        }else{
            MyLog.log(getClass(),"saving wifilocation successful");
        }
    }


    private double calculateDistance(double signalLevelInDbm, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDbm)) / 20.0;
        exp = Math.round(Math.pow(10.0, exp));
        return exp;
    }

    class WifiScanReceiver extends BroadcastReceiver {

        @SuppressLint("UseValueOf")
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = mainWifiObj.getScanResults();

            WifiInformation.this.logWifi(wifiScanList);

            try {
                context.unregisterReceiver(wifiReciever);
            } catch (Exception e) {
                //it was exception but it does not matter
            }
        }
    }
}
