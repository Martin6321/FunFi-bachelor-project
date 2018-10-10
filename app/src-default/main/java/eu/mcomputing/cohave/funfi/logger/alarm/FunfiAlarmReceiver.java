package eu.mcomputing.cohave.funfi.logger.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;

import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.logger.sensor.LocationInfo;
import eu.mcomputing.cohave.funfi.logger.service.FunfiService;


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
public class FunfiAlarmReceiver extends WakefulBroadcastReceiver {
    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgr;
    // The pending intent that is triggered when the alarm fires.
    private PendingIntent alarmIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        MyLog.log(getClass(), "Alarm on Receive");

        Intent service;
        service = new Intent(context, FunfiService.class);
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, service);
    }

    /**
     * Set alarm
     * @param context
     */
    public void setAlarm(Context context) {
        MyLog.log(getClass(), "Alarm set interval " + Config.Alarm.ALARM_SENSING_INTERVAL);

        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FunfiAlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, Config.Alarm.ALARM_SENSING_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + Config.Alarm.ALARM_SENSING_INTERVAL, alarmIntent);
    }

    /**
     * Cancels the alarm.
     *
     * @param context
     */
    public void cancelAlarm(Context context) {
        // If the alarm has been set, cancel it.
        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            MyLog.log(getClass(), "Alarm Mng bol null. ");
        }
        if (alarmIntent == null) {
            Intent intent = new Intent(context, FunfiAlarmReceiver.class);
            alarmIntent = PendingIntent.getBroadcast(context, Config.Alarm.ALARM_SENSING_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            MyLog.log(getClass(), "Alarm intent bol null ");
        }

        alarmMgr.cancel(alarmIntent);
        MyLog.log(getClass(), "Vypol sa napriek vsetkemu ");
        LocationInfo.stopLocationUpdates(context);
    }
}
