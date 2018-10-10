package eu.mcomputing.cohave.funfi.syncadapter.accessor;

import android.accounts.AuthenticatorException;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;

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
import java.util.List;

import eu.mcomputing.cohave.authentication.COhaveEuServer;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.PlaceVoteModel;
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
public class PlaceVoteServerAccessor {
    private final static String PLACE_VOTE_URL = Config.URL.PLACE_VOTE_URL;

    /// select data from local DB into list and remove them
    public boolean uploadPlaceVotes(String token, ContentProviderClient provider) throws Exception
    {
        List<PlaceVoteModel> list = new ArrayList<>();
        int count=0;

        Cursor cursor = provider.query(WifiContentProvider.getLimitUri(WifiContentProvider.PLACE_VOTE_URL, 10, 0),
                new String[]{PlaceVoteModel.Table._ID,"latitude","longitude","poi_id","answer"}, null, null, PlaceVoteModel.Table._ID+" DESC");

        cursor.moveToFirst();
        count = cursor.getCount();
        while (!cursor.isAfterLast()) {
            long id = cursor.getLong(0);
            double latitude = cursor.getDouble(1);
            double longitude = cursor.getDouble(2);
            long poi_id = cursor.getLong(3);
            String answer = cursor.getString(4);

            list.add(new PlaceVoteModel(id, latitude, longitude, poi_id, answer));
            //provider.delete(WifiContentProvider.PLACE_VOTE_CONTENT_URI, PlaceVoteModel.Table._ID + "=?", new String[]{Long.toString(id)});
            cursor.moveToNext();
        }
        cursor.close();

        uploadToServer(token, provider, list);



        return (count < 10) ? true : false;
    }

    // upload selected data to remote DB
    private void uploadToServer(String token, ContentProviderClient provider, List<PlaceVoteModel> records) throws Exception {

        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(PLACE_VOTE_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);

        String jsonData = new Gson().toJson(records);
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

            for (long id : list){
                provider.delete(WifiContentProvider.PLACE_VOTE_CONTENT_URI, PlaceVoteModel.Table._ID + "=?", new String[]{Long.toString(id)});
            }

            return;

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
            return;
            //throw new Exception("Chyba pri spojeni so serverom. Skontrolujte internetove pripojenie. (ERR123)");
        }

    }
}