package eu.mcomputing.cohave.funfi.logger.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;

import java.util.Calendar;

import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.logger.alarm.FunfiAlarmReceiver;
import eu.mcomputing.cohave.funfi.logger.sensor.LocationInfo;
import eu.mcomputing.cohave.funfi.logger.sensor.MyBattery;
import eu.mcomputing.cohave.helper.config.AuthConfig;


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
public class FunfiService extends Service {

    //------------------ GENERAL ----------------//

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        MyLog.log(getClass(), "FunfiService: " + "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyLog.log(getClass(), "FunfiService onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyLog.log(getClass(), "FunfiService: " + "onStartCommand");
        if (intent != null) {
            startAlarm();
            doWork(intent, 0);
            FunfiAlarmReceiver.completeWakefulIntent(intent);
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Set alarm for permanent sensing intervals
     */
    protected void startAlarm() {
        FunfiAlarmReceiver alarm = new FunfiAlarmReceiver();
        alarm.setAlarm(this);
        MyLog.log(getClass(), "Funfi Alarm zapnuty interval " + Config.Alarm.ALARM_SENSING_INTERVAL);
    }

    //---------------------------------------------//


    protected void doWork(Intent intent, int id) {
        MyBattery battery = new MyBattery();
        battery.setBatteryLevel(getApplicationContext());
        //if battery level is lower than preference do not sense
        if (battery.continueSense() == false) {
            MyLog.log(getClass(),"stopping sensing battery low");
            LocationInfo.stopLocationUpdates(getApplicationContext());
            return;
        }

        MyLog.log(getClass(), "sensing battery  is not low");
        LocationInfo locationInfo = new LocationInfo(getApplicationContext());
        locationInfo.senseLocation();

        SharedPreferences settings = getSharedPreferences(Config.SHP.key, Context.MODE_PRIVATE);
        long synctime = settings.getLong(Config.SHP.sensing_status_time, 0);
        Calendar c = Calendar.getInstance();
        if ( c.getTimeInMillis() > synctime + Config.wifiSyncInterval * 1000){
            MyLog.log(getClass(),"forcing to run synchronization");
            requestSync();
        }

    }


    private Account connectAccount() {
        AccountManager accountManager = AccountManager.get(getApplication());
        Account[] accounts = accountManager.getAccountsByType(AuthConfig.Auth.ACCOUNT_TYPE);
        if (accounts != null && accounts.length == 1) {
            return accounts[0];
        } else {
            return null;
        }
    }

    /**
     * Request network sync
     */
    public void requestSync() {
        Account connectedAccount = connectAccount();

        if (connectedAccount != null) {
            MyLog.log(getClass(), "Vynutena Synchronizácia ...");

            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); // Performing a sync no matter if it's off
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); // Performing a sync no matter if it's off
            ContentResolver.requestSync(connectedAccount, WifiContentProvider.AUTHORITY, bundle);
            MyLog.log(getClass(), "Requesting Sync .... wifi data");
        }

    }
}
