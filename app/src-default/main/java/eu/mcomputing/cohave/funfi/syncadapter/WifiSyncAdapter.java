package eu.mcomputing.cohave.funfi.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;

import org.acra.ACRA;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import eu.mcomputing.cohave.authentication.AccountGeneral;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiStaticModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.logger.Logger;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.LocationServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.PlaceVoteServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.ScoreServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.WifiLocationServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.WifiNetworkServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.WifiServerAccessor;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.WifiStaticServerAccessor;

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
public class WifiSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "WifiSyncAdapter";

    private final AccountManager mAccountManager;

    /**
     * Android 3.0 and above
     *
     * @param context
     * @param autoInitialize
     * @param allowParallelSyncs
     */
    public WifiSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mAccountManager = AccountManager.get(context);
    }

    /**
     * Old constructor used in Android before 3.0
     *
     * @param context
     * @param autoInitialize
     */
    public WifiSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mAccountManager = AccountManager.get(context);
    }

    /**
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {


        MyLog.log(getClass(), "> onPerformSync for account[" + account.name + "]");

        String authToken = null;
        try {
            // Get the auth token for the current account
            authToken = mAccountManager.blockingGetAuthToken(account,
                    AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, true);

            if (authToken == null) {
                mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, authToken);
                Logger.stopSensing(getContext());
                throw new AuthenticatorException("Chyba pri ziskani tokenu");
            }else{
                Logger.startSensing(getContext());
            }

            //User user = new User(mAccountManager,account);
            SharedPreferences sharedPref = getContext().getSharedPreferences(Config.SHP.key, Context.MODE_PRIVATE);

            ScoreServerAccessor scoreServerAccessor = new ScoreServerAccessor();
            scoreServerAccessor.updateScore(authToken,getContext());

            LocationServerAccessor locationServerAccessor = new LocationServerAccessor();
            syncResult.stats.numDeletes+=locationServerAccessor.uploadWifiLocationRecords(authToken, provider);//upload locations and set remote locationids to wifilocations

            WifiServerAccessor wifiServerAccessor = new WifiServerAccessor();
            syncResult.stats.numUpdates+=wifiServerAccessor.uploadChangedWifiRecords(authToken, provider);//upload my rating changes
            syncResult.stats.numInserts+=wifiServerAccessor.downloadWifiRecords(authToken, provider);//download next wifinetworks from server
            syncResult.stats.numUpdates+=wifiServerAccessor.syncWifiRating(authToken, provider, sharedPref.getLong(Config.SHP.last_sync,0));//sync rating

            WifiNetworkServerAccessor wifiNetworkServerAccessor = new WifiNetworkServerAccessor();
            syncResult.stats.numUpdates+=wifiNetworkServerAccessor.uploadWifiNetworksRecords(authToken, provider);

            WifiLocationServerAccessor wifiLocationServerAccessor = new WifiLocationServerAccessor();
            syncResult.stats.numDeletes+=wifiLocationServerAccessor.uploadWifiLocationRecords(authToken,provider);


            // sync static wifis
            WifiStaticServerAccessor wifiStaticServerAccessor = new WifiStaticServerAccessor();

            List<WifiStaticModel> records = wifiStaticServerAccessor.getStaticWifiRecords(authToken,provider);
            if (records.size() > 0)
                wifiStaticServerAccessor.insertStaticWifiRecords(records, provider);

            // sync place votes
            PlaceVoteServerAccessor voteAccessor = new PlaceVoteServerAccessor();
            voteAccessor.uploadPlaceVotes(authToken,provider);



            Calendar c = Calendar.getInstance();

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(Config.SHP.last_sync, c.getTimeInMillis());
            editor.commit();

            MyLog.log(getClass(), TAG + "> Finished.");

        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (IOException e) {
            MyLog.log(getClass(), TAG + "> IOexception.");
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            MyLog.log(getClass(), TAG + "> Authexception.");
            syncResult.stats.numAuthExceptions++;
            //remove the token from the cache, otherwise requests will continue failing!
            MyLog.log(getClass(), "token " + authToken);
            mAccountManager.invalidateAuthToken(AccountGeneral.ACCOUNT_TYPE, authToken);
            e.printStackTrace();
        } catch (com.google.gson.JsonIOException ex) {
            MyLog.log(getClass(), TAG + "> ParseException1.");
            ex.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(ex);
            syncResult.stats.numParseExceptions++;
        } catch (com.google.gson.JsonSyntaxException ex) {
            MyLog.log(getClass(), TAG + "> ParseException2.");
            ex.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(ex);
            syncResult.stats.numParseExceptions++;
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
    }
}

