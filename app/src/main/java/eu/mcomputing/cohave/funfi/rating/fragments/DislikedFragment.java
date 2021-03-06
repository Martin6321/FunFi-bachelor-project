package eu.mcomputing.cohave.funfi.rating.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.rating.RatingFragment;

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
public class DislikedFragment extends RatingFragment {

    public DislikedFragment() {
        super();
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDatabaseConfig(
                WifiContentProvider.WIFI_URL,
                WifiModel.getListAttr(),
                WifiModel.Table.RATING + "=?",
                new String[]{WifiModel.WIFI_DISLIKE + ""},
                WifiModel.Table.NEG_SCORE + " DESC"
        );
        fragmentViewCreated(view, savedInstanceState);
        requestSync();
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        registerSyncObserver();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        unregisterSyncObserver();
//    }

}
