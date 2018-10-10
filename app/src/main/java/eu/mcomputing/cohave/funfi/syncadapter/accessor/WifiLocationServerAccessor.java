package eu.mcomputing.cohave.funfi.syncadapter.accessor;

import android.accounts.AuthenticatorException;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import eu.mcomputing.cohave.authentication.COhaveEuServer;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiLocationModel;
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
public class WifiLocationServerAccessor {
    private final static String REST_API_KEY = Config.apikey;
    private final static String WIFI_LOCATION_UPLOAD_URL = Config.URL.NETWORK_LOCATION_UPLOAD;

    public long uploadWifiLocationRecords(String authToken, ContentProviderClient provider)
            throws Exception {

        long numDeletes = 0;

        MyLog.log(getClass(), "> Get local wifi location");
        Cursor c = provider.query(
                WifiContentProvider.getLimitUri(WifiContentProvider.WIFI_LOCATION_URL, 1000, 0),
                null,
                WifiLocationModel.Table.BSSID + "!='' and " + WifiLocationModel.Table.location_sensorTime + ">0",
                null,
                WifiLocationModel.Table.sensor_time + " DESC");

        c.moveToFirst();
        MyLog.log(getClass(), "> Num of local wifilocations to sync " + c.getCount());

        ArrayList<WifiLocationModel> wifiLocations = new ArrayList<>();
        while (!c.isAfterLast()) {
            WifiLocationModel wl = new WifiLocationModel(c);
            wifiLocations.add(wl);
            c.moveToNext();
        }
        c.close();

        if (wifiLocations.isEmpty())
            return numDeletes;

        ArrayList<Long> result = uploadWifiLocationList(authToken, wifiLocations);

        if (result.size() == 0) {
            MyLog.log(getClass(), "> No success inserts to remote database");
        } else {
            MyLog.log(getClass(), "> Updating local database with remote changes");

            for (Long response : result) {
                MyLog.log(getClass(), "> Local synced " + response);
                provider.delete(
                        ContentUris.withAppendedId(WifiContentProvider.WIFI_LOCATION_CONTENT_URI, response),
                        null,
                        null
                );
                numDeletes++;
            }

            MyLog.log(getClass(), "> finished upload");
        }

        return numDeletes;
    }


    private ArrayList<Long> uploadWifiLocationList(String token, ArrayList<WifiLocationModel> wifiLocationModels) throws Exception {

        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(WIFI_LOCATION_UPLOAD_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);

        String jsonData = new Gson().toJson(wifiLocationModels);
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
            return new ArrayList<>();
            //throw new Exception("Chyba pri spojeni so serverom. Skontrolujte internetove pripojenie. (ERR123)");
        }


    }
}
