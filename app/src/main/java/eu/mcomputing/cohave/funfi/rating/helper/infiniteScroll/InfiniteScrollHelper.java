package eu.mcomputing.cohave.funfi.rating.helper.infiniteScroll;

import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.rating.adapter.WifiRatingAdapter;
import eu.mcomputing.cohave.funfi.rating.helper.WifiLoaderListener;

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
public class InfiniteScrollHelper implements InfiniteScrollListener {

    private int loading = InfiniteScrollListener.LoadingIdle;
    private int visibleThreshold = 10;
    private int firstVisibleItem, visibleItemCount, totalItemCount;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private WifiLoaderListener wifiLoaderListener;

    public InfiniteScrollHelper(LinearLayoutManager layoutManager, RecyclerView recyclerView, WifiLoaderListener wifiLoaderListener) {
        this.layoutManager = layoutManager;
        this.recyclerView = recyclerView;
        this.wifiLoaderListener = wifiLoaderListener;

        this.firstVisibleItem = 0;
        this.visibleItemCount = 0;
        this.totalItemCount = 0;
    }

    /**
     * Check data to load on the end of list
     * @param adapter
     */
    @Override
    public void checkDataToAdd(WifiRatingAdapter adapter) {
        visibleItemCount = recyclerView.getChildCount();
        totalItemCount = layoutManager.getItemCount();
        firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

        MyLog.log(getClass(), "Visible " + visibleItemCount + ", total " + totalItemCount + ", first " + firstVisibleItem);

        if (loading!=InfiniteScrollListener.LoadingIdle) {
            MyLog.log(getClass(), "waiting for data to fetch");
        } else if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
            //remaining less then visible + threshold, load next
            MyLog.log(getClass(), "adding new data");
            adapter.loadItems(visibleItemCount+visibleThreshold);
        }
    }

    @Override
    public void setEmptyViewIfNoData() {
        totalItemCount = layoutManager.getItemCount();

    }


    @Override
    public void setLoadingStart(String text) {
        MyLog.log(getClass(), "loading started " + text);
        loading=InfiniteScrollListener.LoadingRunning;
    }

    @Override
    public void setLoadingEnd() {
        MyLog.log(getClass(), "loading end");
        loading=InfiniteScrollListener.LoadingIdle;
    }

    @Override
    public void setLoadingError(String text) {
        MyLog.log(getClass(), "loading error");
        loading=InfiniteScrollListener.LoadingError;
    }

    @Override
    public int getStatus() {
        return loading;
    }
}
