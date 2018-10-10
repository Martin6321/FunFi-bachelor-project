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
import eu.mcomputing.cohave.funfi.contentprovider.dao.LocationModel;
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
public class LocationServerAccessor {
    private final static String REST_API_KEY = Config.apikey;
    private final static String LOCATION_UPLOAD_URL = Config.URL.LOCATION_UPLOAD;

    private class LocationResponse {
        public long id;
        public long remote_id;
    }

    public long uploadWifiLocationRecords(String authToken, ContentProviderClient provider)
            throws Exception {

        long numDeletes = 0;

        MyLog.log(getClass(), "> Get local locations");
        Cursor cLocation = provider.query(
                WifiContentProvider.getLimitUri(WifiContentProvider.LOCATION_URL, 1000, 0),
                null, null, null,
                LocationModel.Table.sensorTime + " DESC");

        cLocation.moveToFirst();
        MyLog.log(getClass(), "> Num of local locations to sync " + cLocation.getCount());

        ArrayList<LocationModel> locationModels = new ArrayList<>();
        while (!cLocation.isAfterLast()) {
            LocationModel l = new LocationModel(cLocation);
            locationModels.add(l);
            cLocation.moveToNext();
        }
        cLocation.close();

        if (locationModels.isEmpty())
            return numDeletes;

        ArrayList<LocationResponse> result = uploadLocationList(authToken, locationModels);

        if (result.size() == 0) {
            MyLog.log(getClass(), "> No success inserts to remote database");
        } else {
            MyLog.log(getClass(), "> Updating local database with remote changes");

            try {
                for (LocationResponse response : result) {
                    MyLog.log(getClass(), "> Local synced " + response.id + "[" + response.remote_id + "]");
                    provider.delete(
                            ContentUris.withAppendedId(WifiContentProvider.LOCATION_CONTENT_URI, response.id),
                            null,
                            null
                    );
                    numDeletes++;
                }
            } finally {

            }

            MyLog.log(getClass(), "> finished upload");
        }

        return numDeletes;
    }

    private ArrayList<LocationResponse> uploadLocationList(String token, ArrayList<LocationModel> locationList) throws Exception {

        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(LOCATION_UPLOAD_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);

        String jsonData = new Gson().toJson(locationList);
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
            Type collectionType = new TypeToken<List<LocationResponse>>() {
            }.getType();
            ArrayList<LocationResponse> list = gson.fromJson(decrypted, collectionType);
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
