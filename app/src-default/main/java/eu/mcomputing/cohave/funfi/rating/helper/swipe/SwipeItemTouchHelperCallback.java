package eu.mcomputing.cohave.funfi.rating.helper.swipe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;

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
public class SwipeItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final SwipeItemTouchHelperAdapter mAdapter;
    private final Context context;

    public SwipeItemTouchHelperCallback(SwipeItemTouchHelperAdapter adapter, Context context) {
        mAdapter = adapter;
        this.context=context;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = 0;

        return makeMovementFlags(dragFlags, mAdapter.getMovementFlags(viewHolder.getAdapterPosition()));
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (mAdapter.isSwipeable(viewHolder.getAdapterPosition(),direction))
            mAdapter.onItemDismiss(viewHolder.getAdapterPosition(),direction);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Get RecyclerView item from the ViewHolder
            View itemView = viewHolder.itemView;
            Paint paint = new Paint();

            Bitmap like_bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_thumb_up_outline);
            Bitmap dislike_bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_thumb_down_outline);
            float width = like_bitmap.getWidth();

            if (dX > 0) { // swiping right
                paint.setColor(context.getResources().getColor(R.color.swipeDislike));
                c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX + dX, (float) itemView.getBottom(), paint);

                if (dX > 96f + width) {//when slided are is more than icon width show icon
                    float height = (itemView.getHeight() / 2) - (dislike_bitmap.getHeight() / 2);
                    c.drawBitmap(dislike_bitmap, 96f, (float) itemView.getTop() + height, null);
                }

            } else { // swiping left
                paint.setColor(context.getResources().getColor(R.color.swipeLike));
                c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom(), paint);

                if (dX < -1* (96+width)) {//when slided are is more than icon width show icon
                    float height = (itemView.getHeight() / 2) - (dislike_bitmap.getHeight() / 2);
                    c.drawBitmap(like_bitmap, ((float) itemView.getRight() - like_bitmap.getWidth()) - 96f, (float) itemView.getTop() + height, null);
                }

            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}
