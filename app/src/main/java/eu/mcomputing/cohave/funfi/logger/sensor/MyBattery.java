package eu.mcomputing.cohave.funfi.logger.sensor;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

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
public class MyBattery {
    public int status;
    public int chargePlug;
    public double level;
    private Context context;

    public static double getBatteryLevel(Context context){
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawlevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        double scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);

        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

        double level = 0;
        if (rawlevel > 0 && scale > 0) {
            level = rawlevel / scale;
        }

        return level+status*10+chargePlug*100;
    }

    public void setBatteryLevel(Context context){
        this.context = context;
        Intent batteryIntent = this.context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawlevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        double scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);

        status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

        level = 0;
        if (rawlevel > 0 && scale > 0) {
            level = rawlevel / scale;
        }
    }

    public boolean continueSense(){
        if (status == BatteryManager.BATTERY_STATUS_DISCHARGING
                || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING
            )
        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            int minlevel = Integer.valueOf(sharedPref.getString("settings_min_battery_level","0"));
            return minlevel < level*100;
        }

        return true;
    }

}
