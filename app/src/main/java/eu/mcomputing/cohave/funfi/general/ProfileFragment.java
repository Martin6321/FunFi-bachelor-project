package eu.mcomputing.cohave.funfi.general;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import eu.mcomputing.cohave.CohaveApplication;
import eu.mcomputing.cohave.authentication.AccountGeneral;
import eu.mcomputing.cohave.authentication.User;
import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiNetworkModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.ScoreServerAccessor;

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
public class ProfileFragment extends Fragment {
    private ProgressDialog dialog;
    private View screenView;

    public ProfileFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        screenView = inflater.inflate(R.layout.frament_profile, container, false);

        dialog = new ProgressDialog(getActivity());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Načítavam. Chvílu strpenia...");
        dialog.setCancelable(true);

        new LoadProfileData().execute();

        return screenView;
    }

    private void setProfileScreen(long[] params){
        View rootView = getView()==null ? this.screenView : getView();


        Account[] accounts = AccountManager.get(getActivity()).getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
        if (accounts.length > 0 && accounts[0].name.compareTo(accounts[0].name) != 0) {
            Toast.makeText(getActivity().getApplicationContext(),"Na jednom zariadení môže byť len jeden účet. Odstránte aktuálny pre pridanie nového.", Toast.LENGTH_LONG).show();
            return;
        }
        if ( accounts.length < 1 ){
            Toast.makeText(getActivity().getApplicationContext(),"Na zariadení musi byť aspon jeden účet.", Toast.LENGTH_LONG).show();
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        Account account = accounts[0];
        AccountManager accountManager = AccountManager.get(getActivity());

        TextView surname = (TextView) rootView.findViewById(R.id.surname_value);
        surname.setText(accountManager.getUserData(account, User.first_name_field));

        TextView lastname = (TextView) rootView.findViewById(R.id.lastname_value);
        lastname.setText(accountManager.getUserData(account, User.last_name_field));

        TextView username = (TextView) rootView.findViewById(R.id.username_value);
        username.setText(accountManager.getUserData(account, User.username_field));

        TextView email = (TextView) rootView.findViewById(R.id.email_value);
        email.setText(account.name);

        //status

        SharedPreferences settings = getActivity().getSharedPreferences(Config.SHP.key, Context.MODE_PRIVATE);

        TextView status = (TextView) rootView.findViewById(R.id.sensing_status_value);
        status.setText(settings.getString(Config.SHP.sensing_status,"--"));

        TextView status_time = (TextView) rootView.findViewById(R.id.time_value);
        long times = settings.getLong(Config.SHP.sensing_status_time, 0);
        Calendar statustime = Calendar.getInstance();
        statustime.setTimeInMillis(times);

        status_time.setText(times == 0 ? "--" : df.format(statustime.getTime()));

        //pending


        TextView pending_location_value = (TextView) rootView.findViewById(R.id.pending_location_value);
        pending_location_value.setText(params[0]+"");

        TextView pending_wifilocation_value = (TextView) rootView.findViewById(R.id.pending_wifilocation_value);
        pending_wifilocation_value.setText(params[1]+"");

        TextView pending_wifi_value = (TextView) rootView.findViewById(R.id.pending_wifi_value);
        pending_wifi_value.setText(params[2] + "");

        TextView last_sync_value = (TextView) rootView.findViewById(R.id.last_sync_value);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(params[3]);

        last_sync_value.setText(df.format(calendar.getTime()));

        //score

        TextView score_value = (TextView) rootView.findViewById(R.id.score_value);
        score_value.setText(String.valueOf(params[4]+params[5]+params[6]+params[7]+params[8]));

        TextView location_score_value = (TextView) rootView.findViewById(R.id.location_score_value);
        location_score_value.setText(String.valueOf(params[4]));

        TextView wifilocation_score_value = (TextView) rootView.findViewById(R.id.wifilocation_score_value);
        wifilocation_score_value.setText(String.valueOf(params[5]));

        TextView wifi_score_value = (TextView) rootView.findViewById(R.id.wifi_score_value);
        wifi_score_value.setText(String.valueOf(params[6]));

        TextView rating_score_value = (TextView) rootView.findViewById(R.id.rating_score_value);
        rating_score_value.setText(String.valueOf(params[7]));

        TextView other_rating_score_value = (TextView) rootView.findViewById(R.id.other_rating_score_value);
        other_rating_score_value.setText(String.valueOf(params[8]));


    }

    public long[] getScoreValues(){
        long[] nums = new long[]{0,0,0,0,0};

        SharedPreferences settings = getActivity().getSharedPreferences(Config.SHP.key, Context.MODE_PRIVATE);
        nums[0]= settings.getLong(Config.SHP.score_location, 0);
        nums[1]= settings.getLong(Config.SHP.score_wifi_location, 0);
        nums[2]= settings.getLong(Config.SHP.score_wifi_network, 0);
        nums[3]= settings.getLong(Config.SHP.score_my_rating, 0);
        nums[4]= settings.getLong(Config.SHP.score_other_rating, 0);

        return nums;
    }

    public long[] getPendingValues(){
        long[] nums = new long[]{0,0,0,0};

        Cursor location = getActivity().getContentResolver().query(
                WifiContentProvider.LOCATION_CONTENT_URI,
                new String[]{"count(*)"},
                null,null,null
        );
        if (location!=null){
            location.moveToFirst();
            nums[0] = location.isAfterLast() ? 0 : location.getLong(0);
            location.close();
        }

        Cursor wifilocation = getActivity().getContentResolver().query(
                WifiContentProvider.WIFI_LOCATION_CONTENT_URI,
                new String[]{"count(*)"},
                null,null,null
        );
        if (wifilocation!=null){
            wifilocation.moveToFirst();
            nums[1] = wifilocation.isAfterLast() ? 0 : wifilocation.getLong(0);
            wifilocation.close();
        }

        Cursor networks = getActivity().getContentResolver().query(
                WifiContentProvider.WIFI_NETWORK_CONTENT_URI,
                new String[]{"count(*)"},
                WifiNetworkModel.Table.REMOTE_ID+"<1",
                null,null
        );
        if (networks!=null){
            networks.moveToFirst();
            nums[2] = networks.isAfterLast() ? 0 : networks.getLong(0);
            networks.close();
        }

        SharedPreferences settings = getActivity().getSharedPreferences(Config.SHP.key, Context.MODE_PRIVATE);
        nums[3]= settings.getLong(Config.SHP.last_sync, 0);

        return nums;
    }


    private class LoadProfileData extends AsyncTask<Void, Void, long[]> {

        public LoadProfileData(){
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // actually could set running = false; right here, but I'll
                    // stick to contract.
                    cancel(true);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        protected long[] doInBackground(Void ... param) {
            long[] result = new long[]{0,0,0,0,0,0,0,0,0};
            long[] nums = getPendingValues();
            long[] score = getScoreValues();

            if (nums != null && nums.length==4){
                result[0]=nums[0];
                result[1]=nums[1];
                result[2]=nums[2];
                result[3]=nums[3];
            }

            if (score != null && score.length==5){
                result[4]=score[0];
                result[5]=score[1];
                result[6]=score[2];
                result[7]=score[3];
                result[8]=score[4];
            }

            return result;
        }

        protected void onProgressUpdate(Void... progress) {

        }

        @Override
        protected void onCancelled(long[] response) {
            super.onCancelled(response);
            if (dialog!=null)
                dialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (dialog != null)
                dialog.dismiss();
        }

        protected void onPostExecute(long[] response) {
            if (dialog==null)
                return;

            if (response != null && response.length==9) {
                setProfileScreen(response);
            }
            else {
                if (getActivity()!=null)
                    Toast.makeText(getActivity(), "Chyba pri načítavaní profilu. Skúste neskôr. ", Toast.LENGTH_LONG).show();
            }

            dialog.dismiss();
        }
    }



}