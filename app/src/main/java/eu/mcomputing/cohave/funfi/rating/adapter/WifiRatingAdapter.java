package eu.mcomputing.cohave.funfi.rating.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.rating.helper.WifiLoaderListener;
import eu.mcomputing.cohave.funfi.rating.helper.infiniteScroll.InfiniteScrollListener;
import eu.mcomputing.cohave.funfi.rating.helper.swipe.SwipeItemTouchHelperAdapter;
import eu.mcomputing.cohave.funfi.rating.helper.undoButton.UndoButtonListener;

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
public class WifiRatingAdapter extends RecyclerView.Adapter<WifiRatingAdapter.ItemViewHolder>
        implements SwipeItemTouchHelperAdapter {

    private final Context context;
    private final List<WifiModel> mItems = new LinkedList<>();

    private final UndoButtonListener undoButtonListener;
    private final InfiniteScrollListener infiniteScrollListener;
    private final WifiLoaderListener wifiLoaderListener;

    protected String dataUrl;
    protected String[] dataAttr;
    protected String dataSelection;
    protected String[] dataSelectionArgs;
    protected String dataOrder;

    public static final int LOADER_QUERY_UPDATE = 1;
    public static final int LOADER_QUERY_SELECT = 2;

    public WifiRatingAdapter(
            Context context,
            UndoButtonListener undoButtonListener,
            InfiniteScrollListener infiniteScrollListener,
            WifiLoaderListener wifiLoaderListener
    ) {
        this.context=context;
        this.undoButtonListener = undoButtonListener;
        this.infiniteScrollListener = infiniteScrollListener;
        this.wifiLoaderListener = wifiLoaderListener;
    }

    /**
     * Set query config
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
    public int getMovementFlags(int position){
        if (position>=getItemCount() || position<0)
            return ItemTouchHelper.ACTION_STATE_IDLE;
        WifiModel item = mItems.get(position);

        MyLog.log(getClass(),"getMovementFlags "+ item.rating);

        if (item.rating == WifiModel.WIFI_NOLIKE)
        {
            return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT; //not rated item is swipeable to both of directions
        }
        else if (item.rating == WifiModel.WIFI_LIKE)
        {
            return ItemTouchHelper.RIGHT;
        }
        else if (item.rating == WifiModel.WIFI_DISLIKE)
        {
            return ItemTouchHelper.LEFT;
        }
        else
            return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

    }


    @Override
    public boolean isSwipeable(int position, int direction){
        if (position>=getItemCount() || position<0)
            return false;

        WifiModel item = mItems.get(position);

        MyLog.log(getClass(), "isSwipeable " + item.rating + " == " + direction);

        if (item.rating == WifiModel.WIFI_NOLIKE)
        {
            return true; //not rated item is swipeable to both of directions
        }
        else if (item.rating == WifiModel.WIFI_LIKE && direction == ItemTouchHelper.LEFT)
        {
            return false; // item is already liked can not be twice
        }
        else if (item.rating == WifiModel.WIFI_DISLIKE && direction == ItemTouchHelper.RIGHT)
        {
            return false; // item is already disliked can not be twice
        }
        else
            return true;

    }

    /**
     * Invalidate data in list. Empty list.
     */
    public void invalidateData(){
        mItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main, parent, false);
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        if (position>=getItemCount() || position<0)
            return;

        WifiModel item = mItems.get(position);
        holder.item_center.setText(item.ssid);
        holder.item_bottom_right.setText(String.valueOf(item.neg_score));
        holder.item_bottom_left.setText(String.valueOf(item.pos_score));

        holder.item_bottom_center.setBackgroundResource(getRatingBorder(item.pos_score,item.neg_score));

    }

    private int getRatingBorder(long positive, long negative){
        if (positive==negative)
            return R.drawable.myborder5;

        long sum = positive + negative;
        int p =  Math.round((10 / (float)sum) * (float)positive);

        switch (p) {
            case 0:
                return  R.drawable.myborder0;
            case 1:
                return  R.drawable.myborder1;
            case 2:
                return  R.drawable.myborder2;
            case 3:
                return  R.drawable.myborder3;
            case 4:
                return  R.drawable.myborder4;
            case 5:
                return  R.drawable.myborder5;
            case 6:
                return  R.drawable.myborder6;
            case 7:
                return  R.drawable.myborder7;
            case 8:
                return  R.drawable.myborder8;
            case 9:
                return  R.drawable.myborder9;
            case 10:
                return  R.drawable.myborder10;
        }
        return R.drawable.myborder5;
    }

    /**
     * Append next "count" items from table used by sql limit
     * @param count
     */
    public void loadItems(int count){
        MyLog.log(getClass(), " loading items");

        //run only when previous loading ended, otherwise it cause data inconsistency exception
        if (infiniteScrollListener.getStatus() == InfiniteScrollListener.LoadingIdle) {
            Bundle b = new Bundle();
            b.putLong("last_id", mItems.size() == 0 ? -1 : mItems.get(mItems.size() - 1)._id);
            b.putInt("total",mItems.size());
            b.putInt("limit", count);
            b.putInt("offset", mItems.size());
            b.putStringArray("dataAttr", dataAttr);
            b.putString("dataSelection", dataSelection);
            b.putStringArray("dataSelectionArgs", dataSelectionArgs);
            b.putString("dataOrder", dataOrder);

            wifiLoaderListener.wifiLoaderQuery(b, LOADER_QUERY_SELECT);
        }

    }

    public void loadItemsCallback(ArrayList<WifiModel> items) throws Exception{
        MyLog.log(getClass(),"loadItemsCallback items "+items.size()+" total "+ mItems.size());

        int last_pos = mItems.size();
        mItems.addAll(items);

        notifyItemRangeInserted(last_pos, last_pos + items.size());
        MyLog.log(getClass(), "data fetched to " + last_pos + " ( " + items.size() + "), total " + mItems.size());
    }

    @Override
    public int getItemCount() {
        return mItems==null ? 0 : mItems.size();
    }

    /**
     * Undo dismiss, hide actionbutton
     *
     * @param item
     * @param position
     */
    public void undoDismiss(WifiModel item, int position){
        MyLog.log(getClass(), "undoDismiss " + position + " type: "+item.rating);

        mItems.add(position, item);
        notifyItemInserted(position);
        undoButtonListener.hide();

        MyLog.log(getClass(), "undoDismiss " + position + " end");
        itemUndoDismissed(item);
    }

    /* ------------ Swiping methods:  SwipeItemTouchHelperAdapter -------------- */

    @Override
    public void onItemDismiss(int position, int direction) {
        MyLog.log(getClass(), "onItemDismiss position(" + position + ") direction("+direction+")");
        if (direction==ItemTouchHelper.LEFT || direction==ItemTouchHelper.RIGHT) {

            WifiModel content = mItems.get(position);

            MyLog.log(getClass(), "onItemDismiss: WifiUpdateTask " +content.rating +" ... " + direction);

            mItems.remove(position);
            notifyItemRemoved(position);

            if (direction == ItemTouchHelper.LEFT) {
                itemDismissedLeft(content);
            } else if (direction == ItemTouchHelper.RIGHT) {
                itemDismissedRight(content);
            }

            infiniteScrollListener.checkDataToAdd(this);
            undoButtonListener.show(content, position, this);
        }
    }

    @Override
    public void itemDismissedLeft(WifiModel item) {
        MyLog.log(getClass(), "itemDismissedLeft -- like");

        Bundle b = new Bundle();
        b.putLong("id", item._id);
        b.putInt("type", WifiModel.WIFI_LIKE);
        wifiLoaderListener.wifiLoaderQuery(b,LOADER_QUERY_UPDATE);

        MyLog.log(getClass(), "itemDismissedLeft -- like end");
    }

    @Override
    public void itemDismissedRight(WifiModel item) {
        MyLog.log(getClass(), "itemDismissedRight -- dislike");

        Bundle b = new Bundle();
        b.putLong("id",item._id);
        b.putInt("type", WifiModel.WIFI_DISLIKE);
        wifiLoaderListener.wifiLoaderQuery(b,LOADER_QUERY_UPDATE);

        MyLog.log(getClass(), "itemDismissedRight -- dislike");
    }

    @Override
    public void itemUndoDismissed(WifiModel item) {
        MyLog.log(getClass(), "itemUndoDismissed -- undo "+item.rating);

        Bundle b = new Bundle();
        b.putLong("id", item._id);
        b.putInt("type", item.rating);
        wifiLoaderListener.wifiLoaderQuery(b,LOADER_QUERY_UPDATE);

        MyLog.log(getClass(), "itemUndoDismissed -- undo end");
    }

    /* ------------------------------------ */

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        public final TextView item_center;
        //public final ImageView item_left;
        public final TextView item_bottom_center;
        public final TextView item_bottom_right;
        public final TextView item_bottom_left;

        public ItemViewHolder(View itemView) {
            super(itemView);
            item_center = (TextView) itemView.findViewById(R.id.item_center);
            //item_left = (ImageView) itemView.findViewById(R.id.item_left);
            item_bottom_center = (TextView) itemView.findViewById(R.id.item_bottom_center);
            item_bottom_right = (TextView) itemView.findViewById(R.id.item_bottom_right);
            item_bottom_left = (TextView) itemView.findViewById(R.id.item_bottom_left);
        }
    }
}
