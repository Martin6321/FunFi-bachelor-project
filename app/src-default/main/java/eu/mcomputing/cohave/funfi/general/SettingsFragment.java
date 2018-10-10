package eu.mcomputing.cohave.funfi.general;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import eu.mcomputing.cohave.authentication.AccountGeneral;
import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.syncadapter.WifiSyncAdapter;

public class SettingsFragment extends PreferenceFragment {
    private boolean gps, agps, wifi_status;
    private int sync = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkSettings();
        showDialogs();
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);
    }

    private void checkSettings() {
        // Get Location Manager and check for GPS & Network location services
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        agps = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        WifiManager wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        wifi_status = wifi.isWifiEnabled();

        Account[] accounts = AccountManager.get(getActivity()).getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
        if (accounts.length == 1 && accounts[0].name.compareTo(accounts[0].name) == 0) {
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

            Account account = accounts[0];
            sync = ContentResolver.getIsSyncable(account, WifiContentProvider.AUTHORITY);
        }


        SharedPreferences.Editor editor =  getPreferenceManager().getSharedPreferences().edit();
        editor.putBoolean("settings_gps_on", gps);
        editor.putBoolean("settings_agps_on",agps);
        editor.putBoolean("settings_wifi_on", wifi_status);
        editor.putBoolean("settings_synchronization_on", sync > 0 );
        editor.commit();

    }

    private void showDialogs(){
        checkSettings();

        if(!gps || !agps) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Poloha nie je povolená !");
            builder.setMessage("Prosím povoľte polohu a GPS v nastaveniach.");
            builder.setPositiveButton("Povoliť", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Neskôr", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getActivity(),"Kým nepovolíte polohu nemôžete zbierať body !",Toast.LENGTH_LONG).show();
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }

        if(!wifi_status) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("WiFi nie je zapnuté !");
            builder.setMessage("Prosím zapnite WiFi v nastaveniach.");
            builder.setPositiveButton("Povoliť", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Neskôr", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getActivity(),"Kým nepovolíte WiFi nemôžete zbierať body !",Toast.LENGTH_LONG).show();
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }

        if(sync <= 0) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Synchronizácia nie je povolená !");
            builder.setMessage("Prosím povolte synchronizáciu v nastaveniach.");
            builder.setPositiveButton("Povoliť", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Neskôr", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getActivity(),"Kým nepovolíte synchronizáciu nemôžete odovzdávať body !",Toast.LENGTH_LONG).show();
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }



}
