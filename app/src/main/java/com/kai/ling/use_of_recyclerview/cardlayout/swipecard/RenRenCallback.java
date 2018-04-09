package com.kai.ling.use_of_recyclerview.cardlayout.swipecard;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import java.util.List;

import static com.kai.ling.use_of_recyclerview.cardlayout.swipecard.CardConfig.MAX_SHOW_COUNT;
import static com.kai.ling.use_of_recyclerview.cardlayout.swipecard.CardConfig.SCALE_GAP;
import static com.kai.ling.use_of_recyclerview.cardlayout.swipecard.CardConfig.TRANS_Y_GAP;


public class RenRenCallback extends ItemTouchHelper.SimpleCallback {

    protected RecyclerView mRv;
    protected List mDatas;
    protected RecyclerView.Adapter mAdapter;

    //前一个int是拖拽-后一个int是滑动
    //需要的是滑动消失（删除） ，所以我们的Callback不需要关注onMove()方法。
    public RenRenCallback(RecyclerView rv, RecyclerView.Adapter adapter, List datas) {
        this(0,
                ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                rv, adapter, datas);
    }

    public RenRenCallback(int dragDirs, int swipeDirs
            , RecyclerView rv, RecyclerView.Adapter adapter, List datas) {
        super(dragDirs, swipeDirs);
        mRv = rv;
        mAdapter = adapter;
        mDatas = datas;
    }

    //水平方向是否可以被回收掉的阈值
    public float getThreshold(RecyclerView.ViewHolder viewHolder) {
        //2016 12 26 考虑 探探垂直上下方向滑动，不删除卡片，这里参照源码写死0.5f
        return mRv.getWidth() * /*getSwipeThreshold(viewHolder)*/ 0.5f;
    }

    //在drag & drop 会回调
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    //滑动消失 swipe dismiss 回调  滑动删除动作已经发生后回调的
    //我们先滑动卡片，然后松手，此时ItemTouchHelper判断我们的手势是删除手势，会自动对这个卡片执行丢出屏幕外的动画，同时回调onSwiped()方法。
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        //★实现循环的要点
        //利用当前被删除的View的ViewHolder拿到Position--删除数据集中对应Position的数据源
        Object remove = mDatas.remove(viewHolder.getLayoutPosition());
        //同时将该数据源插入数据集中的首位，第0位
        mDatas.add(0, remove);
        //调用notifyDataSetChanged(),通知列表刷新
        //使用notifyDataSetChanged()原因:即ItemTouchHelper实现的滑动删除，其实只是隐藏了这个滑动的View。并不是真的删除了。
        mAdapter.notifyDataSetChanged();

        //notifyDataSetChanged()会回调LayoutManager.onLayoutChildren()这个函数，
        // 而在这个函数中，我们会重新布局，即真正的移除（不再layout）滑动掉的View，同时会补充进新的最底层的View。
    }


    //ItemTouchHelper还负责在其他孩子被拖动后绘制孩子。
    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        //先根据滑动的dxdy 算出现在动画的比例系数fraction
        double swipValue = Math.sqrt(dX * dX + dY * dY);
        double fraction = swipValue / getThreshold(viewHolder);
        //边界修正 最大为1
        if (fraction > 1) {
            fraction = 1;
        }
        //对每个ChildView进行缩放 位移
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            //第几层,举例子，count =7， 最后一个TopView（6）是第0层，
            int level = childCount - i - 1;
            if (level > 0) {
                child.setScaleX((float) (1 - SCALE_GAP * level + fraction * SCALE_GAP));

                if (level < MAX_SHOW_COUNT - 1) {
                    child.setScaleY((float) (1 - SCALE_GAP * level + fraction * SCALE_GAP));
                    child.setTranslationY((float) (TRANS_Y_GAP * level - fraction * TRANS_Y_GAP));
                } else {
                    //child.setTranslationY((float) (mTranslationYGap * (level - 1) - fraction * mTranslationYGap));
                }
            }
        }
    }
}
