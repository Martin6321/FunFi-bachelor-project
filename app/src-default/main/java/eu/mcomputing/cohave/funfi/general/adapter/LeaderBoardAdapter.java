package eu.mcomputing.cohave.funfi.general.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.rating.helper.WifiLoaderListener;
import eu.mcomputing.cohave.funfi.rating.helper.infiniteScroll.InfiniteScrollListener;
import eu.mcomputing.cohave.funfi.rating.helper.swipe.SwipeItemTouchHelperAdapter;
import eu.mcomputing.cohave.funfi.rating.helper.undoButton.UndoButtonListener;
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
public class LeaderBoardAdapter extends RecyclerView.Adapter<LeaderBoardAdapter.ItemViewHolder> {

    private final List<ScoreServerAccessor.LeaderBoardResponse> mItems = new ArrayList<>();


    public LeaderBoardAdapter() {

    }

    public void setItems(List<ScoreServerAccessor.LeaderBoardResponse> items){
        mItems.clear();
        mItems.addAll(items);
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.leaderboard_item, parent, false);
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        ScoreServerAccessor.LeaderBoardResponse item = mItems.get(position);

        holder.item_left.setText((position+1)+". "+item.username);
        holder.item_right.setText(String.valueOf(item.score));
    }

    @Override
    public int getItemCount() {
        return mItems==null ? 0 : mItems.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        public final TextView item_left;
        public final TextView item_right;

        public ItemViewHolder(View itemView) {
            super(itemView);
            item_left = (TextView) itemView.findViewById(R.id.item_left);
            item_right = (TextView) itemView.findViewById(R.id.item_right);
        }
    }
}
