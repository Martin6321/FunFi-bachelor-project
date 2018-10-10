package eu.mcomputing.cohave.funfi.general;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.acra.ACRA;

import java.text.SimpleDateFormat;
import java.util.List;

import eu.mcomputing.cohave.authentication.AccountGeneral;
import eu.mcomputing.cohave.funfi.MainActivity;
import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.general.adapter.LeaderBoardAdapter;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.rating.adapter.WifiRatingAdapter;
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
public class LeaderBoardFragment extends Fragment {
    private View snackBarView;
    private Snackbar snackbar;

    private final LinearLayoutManager mLayoutManager;
    private RecyclerView recyclerView;

    private Account mAccount;
    private AccountManager mAccountManager;

    private LeaderBoardAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressDialog dialog;

    public LeaderBoardFragment() {
        // Empty constructor required for fragment subclasses
        mLayoutManager = new LinearLayoutManager(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        Account[] accounts = AccountManager.get(getActivity()).getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
        if (accounts.length > 0 && accounts[0].name.compareTo(accounts[0].name) != 0) {
            Toast.makeText(getActivity().getApplicationContext(), "Na jednom zariadení môže byť len jeden účet. Odstránte aktuálny pre pridanie nového.", Toast.LENGTH_LONG).show();
            return rootView;
        }

        if ( accounts.length < 1 ){
            Toast.makeText(getActivity().getApplicationContext(),"Na zariadení musi byť aspon jeden účet.", Toast.LENGTH_LONG).show();
            return rootView;
        }
        mAccount = accounts[0];
        mAccountManager = AccountManager.get(getActivity());

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fragmentViewCreated(view, savedInstanceState);
    }

    public void fragmentViewCreated(View view, @Nullable Bundle savedInstanceState) {

        //add recycler view
        recyclerView = (RecyclerView) view.findViewById(R.id.leaderboard_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new LeaderBoardAdapter();

        //add adapter with default null cursor
        recyclerView.setAdapter(adapter);

        dialog = new ProgressDialog(getActivity());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Aktualizujem. Chvílu strpenia...");
        dialog.setCancelable(true);

        //add swipe to refresh event
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.leaderboard_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (loadItems() == false)
                    mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        loadItems();

        snackBarView = view.findViewById(R.id.infosnackbar);
        this.snackbar = Snackbar.make(snackBarView, "Potiahnutím dole obnovenie", Snackbar.LENGTH_LONG);
        this.snackbar.show();
    }

    public void addItems(List<ScoreServerAccessor.LeaderBoardResponse> items){
        adapter.setItems(items);
        adapter.notifyDataSetChanged();

        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);
    }

    private boolean loadItems(){
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
            MyLog.log(getClass(), "refreshing content...");
            new UpdateLeaderScore().execute();

            return true;
        } else {
            MyLog.log(getClass(), "not refreshing content...not internet");
            mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), "Skontrolujte internetove pripojenie", Toast.LENGTH_SHORT).show();

        }

        return false;
    }

    private class UpdateLeaderScore extends AsyncTask<Void, Void, List<ScoreServerAccessor.LeaderBoardResponse>> {

        public UpdateLeaderScore(){
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

        protected List<ScoreServerAccessor.LeaderBoardResponse> doInBackground(Void ... param) {
            List<ScoreServerAccessor.LeaderBoardResponse> response = null;

            String token = mAccountManager.peekAuthToken(mAccount,AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS);
            if (token == null){
                return response;
            }

            ScoreServerAccessor scoreServerAccessor = new ScoreServerAccessor();
            try {
                response = scoreServerAccessor.getLeaderBoard(token, getActivity());
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleSilentException(e);
                return null;
            }


            return response;
        }

        protected void onProgressUpdate(Void... progress) {

        }

        @Override
        protected void onCancelled(List<ScoreServerAccessor.LeaderBoardResponse> leaderBoardResponses) {
            super.onCancelled(leaderBoardResponses);
            if (dialog!=null)
                dialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (dialog != null)
                dialog.dismiss();
        }

        protected void onPostExecute(List<ScoreServerAccessor.LeaderBoardResponse> result) {
            if (dialog==null)
                return;

            if (result != null) {
                addItems(result);
            }
            else {
                if (getActivity()!=null)
                    Toast.makeText(getActivity(), "Chyba pri komunikacii so serverom. Skúste neskôr. ", Toast.LENGTH_LONG).show();
            }

            dialog.dismiss();
        }
    }

}
