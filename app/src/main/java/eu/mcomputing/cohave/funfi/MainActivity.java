package eu.mcomputing.cohave.funfi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.Calendar;
import java.util.List;

import eu.mcomputing.cohave.authentication.AccountGeneral;
import eu.mcomputing.cohave.authentication.User;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.drawer.DrawerActivity;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.logger.Logger;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.ScoreServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.WifiServerAccessor;


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
public class MainActivity extends DrawerActivity {

    private AccountManager mAccountManager;
    private String authToken = null;
    private Account mConnectedAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAccountManager = AccountManager.get(this);
        getTokenForAccountCreateIfNeeded(AccountGeneral.ACCOUNT_TYPE, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS);
        initAuthDrawer(savedInstanceState);

    }

    private void getTokenForAccountCreateIfNeeded(String accountType, final String authTokenType) {

        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthTokenByFeatures(accountType, authTokenType, null, this, null, null,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Bundle bnd = null;
                        try {
                            bnd = future.getResult();
                            authToken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                            if (authToken != null) {
                                String accountName = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                                mConnectedAccount = new Account(accountName, AccountGeneral.ACCOUNT_TYPE);

                                Logger.startSensing(getApplicationContext());
                                setSyncSettings();
                                requestSync();
                                setAppUIForUser(mAccountManager,mConnectedAccount);


                            } else {
                                Logger.stopSensing(getApplicationContext());
                                getTokenForAccountCreateIfNeeded(AccountGeneral.ACCOUNT_TYPE, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS);
                            }

                            MyLog.log(getClass(), ((authToken != null) ? ("SUCCESS! token: " + authToken) : "FAIL"));
                            MyLog.log(getClass(), "GetTokenForAccount Bundle is " + bnd);

                        } catch (Exception e) {
                            e.printStackTrace();
                            MyLog.log(getClass(), e.getMessage());
                            ACRA.getErrorReporter().handleSilentException(e);
                        }
                    }
                }
                , null);
    }

    public void requestSync() {
        if (mConnectedAccount != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); // Performing a sync no matter if it's off
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); // Performing a sync no matter if it's off
            ContentResolver.requestSync(mConnectedAccount, WifiContentProvider.AUTHORITY, bundle);
            MyLog.log(getClass(), "Requesting Sync .... wifi data");
        }
    }

    public void setSyncSettings() {
        if (mConnectedAccount != null) {
            String authority = WifiContentProvider.AUTHORITY;
            ContentResolver.setIsSyncable(mConnectedAccount, authority, 1);
            ContentResolver.setSyncAutomatically(mConnectedAccount, authority, true);
            ContentResolver.addPeriodicSync(mConnectedAccount, authority, new Bundle(), Config.wifiSyncInterval);
            MyLog.log(getClass(), "on app request sync settings");
        }
    }
}
