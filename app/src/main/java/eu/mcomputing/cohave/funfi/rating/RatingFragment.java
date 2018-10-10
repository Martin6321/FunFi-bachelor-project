package eu.mcomputing.cohave.funfi.rating;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;

import eu.mcomputing.cohave.funfi.MainActivity;
import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.rating.adapter.WifiRatingAdapter;
import eu.mcomputing.cohave.funfi.rating.fragments.FunFiFragment;
import eu.mcomputing.cohave.funfi.rating.helper.WifiLoaderListener;
import eu.mcomputing.cohave.funfi.rating.helper.infiniteScroll.InfiniteScrollHelper;
import eu.mcomputing.cohave.funfi.rating.helper.swipe.SwipeItemTouchHelperCallback;
import eu.mcomputing.cohave.funfi.rating.helper.undoButton.UndoButtonHelper;
import eu.mcomputing.cohave.helper.config.AuthConfig;

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
public class RatingFragment extends Fragment implements WifiLoaderListener {

    private View snackBarView;
    private Snackbar snackbar;

    private final LinearLayoutManager mLayoutManager;
    private RecyclerView recyclerView;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WifiRatingAdapter adapter;

    private ItemTouchHelper mItemTouchHelper;
    private UndoButtonHelper mUndoButtonHelper;
    private InfiniteScrollHelper mInfiniteScrollHelper;

    private Object syncStatusObserver = null;

    private AccountManager accountManager;
    private Account connectedAccount;

    protected String dataUrl;
    protected String[] dataAttr;
    protected String dataSelection;
    protected String[] dataSelectionArgs;
    protected String dataOrder;

    private int updatingCount = 0;
    public boolean wasShown = false;


    public RatingFragment() {
        mLayoutManager = new LinearLayoutManager(getActivity());
    }


    /**
     * Set query config
     *
     * @param dataUrl
     * @param dataAttr
     * @param dataSelection
     * @param dataSelectionArgs
     * @param dataOrder
     */
    public void setDatabaseConfig(String dataUrl, String[] dataAttr, String dataSelection, String[] dataSelectionArgs, String dataOrder) {
        this.dataUrl = dataUrl;
        this.dataAttr = dataAttr;
        this.dataSelection = dataSelection;
        this.dataSelectionArgs = dataSelectionArgs;
        this.dataOrder = dataOrder;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recyclerview, container, false);
        return root;
    }

    /**
     * Method called in onViewCreated
     *
     * @param view
     * @param savedInstanceState
     */
    public void fragmentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //connect to user account
        connectAccount();

        //register snackbar
        snackBarView = view.findViewById(R.id.mysnackbar);

        //add recycler view
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(mLayoutManager);

        //add swipe to refresh event
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.wifi_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ConnectivityManager cm =
                        (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

                if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                    MyLog.log(getClass(), "refreshing content...");
                    refreshData();//not tu run websync but a local sync, because websync is called on background
                    requestSync();//run on background if user will second swipe new data will be available
                } else {
                    MyLog.log(getClass(), "not refreshing content...not internet");
                    mSwipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getActivity(), "Skontrolujte internetove pripojenie", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mUndoButtonHelper = new UndoButtonHelper((FloatingActionButton) view.findViewById(R.id.fab));
        mInfiniteScrollHelper = new InfiniteScrollHelper(mLayoutManager, recyclerView, this);

        //creating adapter
        adapter = new WifiRatingAdapter(
                getActivity(),
                mUndoButtonHelper, mInfiniteScrollHelper, this
        );
        //set query config
        adapter.setDatabaseConfig(dataUrl, dataAttr, dataSelection, dataSelectionArgs, dataOrder);
        //add adapter with default null cursor
        recyclerView.setAdapter(adapter);

        //add item touch event for swiping and clicking
        ItemTouchHelper.Callback callback = new SwipeItemTouchHelperCallback(adapter, getActivity().getApplicationContext());
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        //add scroll event for infinite scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //check data count if we are not at the end
                if (!mSwipeRefreshLayout.isRefreshing()) {
                    MyLog.log(getClass(), "data scrolled check to add");
                    mInfiniteScrollHelper.checkDataToAdd(adapter);
                }
            }
        });

        mInfiniteScrollHelper.checkDataToAdd(adapter);
        mInfiniteScrollHelper.setEmptyViewIfNoData();

    }

    public void showSnackbar(String text){
        showSnackbar(text,Snackbar.LENGTH_INDEFINITE);
    }

    public void showSnackbar(String text, int length) {
        MyLog.log(getClass(), "showing snackbar " + text);
        if (this.snackbar != null) {
            this.snackbar.dismiss();
        }
        this.snackbar = Snackbar.make(snackBarView, text, length);
        this.snackbar.show();
    }

    public void hideSnackbar() {
        MyLog.log(getClass(), "hidding snackbar");
        if (this.snackbar != null) {
            this.snackbar.dismiss();
            this.snackbar = null;
        }
    }


    /* ----------------- WifiLoaderListener ------------------ */

    @Override
    public void wifiLoaderQuery(Bundle bundle, int type) {
        if (type == WifiRatingAdapter.LOADER_QUERY_UPDATE) {
            new WifiUpdateTask(getActivity().getApplicationContext()).execute(bundle);
        } else if (type == WifiRatingAdapter.LOADER_QUERY_SELECT) {
            new WifiLoadTask(getActivity().getApplicationContext()).execute(bundle);
        }
    }


    /* ------------------------------------------------------- */

    /**
     * Async load
     */
    private class WifiLoadTask extends AsyncTask<Bundle, Void, ArrayList<WifiModel>> {
        private boolean refresh = false;
        private final Context context;

        public WifiLoadTask(Context context) {
            this.context = context;
        }

        @Override
        // async task in background loads wifis from database
        protected ArrayList<WifiModel> doInBackground(Bundle... params) {
            try {
                for (Bundle param : params) {

                    Cursor cursor = context.getContentResolver().query(
                            WifiContentProvider.getLimitUri(dataUrl, param.getInt("limit", 0), param.getInt("offset", 0)),
                            param.getStringArray("dataAttr"),
                            param.getString("dataSelection"),
                            param.getStringArray("dataSelectionArgs"),
                            param.getString("dataOrder")
                    );

                    ArrayList<WifiModel> wifiModels = new ArrayList<>();
                    if (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
                        cursor.moveToNext();
                        while (!cursor.isAfterLast()) {
                            wifiModels.add(new WifiModel(cursor));
                            cursor.moveToNext();
                        }
                        cursor.close();
                    }

                    MyLog.log(getClass(), "data loaded total " + wifiModels.size());
                    return wifiModels;
                }
            } catch (Exception e) {
                hideSnackbar();
                MyLog.log(getClass(), "doInBackground error");
                e.printStackTrace();
                mInfiniteScrollHelper.setLoadingError("Chyba pri načítavaní ...");
                showSnackbar("Chyba pri načítavaní ...");
                ACRA.getErrorReporter().handleSilentException(e);
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(ArrayList<WifiModel> result) {
            MyLog.log(getClass(), "finished loading  " + result.size());
            try {
                if (refresh) {
                    adapter.invalidateData();
                }



                ////// tu nacitavam posielam nacitane data do recycle listu
                adapter.loadItemsCallback(result);
                mInfiniteScrollHelper.setLoadingEnd();
                hideSnackbar();
                if (adapter.getItemCount() == 0){
                    showSnackbar("Pre stiahnutie WiFi sietí je potrebné mať internetové pripojenie !", Snackbar.LENGTH_INDEFINITE);
                }else if (RatingFragment.this.wasShown == false) {
                    showSnackbar("Hlasuj potiahnutím vľavo alebo vpravo", Snackbar.LENGTH_LONG);
                    RatingFragment.this.wasShown = true;
                }

            } catch (Exception e) {
                hideSnackbar();
                MyLog.log(getClass(), "onPostExecute error");
                e.printStackTrace();
                mInfiniteScrollHelper.setLoadingError("Chyba pri načítavaní ...");
                showSnackbar("Chyba pri načítavaní ...");
                ACRA.getErrorReporter().handleSilentException(e);
            }
        }

        @Override
        protected void onPreExecute() {
            mInfiniteScrollHelper.setLoadingStart("Nacitavam WiFi siete ...");
            showSnackbar("Načítavam WiFi siete ...");
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    /**
     * Async update
     */
    private class WifiUpdateTask extends AsyncTask<Bundle, Void, Long> {

        private final Context context;

        public WifiUpdateTask(Context context) {
            this.context = context;
        }

        @Override
        protected Long doInBackground(Bundle... params) {
            for (Bundle param : params) {
                MyLog.log(getClass(), "updating " + param.getLong("id") + " --> " + param.getInt("type"));
                context.getContentResolver().update(
                        ContentUris.withAppendedId(WifiContentProvider.WIFI_CONTENT_URI, param.getLong("id")),
                        WifiModel.getLikeContentValues(param.getInt("type")),
                        null,
                        null
                );

                return param.getLong("id");

            }
            return 0l;
        }

        @Override
        protected void onPostExecute(Long result) {
            MyLog.log(getClass(), "finished updating " + result);
            if (updatingCount > 0) updatingCount--;

        }

        @Override
        protected void onPreExecute() {
            updatingCount++;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    /* ------------ Sync related methods --------------------- */

    private void connectAccount() {
        accountManager = AccountManager.get(getActivity());
        Account[] accounts = accountManager.getAccountsByType(AuthConfig.Auth.ACCOUNT_TYPE);
        if (accounts != null && accounts.length == 1) {
            connectedAccount = accounts[0];
        } else {
            connectedAccount = null;
        }
    }

    /**
     * Request network sync
     */
    public void requestSync() {
        if (connectedAccount == null)
            connectAccount();

        if (connectedAccount != null) {
            MyLog.log(getClass(), "Synchronizácia ...");
            //mInfiniteScrollHelper.setLoadingStart("Synchronizácia ...");
            //showSnackbar("Synchronizácia ...");

            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); // Performing a sync no matter if it's off
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); // Performing a sync no matter if it's off
            ContentResolver.requestSync(connectedAccount, WifiContentProvider.AUTHORITY, bundle);
            MyLog.log(getClass(), "Requesting Sync .... wifi data");
        }

    }

    /* ------------------------------------------------------- */

    /**
     * When network sync finished update UI
     */
    public void refreshData() {
        MyLog.log(getClass(), "refreshing data");
        hideSnackbar();
        if (mSwipeRefreshLayout.isRefreshing())
            mSwipeRefreshLayout.setRefreshing(false);

        adapter.invalidateData();
        mInfiniteScrollHelper.checkDataToAdd(adapter);
    }

//    /**
//     * Register sync observer to run after network sync end
//     */
//    public void registerSyncObserver() {
//        MyLog.log(getClass(), "register Syncobserver");
//        if (syncStatusObserver == null) {
//            syncStatusObserver = ContentResolver.addStatusChangeListener(
//                    ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE | ContentResolver.SYNC_OBSERVER_TYPE_PENDING,
//                    new SyncStatusObserver() {
//                        @Override
//                        public void onStatusChanged(int mask) {
//
//                            boolean sync = ContentResolver.isSyncActive(connectedAccount, WifiContentProvider.AUTHORITY);
//                            boolean pending = ContentResolver.isSyncPending(connectedAccount, WifiContentProvider.AUTHORITY);
//                            MyLog.log(getClass(), "sync status changed " + mask + " sync " + sync + " , pend: " + pending);
//
//                            if (!sync && !pending) {
//                                mInfiniteScrollHelper.setLoadingEnd();
//                                getActivity().runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        refreshData();
//                                    }
//                                });
//                            }
//                        }
//                    });
//        }
//    }
//
//    /**
//     * Unregister sync observer for network sync
//     */
//    public void unregisterSyncObserver() {
//        MyLog.log(getClass(), "unregister Syncobserver");
//        if (syncStatusObserver != null) {
//            ContentResolver.removeStatusChangeListener(syncStatusObserver);
//            syncStatusObserver = null;
//        }
//    }
}
