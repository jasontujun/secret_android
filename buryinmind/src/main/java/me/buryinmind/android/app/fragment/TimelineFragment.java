package me.buryinmind.android.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.activity.MemoryDetailActivity;
import me.buryinmind.android.app.activity.MemoryGiftActivity;
import me.buryinmind.android.app.adapter.XViewHolder;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.dialog.ConfirmDialog;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.uicontrol.XLinearLayoutManager;
import me.buryinmind.android.app.uicontrol.XParticleLayout;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/6/15.
 */
public class TimelineFragment extends XFragment {

    private static final String TAG = "BIM_MainActivity";
    public static final int REFRESH_EXPAND = 11;
    public static final int REFRESH_COLLAPSE = 12;
    public static final int REFRESH_SET_BIRTHDAY = 21;
    public static final int REFRESH_ADD_MEMORY = 31;

    private XListIdDataSourceImpl<Memory> mMemorySource;
    private RecyclerView mTimelineView;
    private TimelineAdapter mAdapter;

    private boolean mWaiting;
    private boolean mListTop = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register data listener
        mMemorySource = (XListIdDataSourceImpl<Memory>)
                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
        // init list adapter
        mAdapter = new TimelineAdapter(mMemorySource.copyAll());
        // 在onCreate时请求数据，可以避免fragment切换时过于频繁请求
        requestMemoryList();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_timeline, container, false);
        mTimelineView = (RecyclerView) rootView.findViewById(R.id.timeline_list);

        mTimelineView.setLayoutManager(new XLinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false));
        mTimelineView.setItemAnimator(new DefaultItemAnimator());
        mTimelineView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 已经滑到顶部，不能再向上滑了
                if (!ViewCompat.canScrollVertically(recyclerView, -1)) {
                    if (!mListTop) {
                        mListTop = true;
                        notifyRefresh(REFRESH_EXPAND, null);
                        XLog.d(TAG, "mTimelineView scroll to top! expand appbar!");
                    } else {
                        XLog.d(TAG, "mTimelineView already scroll to top! not expand appbar!");
                    }
                } else {
                    mListTop = false;
                }
            }
        });
        mTimelineView.setAdapter(mAdapter);

        return rootView;
    }

    public void addMemory(final Memory memory) {
        if (memory == null)
            return;
        mTimelineView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mMemorySource.add(memory);
                mMemorySource.sort(Memory.comparator);
                int index = mMemorySource.indexOf(memory);
                mAdapter.addData(index, memory);
                // 闪烁动画提示
                mAdapter.addAnimateItem(memory, ViewUtil.animateShine(5),
                        R.id.timeline_node_swipe_layout);
                // 滚动到刚添加的item处
                if (index > 0) {
                    notifyRefresh(REFRESH_COLLAPSE, null);
                }
                mTimelineView.smoothScrollToPosition(index + 1);
                mAdapter.notifyItemChanged(index + 1);
            }
        }, 50);
    }

    private void requestMemoryList() {
        if (mWaiting)
            return;
        mWaiting = true;
        notifyLoading(true);
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryList(),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onCancelled() {
                        mWaiting = false;
                        notifyLoading(false);
                    }

                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        notifyLoading(false);
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        notifyLoading(false);
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        mWaiting = false;
                        notifyLoading(false);
                        List<Memory> memories = Memory.fromJson(jsonArray);
                        if (memories != null && memories.size() > 0) {
                            mMemorySource.addAll(memories);
                            mMemorySource.sort(Memory.comparator);
                            mAdapter.setData(mMemorySource.copyAll());
                        }
                        // 获取待接收列表
                        getMemoryGift();
                    }
                }));
    }

    private boolean deleteMemory(final Memory memory) {
        if (mWaiting)
            return false;
        mWaiting = true;
        mAdapter.addLoadingItem(memory);
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.deleteMemory(memory.mid),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onCancelled() {
                        mWaiting = false;
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object obj) {
                        XLog.d(TAG, "deleteMemory onFinishSuccess()! mid=" + memory.mid);
                        mWaiting = false;
                        mMemorySource.deleteById(memory.mid);
                        mAdapter.deleteData(memory);
                    }
                }
        ));
        return true;
    }

    private void getMemoryGift() {
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.inMemory(),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onCancelled() {}

                    @Override
                    public void onNetworkError() {
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jo) {
                        List<Memory> paMemories = null;
                        List<Memory> pbMemories = null;
                        try {
                            if (jo.has("pa") && !jo.isNull("pa")) {
                                JSONArray pa = jo.getJSONArray("pa");
                                paMemories = Memory.fromGiftJson(pa);
                            }
                            if (jo.has("pb") && !jo.isNull("pb")) {
                                JSONArray pb = jo.getJSONArray("pb");
                                pbMemories = Memory.fromGiftJson(pb);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        final List<Memory> inMemories = new ArrayList<Memory>();
                        if (paMemories != null && paMemories.size() > 0) {
                            inMemories.addAll(paMemories);
                        }
                        if (pbMemories != null && pbMemories.size() > 0) {
                            inMemories.addAll(pbMemories);
                        }
                        if (inMemories.size() > 0) {
                            mMemorySource.addAll(inMemories);
                            mMemorySource.sort(Memory.comparator);
                            mAdapter.setData(mMemorySource.copyAll());
                            // 闪烁动画提示
                            for (Memory m : inMemories) {
                                mAdapter.addAnimateItem(m, ViewUtil.animateShine(5),
                                        R.id.timeline_node_swipe_layout);
                            }
                            // 滚动到第一个GiftMemory
                            mTimelineView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    int index = mMemorySource.indexOf(inMemories.get(0));
                                    if (index > 0) {
                                        notifyRefresh(REFRESH_COLLAPSE, null);
                                    }
                                    mTimelineView.smoothScrollToPosition(index + 1);
                                    mAdapter.notifyItemChanged(index + 1);
                                }
                            }, 50);
                        }
                    }
                }));
    }

    private boolean receiveMemory(final Memory memory, String gid, String answer) {
        if (mWaiting)
            return false;
        mWaiting = true;
        mAdapter.addLoadingItem(memory);
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.receiveMemory(gid, answer),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onCancelled() {
                        mWaiting = false;
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jo) {
                        mWaiting = false;
                        try {
                            memory.mid = jo.getString("mid");
                            memory.inGift = null;
                            final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                                    .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                            memory.ownerId = user.uid;
                            memory.ownerName = user.name;
                            mAdapter.addAnimateItem(memory, ViewUtil.animateScale(5.0f, 50f, 50f, null),
                                    R.id.timeline_node_stamp);
                            mAdapter.removeLoadingItem(memory);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }));
        return true;
    }

    private boolean rejectMemory(final Memory memory, String gid) {
        if (mWaiting)
            return false;
        mWaiting = true;
        mAdapter.addLoadingItem(memory);
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.rejectMemory(gid),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onCancelled() {
                        mWaiting = false;
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                        mWaiting = false;
                        mAdapter.removeLoadingItem(memory);
                        mMemorySource.deleteById(memory.mid);
                        mAdapter.deleteData(memory);
                    }
                }));
        return true;
    }

    private void gotoDetail(Memory memory) {
        Intent intent = memory.editable ?
                new Intent(getActivity(), MemoryDetailActivity.class) :
                new Intent(getActivity(), MemoryGiftActivity.class);
        intent.putExtra("mid", memory.mid);
        startActivity(intent);
    }

    public boolean isScrollToTop() {
        return mListTop;
    }


    private class TimelineAdapter extends RecyclerView.Adapter<XViewHolder> {

        private static final int TYPE_HEADER = 1;
        private static final int TYPE_FOOTER = 2;
        private static final int TYPE_NODE = 3;

        private Map<Integer, Memory> mAges;
        private List<Memory> mItems;
        private List<Memory> mLoadingItems;
        private Map<Memory, Pair<Animation, Integer>> mItemAnimations;

        public TimelineAdapter(List<Memory> items) {
            mAges = new HashMap<Integer, Memory>();
            mLoadingItems = new ArrayList<Memory>();
            mItemAnimations = new HashMap<Memory, Pair<Animation, Integer>>();
            setData(items);
        }

        public void setData(final List<Memory> memories) {
            mItems = memories;
            mItemAnimations.clear();
            mLoadingItems.clear();
            mAges.clear();
            for (Memory memory : memories) {
                if (!mAges.containsKey(memory.age)) {
                    mAges.put(memory.age, memory);
                }
            }
            notifyDataSetChanged();
        }

        public void addData(int pos, Memory memory) {
            mItems.add(pos, memory);
            if (!mAges.containsKey(memory.age)) {
                mAges.put(memory.age, memory);
            } else {
                // 如果新增的memory比同样age的memory早，则替换
                Memory m = mAges.get(memory.age);
                if (Memory.comparator.compare(memory, m) < 0) {
                    mAges.put(memory.age, memory);
                }
            }
            notifyItemInserted(pos + 1);
            notifyItemRangeChanged(pos + 1, Math.min(2, mItems.size() - (pos + 1)));
        }

        public void deleteData(Memory memory) {
            int pos = mItems.indexOf(memory);
            if (pos == -1) {
                return;
            }
            mItems.remove(memory);
            mLoadingItems.remove(memory);
            mItemAnimations.remove(memory);
            if (memory.equals(mAges.get(memory.age))) {
                boolean needRemove = true;
                for (Memory item : mItems) {
                    if (item.age == memory.age) {
                        mAges.put(item.age, item);
                        needRemove = false;
                        break;
                    }
                }
                if (needRemove) {
                    mAges.remove(memory.age);
                }
            }
            notifyItemRemoved(pos + 1);
            notifyItemRangeChanged(pos + 1, Math.min(2, mItems.size() - (pos + 1)));
        }

        public void addAnimateItem(Memory memory, Animation anim, int viewId) {
            int pos = mItems.indexOf(memory);
            if (pos == -1) {
                return;
            }
            mItemAnimations.put(memory, new Pair<Animation, Integer>(anim, viewId));
        }

        public void addLoadingItem(Memory memory) {
            int pos = mItems.indexOf(memory);
            if (pos == -1) {
                return;
            }
            if (!mLoadingItems.contains(memory)) {
                mLoadingItems.add(memory);
                notifyItemChanged(pos + 1);
            }
        }

        public void removeLoadingItem(Memory memory) {
            int pos = mItems.indexOf(memory);
            if (pos == -1) {
                return;
            }
            if (mLoadingItems.remove(memory)) {
                notifyItemChanged(pos + 1);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return TYPE_HEADER;
            else if (position == mItems.size() + 1)
                return TYPE_FOOTER;
            else
                return TYPE_NODE;
        }

        @Override
        public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resId;
            if (viewType == TYPE_HEADER)
                resId = R.layout.item_timeline_header;
            else if (viewType == TYPE_FOOTER)
                resId = R.layout.item_timeline_footer;
            else
                resId = R.layout.item_timeline_node;
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(resId, parent, false);
            return new XViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final XViewHolder holder, final int position) {
            int type = getItemViewType(position);
            final User user = ((GlobalSource) XDefaultDataRepo.getInstance().
                    getSource(MyApplication.SOURCE_GLOBAL)).getUser();
            if (type == TYPE_HEADER) {
                holder.getView(R.id.timeline_header_layout).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 跳转到生日设置的fragment
                                notifyRefresh(REFRESH_SET_BIRTHDAY, null);
                            }
                        });
                Calendar birth = TimeUtil.getCalendar(user.bornTime);
                holder.getView(R.id.timeline_header_time, TextView.class)
                        .setText(XStringUtil.calendar2str(birth, "."));
            } else if (type == TYPE_FOOTER) {
                holder.getView(R.id.timeline_footer_txt).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 跳转到AddMemoryFragment
                                notifyRefresh(REFRESH_ADD_MEMORY, null);
                            }
                        });
            } else {
                final Memory item = mItems.get(position - 1);
                // 设置age标签
                if (item.age > 0 && mAges.get(item.age).equals(item)) {
                    holder.getView(R.id.timeline_tag, TextView.class)
                            .setText(String.valueOf(item.age));
                    holder.getView(R.id.timeline_tag_layout).setVisibility(View.VISIBLE);
                }else {
                    holder.getView(R.id.timeline_tag_layout).setVisibility(View.GONE);
                }
                // 设置头像。如果不是自己的回忆,显示赠送者头像
                if (!user.uid.equals(item.authorId)) {
                    holder.getView(R.id.timeline_sender_name).setVisibility(View.VISIBLE);
                    holder.getView(R.id.timeline_sender_name, TextView.class).setText(
                            getResources().getString(R.string.info_memory_sender, item.authorName));
                    holder.getView(R.id.timeline_sender_head).setVisibility(View.VISIBLE);
                    Glide.with(TimelineFragment.this)
                            .load(ApiUtil.getIdUrl(item.authorId))
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.headicon_default)
                            .into(holder.getView(R.id.timeline_sender_head, ImageView.class));
                    if (item.inGift != null) {// 如果是未接收的回忆，则头像闪动提醒
                        ViewUtil.animateScale(holder.getView(R.id.timeline_sender_head));
                    } else {
                        holder.getView(R.id.timeline_sender_head).setAnimation(null);
                    }
                } else {
                    holder.getView(R.id.timeline_sender_name).setVisibility(View.GONE);
                    holder.getView(R.id.timeline_sender_head).setVisibility(View.GONE);
                    holder.getView(R.id.timeline_sender_head).setAnimation(null);
                }
                final XParticleLayout particleLayout = (XParticleLayout) holder.getView(R.id.timeline_node_layout);
                // 正在loading的条目，不显示内容，也不能点击
                if (mLoadingItems.contains(item)) {
                    holder.getView(R.id.loading_progress).setVisibility(View.VISIBLE);
                    holder.getView(R.id.timeline_node_swipe_layout).setVisibility(View.GONE);
                    particleLayout.hide();
                    particleLayout.setOnClickListener(null);
                }
                // 非loading的条目，正常显示和点击
                else {
                    // 设置粒子效果图层
                    holder.getView(R.id.loading_progress).setVisibility(View.GONE);
                    particleLayout.setParticle(R.drawable.particle_star);
                    particleLayout.reset();
                    particleLayout.setListener(new XParticleLayout.Listener() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onEnd() {
                            final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                                    .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                            ConfirmDialog.newInstance(
                                    getResources().getString(R.string.info_delete_memory,
                                            TimeUtil.calculateAge(user.bornTime, item.happenStartTime),
                                            item.name),
                                    new DialogListener() {
                                        boolean confirm = false;

                                        @Override
                                        public void onDone(Object... result) {
                                            confirm = (boolean) result[0];
                                            if (confirm) {
                                                // 删除memory
                                                if(deleteMemory(item)) {
                                                } else {
                                                    Toast.makeText(getActivity(), R.string.error_loading,
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onDismiss() {
                                            if (!confirm) {
                                                particleLayout.reset();
                                            }
                                        }
                                    }).show(getFragmentManager(), ConfirmDialog.TAG);
                        }

                        @Override
                        public void onCancelled() {
                        }
                    });
                    XLog.d(TAG, "onBindViewHolder. position=" + position);
                    // 设置memory信息
                    holder.getView(R.id.timeline_node_txt, TextView.class).setText(item.name);
                    holder.getView(R.id.timeline_node_date, TextView.class).setText(
                            XStringUtil.calendar2str(TimeUtil.getCalendar(item.happenStartTime), "."));
                    ImageView memoryImage = (ImageView) holder.getView(R.id.timeline_node_img);
                    if (!XStringUtil.isEmpty(item.coverUrl)) {
                        Glide.with(TimelineFragment.this)
                                .load(item.coverUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.color.darkGray)
                                .error(R.color.darkGray)
                                .into(memoryImage);
                    } else {
                        memoryImage.setImageResource(R.color.darkGray);
                    }
                    final SwipeLayout swipeLayout = (SwipeLayout) holder.getView(R.id.timeline_node_swipe_layout);
                    if (swipeLayout.getOpenStatus() != SwipeLayout.Status.Close) {
                        XLog.d(TAG, "swipeLayout force close!!pos=" + position);
                        swipeLayout.close(false, false);
                    }
                    // 是自己的或已接收的回忆
                    if (item.inGift == null) {
                        holder.getView(R.id.timeline_node_lock).setVisibility(View.GONE);
                        holder.getView(R.id.timeline_node_unlock_layout).setVisibility(View.GONE);
                        if (!item.ownerId.equals(item.authorId)) {
                            // 是已接收回忆，显示已接收邮戳
                            holder.getView(R.id.timeline_node_stamp).setVisibility(View.VISIBLE);
                        } else {
                            holder.getView(R.id.timeline_node_stamp).setVisibility(View.GONE);
                        }
                        particleLayout.setEnable(true);
                        swipeLayout.setSwipeEnabled(false);
                        holder.getView(R.id.timeline_node_card_layout).setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        gotoDetail(item);
                                    }
                                });
                    }
                    // 是待接收的回忆
                    else {
                        holder.getView(R.id.timeline_node_lock).setVisibility(View.VISIBLE);
                        holder.getView(R.id.timeline_node_unlock_layout).setVisibility(View.VISIBLE);
                        holder.getView(R.id.timeline_node_stamp).setVisibility(View.GONE);
                        // 设置滑动图层
                        particleLayout.setEnable(false);
                        swipeLayout.setSwipeEnabled(true);
                        holder.getView(R.id.timeline_node_card_layout).setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // 显示问题
                                        swipeLayout.toggle(true);
                                    }
                                });
                        // 没有设锁的回忆
                        if (XStringUtil.isEmpty(item.inGift.question)) {
                            holder.getView(R.id.timeline_node_lock, ImageView.class)
                                    .setImageResource(R.drawable.icon_lock_open_black);
                            holder.getView(R.id.timeline_node_question).setVisibility(View.GONE);
                            holder.getView(R.id.timeline_node_answer_input_layout).setVisibility(View.GONE);
                            holder.getView(R.id.timeline_node_unlock_btn, Button.class).setText(R.string.button_take);
                            holder.getView(R.id.timeline_node_ignore_btn, Button.class).setText(R.string.button_reject);
                            holder.getView(R.id.timeline_node_unlock_btn)
                                    .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // 第一次点击进去表示解锁并接收Memory,通知服务器
                                            if (receiveMemory(item, item.inGift.gid, null)) {
                                                swipeLayout.toggle(true);
                                            } else {
                                                Toast.makeText(getActivity(), R.string.error_loading,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                            holder.getView(R.id.timeline_node_ignore_btn)
                                    .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // 拒绝此Memory
                                            if (rejectMemory(item, item.inGift.gid)) {
                                                swipeLayout.toggle(true);
                                            } else {
                                                Toast.makeText(getActivity(), R.string.error_loading,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                        // 有设锁的回忆
                        else {
                            holder.getView(R.id.timeline_node_lock, ImageView.class)
                                    .setImageResource(R.drawable.icon_lock_black);
                            holder.getView(R.id.timeline_node_question).setVisibility(View.VISIBLE);
                            holder.getView(R.id.timeline_node_question, TextView.class).setText(item.inGift.question);
                            holder.getView(R.id.timeline_node_answer_input_layout).setVisibility(View.VISIBLE);
                            final EditText answerInput = (EditText) holder.getView(R.id.timeline_node_answer_input);
                            answerInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                                @Override
                                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                                    if (id == EditorInfo.IME_ACTION_GO) {
                                        ViewUtil.hideInputMethod(getActivity());
                                        holder.getView(R.id.timeline_node_unlock_btn).performClick();
                                        return true;
                                    }
                                    return false;
                                }
                            });
                            holder.getView(R.id.timeline_node_unlock_btn, Button.class).setText(R.string.button_unlock);
                            holder.getView(R.id.timeline_node_ignore_btn, Button.class).setText(R.string.button_ignore);
                            holder.getView(R.id.timeline_node_unlock_btn)
                                    .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            String answer = answerInput.getText().toString().trim();
                                            if (XStringUtil.isEmpty(answer)) {
                                                answerInput.setError(getString(R.string.error_field_required));
                                                answerInput.requestFocus();
                                                return;
                                            }
                                            // 解锁并接收Memory
                                            if (receiveMemory(item, item.inGift.gid, answer)) {
                                                swipeLayout.toggle(true);
                                            } else {
                                                Toast.makeText(getActivity(), R.string.error_loading,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                            holder.getView(R.id.timeline_node_ignore_btn)
                                    .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // 拒绝此Memory
                                            swipeLayout.toggle(true);
                                            if (rejectMemory(item, item.inGift.gid)) {
                                                swipeLayout.toggle(true);
                                            } else {
                                                Toast.makeText(getActivity(), R.string.error_loading,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                    // 播放动画动画
                    if (mItemAnimations.containsKey(item)) {
                        XLog.d(TAG, "swipeLayout animate!!pos=" + position);
                        Pair<Animation, Integer> param = mItemAnimations.remove(item);
                        final Animation animation = param.first;
                        final View view = holder.getView(param.second);
                        view.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                XLog.d(TAG, "swipeLayout real animate!!pos=" + position);
                                view.startAnimation(animation);
                            }
                        }, 50);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size() + 2;
        }
    }
}
