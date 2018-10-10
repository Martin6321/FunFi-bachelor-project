package eu.mcomputing.cohave.funfi.syncadapter.accessor;

import android.accounts.AuthenticatorException;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.acra.ACRA;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import eu.mcomputing.cohave.authentication.COhaveEuServer;
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
public class ScoreServerAccessor {
    private final static String REST_API_KEY = Config.apikey;
    private final static String SCORE_GET_URL = Config.URL.SCORE_GET_URL;
    private final static String LEADER_BOARD_GET_URL = Config.URL.LEADERBOARD_GET_URL;

    private class ScoreResponse {
        public long location=0;
        public long wifi_location=0;
        public long wifi_network=0;
        public long my_rating=0;
        public long other_rating=0;
    }

    public class LeaderBoardResponse{
        public long device_id=0;
        public String username="";
        public long score=0;
    }

    public void updateScore(String authToken, Context context)
            throws Exception {

        MyLog.log(getClass(), "> updating score starting");

        ScoreResponse scoreResponse = donwloadScore(authToken);

        SharedPreferences sharedPref = context.getSharedPreferences(Config.SHP.key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(Config.SHP.score_location,scoreResponse.location);
        editor.putLong(Config.SHP.score_wifi_location,scoreResponse.wifi_location);
        editor.putLong(Config.SHP.score_wifi_network,scoreResponse.wifi_network);
        editor.putLong(Config.SHP.score_my_rating,scoreResponse.my_rating);
        editor.putLong(Config.SHP.score_other_rating,scoreResponse.other_rating);
        editor.commit();

        MyLog.log(getClass(), "> updating score finished");

    }

    public ArrayList<LeaderBoardResponse> getLeaderBoard(String authToken, Context context) throws Exception{
        MyLog.log(getClass(), "> get leaderboard starting");

        ArrayList<LeaderBoardResponse> leaderboard = donwloadLeaderBoard(authToken);

        MyLog.log(getClass(), "> get leaderboard finished");

        return leaderboard;
    }

    private ScoreResponse donwloadScore(String token) throws Exception {

        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(SCORE_GET_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);

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
            ScoreResponse score = gson.fromJson(decrypted, ScoreResponse.class);
            return score;

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
            return new ScoreResponse();
            //throw new Exception("Chyba pri spojeni so serverom. Skontrolujte internetove pripojenie. (ERR123)");
        }


    }


    private ArrayList<LeaderBoardResponse> donwloadLeaderBoard(String token) throws Exception {

        String headerdata = new Gson().toJson(new RestRequestParam(token), RestRequestParam.class);
        CryptHelper cryptHelper = new CryptHelper();
        headerdata = cryptHelper.encrypt(headerdata);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(LEADER_BOARD_GET_URL);
        httpPost.addHeader("X-COhave-Data", headerdata);

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
            Type collectionType = new TypeToken<List<LeaderBoardResponse>>() {
            }.getType();
            ArrayList<LeaderBoardResponse> list = gson.fromJson(decrypted, collectionType);
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
