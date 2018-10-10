package eu.mcomputing.cohave.funfi.syncadapter.accessor;

import android.accounts.AuthenticatorException;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
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
public class WifiStaticServerAccessor {
    private final static String WIFI_STATIC_URL = Config.URL.WIFI_STATIC_URL;


    // gets offset for server downloading with local database row number
    public List<WifiStaticModel> getStaticWifiRecords(String token, ContentProviderClient provider) throws Exception {
        MyLog.log(getClass(), "> Get last local WifiModel");
        Cursor c = provider.query(
                WifiContentProvider.getLimitUri(WifiContentProvider.WIFI_STATIC_URL, 1, 0), new String[]{WifiStaticModel.Table._ID}, null,null, WifiStaticModel.Table._ID+" DESC"
        );
        c.moveToFirst();
        long last_id = c.isAfterLast() ? -1 : c.getLong(0);
        c.close();

        MyLog.log(getClass(), "> Last local static wifi id " + last_id);

        return getStaticWifiFromServer(token, last_id);
    }

    // downloads static wifi records from server
    private List<WifiStaticModel> getStaticWifiFromServer(String token, long last_id) throws Exception
    {


        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(WIFI_STATIC_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);
        httpPost.addHeader("X-COhave-Extra", cryptHelper.encrypt(String.valueOf(last_id)));

        try {
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            MyLog.log(getClass(), "GET STATIC RES: " + responseString);
            String decrypted = cryptHelper.decrypt(responseString);
            MyLog.log(getClass(), "GET STATIC RES DECRYPTED: " + decrypted);

            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                COhaveEuServer.COhaveEuError error = new Gson().fromJson(decrypted, COhaveEuServer.COhaveEuError.class);
                if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw new AuthenticatorException(error.error);
                } else {
                    throw new IOException("Chybna akcia [" + error.code + "] - " + error.error);
                }
            }

            Gson gson = new Gson();
            Type collectionType = new TypeToken<List<WifiStaticModel>>() {
            }.getType();
            List<WifiStaticModel> list = gson.fromJson(decrypted, collectionType);
            return  list;

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
            return new ArrayList<WifiStaticModel>();
            //throw new Exception("Chyba pri spojeni so serverom. Skontrolujte internetove pripojenie. (ERR123)");
        }

    }

    // inserts downloaded static networks into local database
    public long insertStaticWifiRecords(List<WifiStaticModel> list, ContentProviderClient provider) throws Exception
    {

        MyLog.log(getClass(), "> Num of static wifis to insert" + list.size());

        long numInserts = 0;

        if (list.size() == 0) {
            MyLog.log(getClass(), "> No static wifis to insert local database");
        } else {
            MyLog.log(getClass(), "> updating local database with static wifis");

            ContentValues[] contentValues = new ContentValues[list.size()];

            for (int i=0; i < list.size();i++) {
                contentValues[i] = list.get(i).getContentValues();
            }
            provider.bulkInsert(WifiContentProvider.WIFI_STATIC_CONTENT_URI,contentValues);
            numInserts+=contentValues.length;
        }

        return numInserts;
    }
}