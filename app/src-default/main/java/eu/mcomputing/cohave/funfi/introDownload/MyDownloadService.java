package eu.mcomputing.cohave.funfi.introDownload;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import org.apache.http.conn.HttpHostConnectException;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import eu.mcomputing.cohave.authentication.AccountGeneral;
import eu.mcomputing.cohave.funfi.MainActivity;
import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiStaticModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.logger.alarm.FunfiAlarmReceiver;
import eu.mcomputing.cohave.funfi.logger.sensor.LocationInfo;
import eu.mcomputing.cohave.funfi.logger.sensor.MyBattery;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.WifiServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.WifiStaticServerAccessor;
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
public class MyDownloadService extends Service {

    //------------------ GENERAL ----------------//

    MyObserver myObserver;
    public final int initDownloadNotificationId = 1;
    public int numberToInsert=0;
    private double oneItem = 0.0;
    private int numOfInserted = 0;

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
        MyLog.log(getClass(), "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyLog.log(getClass(), "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyLog.log(getClass(), ": " + "onStartCommand");
        if (intent != null) {
            doWork();
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    protected void doWork() {
        myObserver = new MyObserver(new Handler());
        new DownloadFilesTask().execute();
        getContentResolver().
                registerContentObserver(
                        WifiContentProvider.BATCH_INSERT_URI,
                        true,
                        myObserver);
    }

    private class DownloadFilesTask extends AsyncTask<Void, Void, Exception> {
        @Override
        protected void onPreExecute() {
            sendStartNotification();
        }

        protected Exception doInBackground(Void... urls) {
            try {
                Account account = connectAccount();
                if (account==null){
                    MyLog.log(getClass(),"account not available");
                    return new Exception("COhave účet nenájdený");
                }

                AccountManager am  = AccountManager.get(MyDownloadService.this);
                String token =  am.peekAuthToken(account, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS);

                if (TextUtils.isEmpty(token)) {
                    MyLog.log(getClass(), "token not available");
                    return new Exception("COhave prihlásenie expirovalo");
                }
                WifiServerAccessor wifiServerAccessor = new WifiServerAccessor();

                //// Wifi networks downloading
               String[] wifiArr = wifiServerAccessor.downloadInitWifiRecords(token, getContentResolver().acquireContentProviderClient(WifiContentProvider.WIFI_CONTENT_URI));
               numberToInsert= wifiArr.length;
               while (numberToInsert>0) {
                   sendInsertNotification(0, true);
                   wifiServerAccessor.insertInitWiFiRecords(wifiArr, getContentResolver().acquireContentProviderClient(WifiContentProvider.WIFI_CONTENT_URI));

                   numberToInsert=0;
                   numOfInserted=0;
                   wifiArr = wifiServerAccessor.downloadInitWifiRecords(token, getContentResolver().acquireContentProviderClient(WifiContentProvider.WIFI_CONTENT_URI));
                   numberToInsert= wifiArr.length;
               }

                //// Initial static wifi downloading
                WifiStaticServerAccessor wifiStaticServerAccessor = new WifiStaticServerAccessor();

                List<WifiStaticModel> records = wifiStaticServerAccessor.getStaticWifiRecords(token,getContentResolver().acquireContentProviderClient(WifiContentProvider.WIFI_STATIC_CONTENT_URI));
                numberToInsert= records.size();
                while (numberToInsert>0)
                {
                    wifiStaticServerAccessor.insertStaticWifiRecords(records, getContentResolver().acquireContentProviderClient(WifiContentProvider.WIFI_STATIC_CONTENT_URI));
                    numberToInsert=0;
                    numOfInserted=0;
                    records = wifiStaticServerAccessor.getStaticWifiRecords(token, getContentResolver().acquireContentProviderClient(WifiContentProvider.WIFI_STATIC_CONTENT_URI));
                    numberToInsert = records.size();
                    MyLog.log(getClass(),records.toString());
                };

            }catch (Exception e){
                e.printStackTrace();
                return e;
            }
            return null;
        }

        protected void onPostExecute(Exception result) {
            if (result==null) {
                sendCompleteNotification();
            }else{
                if (result instanceof HttpHostConnectException){
                    sendExceptionNotification("Skontrolujte si internet...");
                }else {
                    sendExceptionNotification(result.getMessage());
                }
            }
            getContentResolver().
                    unregisterContentObserver(myObserver);
        }
    }

    class MyObserver extends ContentObserver {
        int progress=0;

        public MyObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            numOfInserted++;
            MyLog.log(getClass(), "change uri ["+numOfInserted+"] " + uri.toString());
            if (numOfInserted%1000==0) {
                sendInsertNotification(numOfInserted, numOfInserted==numberToInsert ? true : false);
                MyLog.log(getClass(),"progresss "+progress);
            }
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


    private void sendStartNotification() {
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.init_download_title))
                        .setTicker(getString(R.string.init_download_ticker))
                        .setContentText(getString(R.string.init_download_download))
                        .setPriority(2)
                        .setAutoCancel(true)
                        .setProgress(0, 0, true);
            showNotification(mBuilder);
    }

    private void sendInsertNotification(int complete, boolean indeterminate) {
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.init_inserting_title))
                        .setTicker(getString(R.string.init_inserting_ticker))
                        .setContentText(getString(R.string.init_inserting_download))
                        .setPriority(2)
                        .setAutoCancel(true)
                        .setProgress(numberToInsert, complete, indeterminate);
        showNotification(mBuilder);
    }

    private void sendCompleteNotification() {
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.init_finish_title))
                        .setTicker(getString(R.string.init_finish_ticker))
                        .setContentText(getString(R.string.init_finish_download))
                        .setPriority(2)
                        .setAutoCancel(true);
        showNotification(mBuilder);
    }

    private void sendExceptionNotification(String text) {
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.init_exception_title))
                        .setTicker(getString(R.string.init_exception_ticker))
                        .setContentText(text)
                        .setPriority(2)
                        .setAutoCancel(true);
        showNotification(mBuilder);
    }


    private void showNotification(NotificationCompat.Builder mBuilder){
        Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(initDownloadNotificationId, mBuilder.build());
    }

}
