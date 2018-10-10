package eu.mcomputing.cohave.funfi.syncadapter.accessor;

import android.accounts.AuthenticatorException;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.RemoteException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.mcomputing.cohave.authentication.COhaveEuServer;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiStaticModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.syncadapter.RestRequestParam;
import eu.mcomputing.cohave.helper.crypt.CryptHelper;


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
public class WifiServerAccessor {
    private final static String WIFI_FEED_URL = Config.URL.WIFI_FEED;
    private final static String WIFI_UPLOAD_URL = Config.URL.WIFI_UPLOAD;
    private final static String WIFI_SYNC_URL = Config.URL.WIFI_SYNC_FEED_URL;
    private final static String WIFI_INIT_DOWNLOAD = Config.URL.WIFI_INIT_DOWNLOAD;

    public long uploadChangedWifiRecords(String authToken, ContentProviderClient provider)
            throws Exception {
        long numUpdates = 0;

        MyLog.log(getClass(), "> Get local WifiModel");
        Cursor c = provider.query(
                WifiContentProvider.WIFI_CONTENT_URI, null, WifiModel.Table.SYNC + "=?", new String[]{WifiModel.WIFI_SYNC_REQ + ""},
                WifiModel.Table.FOUND_AT + " DESC");

        MyLog.log(getClass(), "> Num of local wifis to sync " + c.getCount());
        ArrayList<String> wifis = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            WifiModel w = new WifiModel(c);
            wifis.add(w._id + "-" + w.rating + "-" + w.rating_time);
            c.moveToNext();
        }
        c.close();

        if (wifis.isEmpty())
            return numUpdates;

        ArrayList<Long> result = uploadWifiList(authToken, wifis);

        if (result.size() == 0) {
            MyLog.log(getClass(), "> No success inserts to remote database");
        } else {
            MyLog.log(getClass(), "> updating local database with remote changes");

            for (Long id : result) {
                MyLog.log(getClass(), "> Local synced " + id + "");
                provider.update(
                        ContentUris.withAppendedId(WifiContentProvider.WIFI_CONTENT_URI, id),
                        WifiModel.getSyncContentValues(WifiModel.WIFI_SYNC_OK),
                        null,
                        null
                );
                numUpdates++;
            }

            MyLog.log(getClass(), "> finished upload");
        }

        return numUpdates;
    }

    // get during sync
    public long downloadWifiRecords(String authToken, ContentProviderClient provider)
            throws Exception {
        long numInserts = 0;

//        Cursor cCount = provider.query(WifiContentProvider.WIFI_CONTENT_URI, new String[]{"count(*) as pocet"}, null,null, null);
//        cCount.moveToFirst();
//        if (!cCount.isAfterLast())
//            MyLog.log(getClass(), "> Num of local wifis " + cCount.getLong(0));
//        cCount.close();

        MyLog.log(getClass(), "> Get last local WifiModel");
        Cursor c = provider.query(
                WifiContentProvider.getLimitUri(WifiContentProvider.WIFI_URL, 1, 0), new String[]{WifiModel.Table.FOUND_AT}, null, null, WifiModel.Table.FOUND_AT + " DESC"
        );
        c.moveToFirst();
        long last_found_at = c.isAfterLast() ? -1 : c.getLong(0);
        c.close();

        // Get last local wifi
        List<WifiModel> wifiModelList = getWifiList(authToken, last_found_at);
        MyLog.log(getClass(), "> Num of remote wifis " + wifiModelList.size());

        if (wifiModelList.size() == 0) {
            MyLog.log(getClass(), "> No server changes to insert local database");
        } else {
            MyLog.log(getClass(), "> updating local database with remote changes");

            ContentValues[] contentValues = new ContentValues[wifiModelList.size()];
            int i = 0;
            for (WifiModel wifiModel : wifiModelList) {
                MyLog.log(getClass(), "> Remote -> Local " + wifiModel.toString() + "");
                contentValues[i++] = wifiModel.getContentValues();
            }
            provider.bulkInsert(WifiContentProvider.WIFI_CONTENT_URI, contentValues);
            numInserts += contentValues.length;
        }

        return numInserts;
    }

    public long importInitWifiRecords(String authToken, ContentProviderClient provider)
            throws Exception {
        String[] wifiarr = downloadInitWifiRecords(authToken, provider);
        long inserts = insertInitWiFiRecords(wifiarr, provider);
        return inserts;
    }

    public String[] downloadInitWifiRecords(String authToken, ContentProviderClient provider)
            throws Exception {

        Cursor cCount = provider.query(WifiContentProvider.WIFI_CONTENT_URI, new String[]{"count(*) as pocet"}, null, null, null);
        cCount.moveToFirst();
        if (!cCount.isAfterLast())
            MyLog.log(getClass(), "> Num of local wifis " + cCount.getLong(0));
        cCount.close();

        MyLog.log(getClass(), "> Get last local WifiModel");
        Cursor c = provider.query(
                WifiContentProvider.getLimitUri(WifiContentProvider.WIFI_URL, 1, 0), new String[]{WifiModel.Table.FOUND_AT}, null, null, WifiModel.Table.FOUND_AT + " DESC"
        );
        c.moveToFirst();
        long last_found_at = c.isAfterLast() ? -1 : c.getLong(0);
        c.close();

        // Get last local wifi
        String[] wifiArr = getInitWifiList(authToken, last_found_at);
        MyLog.log(getClass(), "> Num of remote wifiArr" + wifiArr.length);

        return wifiArr;
    }

    public long insertInitWiFiRecords(String[] wifiArr, ContentProviderClient provider)
            throws Exception {
        MyLog.log(getClass(), "> Num of remote wifiArr" + wifiArr.length);

        long numInserts = 0;

        if (wifiArr.length == 0) {
            MyLog.log(getClass(), "> No server changes to insert local database");
        } else {
            MyLog.log(getClass(), "> updating local database with remote changes");

            ContentValues[] contentValues = new ContentValues[wifiArr.length / 10];

            int id = 0;
            String[] item = new String[10];
            for (int i = 0; i < wifiArr.length; i++) {
                item[i % 10] = wifiArr[i];
                if (i % 10 == 9) {
                    contentValues[id++] = WifiModel.getContentValuesFromArr(item);
                    item = new String[10];
                }
            }
            provider.bulkInsert(WifiContentProvider.WIFI_CONTENT_URI, contentValues);
            numInserts += contentValues.length;
        }

        return numInserts;
    }

    public long syncWifiRating(String authToken, ContentProviderClient provider, long last_sync_time)
            throws Exception {
        long numUpdates = 0;

        List<Integer> ratingList = getWifiRatingSyncList(authToken, last_sync_time);

        if (ratingList == null)
            return numUpdates;

        MyLog.log(getClass(), "> Get sync response length " + ratingList.size());


        if (ratingList.size() == 0 || ratingList.size() % 4 != 0) {
            MyLog.log(getClass(), "> Server sync response is empty or not multiply of 4");
            return numUpdates;
        }


        MyLog.log(getClass(), "> updating local database with remote changes");
        Iterator<Integer> it = ratingList.iterator();
        while (it.hasNext()) {
            int wid = it.next();
            int[] param = new int[]{it.next(), it.next(), it.next()};

            provider.update(ContentUris.withAppendedId(WifiContentProvider.WIFI_CONTENT_URI, (long) wid),
                    WifiModel.getSyncUpdateValues(param), null, null
            );

            numUpdates++;
        }

        return numUpdates;
    }


    private List<WifiModel> getWifiList(String token, long last_found_at) throws Exception {

        MyLog.log(getClass(), "getWifi after " + last_found_at + " auth[" + token + "]");


        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);
        String data = cryptHelper.encrypt(last_found_at + "");

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(WIFI_FEED_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);
        httpPost.addHeader("X-COhave-Extra", data);

        HttpResponse response = httpClient.execute(httpPost);
        String responseString = EntityUtils.toString(response.getEntity());
        MyLog.log(getClass(), "getWifi> Response (" + response.getStatusLine().getStatusCode() + "= " + responseString);
        String decrypted = cryptHelper.decrypt(responseString);
        MyLog.log(getClass(), "getWifi> Response DEC " + decrypted);

        if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
            COhaveEuServer.COhaveEuError error = new Gson().fromJson(decrypted, COhaveEuServer.COhaveEuError.class);
            if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new AuthenticatorException(error.error);
            } else {
                throw new IOException("Chybna akcia [" + error.code + "] - " + error.error);
            }
        }

        try {

            Gson gson = new Gson();
            Type collectionType = new TypeToken<List<WifiModel>>() {
            }.getType();
            List<WifiModel> list = gson.fromJson(decrypted, collectionType);
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
            throw new com.google.gson.JsonSyntaxException("Zle udaje");
        }

    }

    private String[] getInitWifiList(String token, long last_found_at) throws Exception {

        MyLog.log(getClass(), "getWifi after " + last_found_at + " auth[" + token + "]");

        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);
        String data = cryptHelper.encrypt(last_found_at + "");

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(WIFI_INIT_DOWNLOAD);
        httpPost.addHeader("X-COhave-Data", headerdata);
        httpPost.addHeader("X-COhave-Extra", data);

        HttpResponse response = httpClient.execute(httpPost);
        String responseString = EntityUtils.toString(response.getEntity());
        MyLog.log(getClass(), "getWifi> Response (" + response.getStatusLine().getStatusCode() + "= " + responseString);

        if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
            String decrypted = cryptHelper.decrypt(responseString);
            MyLog.log(getClass(), "getWifi> Response DEC " + decrypted);

            COhaveEuServer.COhaveEuError error = new Gson().fromJson(decrypted, COhaveEuServer.COhaveEuError.class);
            if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new AuthenticatorException(error.error);
            } else {
                throw new IOException("Chybna akcia [" + error.code + "] - " + error.error);
            }
        }

        try {

            if (responseString.length() < 2
                    || responseString.charAt(0) != '['
                    || responseString.charAt(responseString.length() - 1) != ']') {
                throw new Exception("Zle udaje (nespravny pocet atributov)");
            }

            if (responseString.length() < 6)
                return new String[0];

            responseString = responseString.substring(2, responseString.length() - 2);

            String[] wifis = responseString.split("\",\"");
            if (wifis.length % 10 != 0) {
                throw new Exception("Zle udaje (nespravny pocet atributov)");
            }

            return wifis;

        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
            throw new com.google.gson.JsonSyntaxException("Zle udaje");
        }

    }

    private List<Integer> getWifiRatingSyncList(String token, long last_sync_time) throws Exception {

        MyLog.log(getClass(), "getWifiRatingSync  auth[" + token + "]");


        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(WIFI_SYNC_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);
        httpPost.addHeader("X-COhave-Extra", String.valueOf(last_sync_time));

        HttpResponse response = httpClient.execute(httpPost);
        String responseString = EntityUtils.toString(response.getEntity());
        MyLog.log(getClass(), "getWifi> Response (" + response.getStatusLine().getStatusCode() + "= " + responseString);
        String decrypted = cryptHelper.decrypt(responseString);
        MyLog.log(getClass(), "getWifi> Response DEC " + decrypted);

        if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
            COhaveEuServer.COhaveEuError error = new Gson().fromJson(decrypted, COhaveEuServer.COhaveEuError.class);
            if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new AuthenticatorException(error.error);
            } else {
                throw new IOException("Chybna akcia [" + error.code + "] - " + error.error);
            }
        }

        try {

            Gson gson = new Gson();
            Type collectionType = new TypeToken<List<Integer>>() {
            }.getType();
            List<Integer> list = gson.fromJson(decrypted, collectionType);
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
            throw new com.google.gson.JsonSyntaxException("Zle udaje");
        }

    }

    private ArrayList<Long> uploadWifiList(String token, ArrayList<String> wifiList) throws Exception {

        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(WIFI_UPLOAD_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);

        String jsonData = new Gson().toJson(wifiList);
        jsonData = cryptHelper.encrypt(jsonData);
        httpPost.setEntity(new StringEntity(jsonData, "UTF-8"));

        try {
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            MyLog.log(getClass(), "UPL RES: " + responseString);
            String decrypted = cryptHelper.decrypt(responseString);
            MyLog.log(getClass(), "UPL RES DECRYPTED: " + decrypted);

            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                COhaveEuServer.COhaveEuError error = new Gson().fromJson(decrypted, COhaveEuServer.COhaveEuError.class);
                if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw new AuthenticatorException(error.error);
                } else {
                    throw new IOException("Chybna akcia [" + error.code + "] - " + error.error);
                }
            }

            Gson gson = new Gson();
            Type collectionType = new TypeToken<List<Long>>() {
            }.getType();
            ArrayList<Long> list = gson.fromJson(decrypted, collectionType);
            return list;

        } catch (com.google.gson.JsonIOException ex) {
            ex.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(ex);
            throw new Exception("Chybna odpoved zo servera. Skuste neskor. (ERR124)");
        } catch (com.google.gson.JsonSyntaxException ex) {
            ex.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(ex);
            throw new Exception("Chybna odpoved zo servera. Skuste neskor. (ERR125)");
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<Long>();
            //throw new Exception("Chyba pri spojeni so serverom. Skontrolujte internetove pripojenie. (ERR123)");
        }


    }
}
