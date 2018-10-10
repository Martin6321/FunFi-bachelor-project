package eu.mcomputing.cohave.funfi.rating.helper.undoButton;


import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;

import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.rating.adapter.WifiRatingAdapter;

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
public class UndoButtonHelper implements UndoButtonListener {
    private FloatingActionButton myFab;
    private Runnable fabHideHandler = null;

    public UndoButtonHelper(FloatingActionButton fab) {
        this.myFab = fab;
        myFab.hide();
    }

    @Override
    public void show(final WifiModel item, final int position, final WifiRatingAdapter adapter) {
        // set undo button event to undo dismiss
        myFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.undoDismiss(item, position);
            }
        });

        MyLog.log(getClass() , "showing");
        if (!myFab.isShown())
            myFab.show();

        //remove previous postDelayed message
        if (fabHideHandler != null)
            myFab.removeCallbacks(fabHideHandler);

        //hide undo button after 5 seconds
        myFab.postDelayed(fabHideHandler = new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, 5000);

    }

    @Override
    public void hide() {
        MyLog.log(getClass(), "hidding");
        if (myFab.isShown())
            myFab.hide();
    }
}
