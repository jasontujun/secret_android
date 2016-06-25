package me.buryinmind.android.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.tj.xengine.android.data.listener.XHandlerIdDataSourceListener;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
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
        mMemorySource.registerListener(new XHandlerIdDataSourceListener<Memory>() {
            @Override
            public void onReplaceInUI(List<Memory> list, List<Memory> list1) {
            }

            @Override
            public void onChangeInUI() {
            }

            @Override
            public void onAddInUI(Memory memory) {
                int pos = mMemorySource.getIndexById(memory.mid);
                mAdapter.addData(pos, memory);
                mTimelineView.scrollToPosition(pos + 1);
            }


            @Override
            public void onAddAllInUI(List<Memory> list) {
                mAdapter.setData(mMemorySource.copyAll());
            }

            @Override
            public void onDeleteInUI(Memory memory) {
                mAdapter.deleteData(memory);
            }

            @Override
            public void onDeleteAllInUI(List<Memory> list) {
                mAdapter.setData(mMemorySource.copyAll());
            }
        });
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

    private void requestMemoryList() {
        if (mWaiting)
            return;
        mWaiting = true;
        notifyLoading(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryList(),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
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
                        }
                        // 获取待接收列表
                        getMemoryGift();
                    }
                });
    }

    private void deleteMemory(final Memory memory) {
        if (mWaiting)
            return;
        mWaiting = true;
        MyApplication.getAsyncHttp().execute(
                ApiUtil.deleteMemory(memory.mid),
                new XAsyncHttp.Listener() {
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
                    }
                }
        );
    }

    private void getMemoryGift() {
        MyApplication.getAsyncHttp().execute(
                ApiUtil.inMemory(),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
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
                        List<Memory> inMemories = new ArrayList<Memory>();
                        if (paMemories != null && paMemories.size() > 0) {
                            inMemories.addAll(paMemories);
                        }
                        if (pbMemories != null && pbMemories.size() > 0) {
                            inMemories.addAll(pbMemories);
                        }
                        if (inMemories.size() > 0) {
                            mMemorySource.addAll(inMemories);
                            mMemorySource.sort(Memory.comparator);
                        }
                    }
                });
    }

    private void receiveMemory(final Memory memory, String gid, String answer, final Runnable postExecutor) {
        MyApplication.getAsyncHttp().execute(
                ApiUtil.receiveMemory(gid, answer),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }
                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }
                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jo) {
                        try {
                            memory.mid = jo.getString("mid");
                            memory.inGift = null;
                            final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                                    .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                            memory.ownerId = user.uid;
                            memory.ownerName = user.name;
                            mAdapter.removeLoadingItem(memory);
                            if (postExecutor != null) {
                                postExecutor.run();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void rejectMemory(final Memory memory, String gid) {
        MyApplication.getAsyncHttp().execute(
                ApiUtil.rejectMemory(gid),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onNetworkError() {
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mAdapter.removeLoadingItem(memory);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                        mAdapter.removeLoadingItem(memory);
                        mMemorySource.deleteById(memory.mid);
                    }
                });
    }

    public boolean isScrollToTop() {
        return mListTop;
    }

    public void needScrollTo(final int pos) {
        if (!mListTop) {
            mTimelineView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTimelineView.smoothScrollToPosition(pos);
                }
            }, 50);
        }
    }

    public void needScrollToTop() {
        if (!mListTop) {
            mTimelineView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTimelineView.smoothScrollToPosition(0);
                }
            }, 50);
        }
    }


    private class TimelineAdapter extends RecyclerView.Adapter<XViewHolder> {

        private static final int TYPE_HEADER = 1;
        private static final int TYPE_FOOTER = 2;
        private static final int TYPE_NODE = 3;

        private static final String KEY_LISTENER = "swipeLayoutListener";

        private Map<Integer, Memory> mAges;
        private List<Memory> mItems;
        private List<Memory> mLoadingItems;

        public TimelineAdapter(List<Memory> items) {
            mAges = new HashMap<Integer, Memory>();
            mLoadingItems = new ArrayList<Memory>();
            setData(items);
        }

        public void setData(final List<Memory> memories) {
            mItems = memories;
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
                            String.format(getResources().getString(R.string.info_memory_sender),
                                    item.authorName));
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
                                    String.format(getResources().getString(R.string.info_delete_memory),
                                            TimeUtil.calculateAge(user.bornTime, item.happenStartTime),
                                            item.name),
                                    new DialogListener() {
                                        boolean confirm = false;

                                        @Override
                                        public void onDone(Object... result) {
                                            confirm = (boolean) result[0];
                                            if (confirm) {
                                                addLoadingItem(item);
                                                // 删除memory
                                                deleteMemory(item);
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
                    // 是自己的或已接收的回忆
                    final SwipeLayout swipeLayout = (SwipeLayout) holder.getView(R.id.timeline_node_swipe_layout);
                    swipeLayout.setVisibility(View.VISIBLE);
                    if (item.inGift == null) {
                        holder.getView(R.id.timeline_node_lock).setVisibility(View.GONE);
                        holder.getView(R.id.timeline_node_unlock_layout).setVisibility(View.GONE);
                        swipeLayout.setSwipeEnabled(false);
                        swipeLayout.removeSwipeListener(holder.getTag(KEY_LISTENER, MySwipeListener.class));
                        holder.getView(R.id.timeline_node_card_layout).setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(getActivity(), MemoryGiftActivity.class);
                                        intent.putExtra("mid", item.mid);
                                        startActivity(intent);
                                    }
                                });
                    }
                    // 是未接收的回忆
                    else {
                        // 没有设锁的回忆
                        if (XStringUtil.isEmpty(item.inGift.question)) {
                            holder.getView(R.id.timeline_node_lock).setVisibility(View.GONE);
                            holder.getView(R.id.timeline_node_unlock_layout).setVisibility(View.GONE);
                            swipeLayout.setSwipeEnabled(false);
                            swipeLayout.removeSwipeListener(holder.getTag(KEY_LISTENER, MySwipeListener.class));
                            holder.getView(R.id.timeline_node_card_layout).setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            addLoadingItem(item);
                                            // 第一次点击进去表示解锁并接收Memory,通知服务器
                                            receiveMemory(item, item.inGift.gid, null,
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Intent intent = new Intent(getActivity(),
                                                                    MemoryDetailActivity.class);
                                                            intent.putExtra("mid", item.mid);
                                                            startActivity(intent);
                                                        }
                                                    });
                                        }
                                    });
                        }
                        // 有设锁的回忆
                        else {
                            holder.getView(R.id.timeline_node_lock).setVisibility(View.VISIBLE);
                            holder.getView(R.id.timeline_node_unlock_layout).setVisibility(View.VISIBLE);
                            swipeLayout.setSwipeEnabled(true);
                            // 设置滑动图层
                            MySwipeListener sListener = holder.getTag(KEY_LISTENER, MySwipeListener.class);
                            if (sListener == null) {
                                sListener = new MySwipeListener(particleLayout);
                            } else {
                                sListener.setParticleLayout(particleLayout);
                            }
                            swipeLayout.addSwipeListener(sListener);
                            holder.getView(R.id.timeline_node_card_layout).setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // 显示问题
                                            swipeLayout.toggle(true);
                                        }
                                    });
                            final EditText answerInput = (EditText) holder.getView(R.id.timeline_node_answer_input);
                            holder.getView(R.id.timeline_node_question, TextView.class).setText(item.inGift.question);
                            holder.getView(R.id.timeline_node_unlock_btn).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String answer = answerInput.getText().toString().trim();
                                    if (XStringUtil.isEmpty(answer)) {
                                        answerInput.setError(getString(R.string.error_field_required));
                                        answerInput.requestFocus();
                                        return;
                                    }
                                    swipeLayout.toggle(true);
                                    addLoadingItem(item);
                                    // 解锁并接收Memory
                                    receiveMemory(item, item.inGift.gid, answer, null);
                                }
                            });
                            holder.getView(R.id.timeline_node_ignore_btn).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // 拒绝此Memory
                                    swipeLayout.toggle(true);
                                    addLoadingItem(item);
                                    rejectMemory(item, item.inGift.gid);
                                }
                            });
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
                        }
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size() + 2;
        }
    }


    private class MySwipeListener extends SimpleSwipeListener {
        XParticleLayout pLayout;

        public MySwipeListener(XParticleLayout pLayout) {
            this.pLayout = pLayout;
        }

        public void setParticleLayout(XParticleLayout pLayout) {
            this.pLayout = pLayout;
        }

        @Override
        public void onOpen(SwipeLayout layout) {
            XLog.d(TAG, "滑开图层，禁止粒子效果");
            if (pLayout != null) {
                pLayout.setEnable(false);
            }
        }

        @Override
        public void onClose(SwipeLayout layout) {
            XLog.d(TAG, "隐藏图层，恢复粒子效果");
            if (pLayout != null) {
                pLayout.setEnable(true);
            }
        }
    };
}
