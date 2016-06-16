package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import java.util.TimeZone;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.activity.MemoryDetailActivity;
import me.buryinmind.android.app.activity.TimelineActivity;
import me.buryinmind.android.app.adapter.XViewHolder;
import me.buryinmind.android.app.controller.ResultListener;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.dialog.AddMemoryDialog;
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
public class TimelineFragment extends Fragment {

    private static final String TAG = "BIM_MainActivity";

    private FragmentInteractListener mListener;

    private XListIdDataSourceImpl<Memory> mMemorySource;
    private RecyclerView mTimelineView;
    private TimelineAdapter mAdapter;
    private View mAddBtn;

    private boolean mWaiting;
    private boolean mListTop = true;
    private boolean mAddingMemory;

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
        mAddBtn = rootView.findViewById(R.id.timeline_add_btn);

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
                        ((TimelineActivity) getActivity()).expandToolBar();
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

        // init add btn
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mAddingMemory) {
                    mAddingMemory = true;
                    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_start);
                    animation.setFillAfter(true);
                    mAddBtn.startAnimation(animation);
                    AddMemoryDialog.newInstance(new DialogListener() {
                        @Override
                        public void onDone(Object... result) {
                            String name = (String) result[0];
                            Calendar date = (Calendar) result[1];
                            // 统一改成GMT时区,再上传服务器
                            date.setTimeZone(TimeZone.getTimeZone("GMT"));
                            addMemory(name, date.getTimeInMillis());
                        }

                        @Override
                        public void onDismiss() {
                            mAddingMemory = false;
                            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_back);
                            animation.setFillAfter(true);
                            mAddBtn.startAnimation(animation);
                        }
                    }).show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), AddMemoryDialog.TAG);
                }
            }
        });

        return rootView;
    }

    public void setListener(FragmentInteractListener listener) {
        mListener = listener;
    }

    private void requestMemoryList() {
        if (mWaiting)
            return;
        mWaiting = true;
        if (mListener != null) {
            mListener.onLoading(true);
        }
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryList(),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        if (mListener != null) {
                            mListener.onLoading(false);
                        }
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        if (mListener != null) {
                            mListener.onLoading(false);
                        }
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        mWaiting = false;
                        if (mListener != null) {
                            mListener.onLoading(false);
                        }
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

    private void addMemory(String memoryName, long happenTime) {
        if (mWaiting)
            return;
        mWaiting = true;
        MyApplication.getAsyncHttp().execute(
                ApiUtil.addMemory(memoryName, happenTime),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jsonObject) {
                        mWaiting = false;
                        Memory memory = Memory.fromJson(jsonObject);
                        if (memory != null) {
                            mMemorySource.add(memory);
                            mMemorySource.sort(Memory.comparator);
                        }
                    }
                }
        );
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
                        XLog.d(TAG, "deleteMemory onNetworkError()! mid=" + memory.mid);
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        mAdapter.resetData(memory);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        XLog.d(TAG, "deleteMemory onFinishError()! mid=" + memory.mid);
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        mAdapter.resetData(memory);
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

    private void receiveMemory(final Memory memory, String gid, String answer,
                               final ResultListener<Memory> listener) {
        MyApplication.getAsyncHttp().execute(
                ApiUtil.receiveMemory(gid, answer),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onResult(false, null);
                        }
                    }
                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onResult(false, null);
                        }
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
                            mAdapter.resetData(memory);
                            if (listener != null) {
                                listener.onResult(true, memory);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private class TimelineAdapter extends RecyclerView.Adapter<XViewHolder> {

        private static final int TYPE_HEADER = 1;
        private static final int TYPE_FOOTER = 2;
        private static final int TYPE_NODE = 3;

        private static final String KEY_LISTENER = "swipeLayoutListener";

        private Map<Integer, Memory> mAges;
        private List<Memory> mItems;
        private List<Memory> mToBeDelete;

        public TimelineAdapter(List<Memory> items) {
            mAges = new HashMap<Integer, Memory>();
            mToBeDelete = new ArrayList<Memory>();
            setData(items);
        }

        public void setData(final List<Memory> memories) {
            mItems = memories;
            mToBeDelete.clear();
            mAges.clear();
            for (Memory memory : memories) {
                if (!mAges.containsKey(memory.age)) {
                    mAges.put(memory.age, memory);
                }
            }
            notifyDataSetChanged();
        }

        public void resetData(Memory memory) {
            mToBeDelete.remove(memory);
            int pos = mItems.indexOf(memory);
            if (pos == -1) {
                return;
            }
            notifyItemChanged(pos + 1);
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
            mToBeDelete.remove(memory);
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
                                Calendar lastBornTime = TimeUtil.getCalendar(user.bornTime);
                                // TODO 跳转到生日设置的fragment
                            }
                        });
                holder.getView(R.id.timeline_header_time, TextView.class)
                        .setText(XStringUtil.calendar2str(TimeUtil.getCalendar(user.bornTime), "."));
            } else if (type == TYPE_FOOTER) {
                holder.getView(R.id.timeline_footer_txt).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mAddBtn.performClick();
                            }
                        });
            } else {
                final Memory item = mItems.get(position - 1);
                if (item.age > 0 && mAges.get(item.age).equals(item)) {
                    holder.getView(R.id.timeline_tag, TextView.class)
                            .setText(String.valueOf(item.age));
                    holder.getView(R.id.timeline_tag_layout).setVisibility(View.VISIBLE);
                }else {
                    holder.getView(R.id.timeline_tag_layout).setVisibility(View.GONE);
                }
                // 如果不是自己的回忆,显示赠送者头像
                if (!user.uid.equals(item.authorId)) {
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
                    holder.getView(R.id.timeline_sender_head).setVisibility(View.GONE);
                    holder.getView(R.id.timeline_sender_head).setAnimation(null);
                }
                final XParticleLayout particleLayout = (XParticleLayout) holder.getView(R.id.timeline_node_layout);
                if (mToBeDelete.contains(item)) {
                    // 正在删除的条目，不显示内容，也不能点击
                    particleLayout.hide();
                    particleLayout.setOnClickListener(null);
                } else {
                    holder.getView(R.id.timeline_node_txt, TextView.class).setText(item.name);
                    holder.getView(R.id.timeline_node_date, TextView.class).setText(
                            XStringUtil.calendar2str(TimeUtil.getCalendar(item.happenTime), "."));
                    ImageView memoryImage = (ImageView) holder.getView(R.id.timeline_node_img);
                    if (!XStringUtil.isEmpty(item.coverUrl)) {
                        memoryImage.setVisibility(View.VISIBLE);
                        Glide.with(TimelineFragment.this)
                                .load(item.coverUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.color.darkGray)
                                .error(R.color.darkGray)
                                .into(memoryImage);
                    } else {
                        memoryImage.setVisibility(View.GONE);
                    }
                    // 设置粒子效果图层
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
                                            TimeUtil.calculateAge(user.bornTime, item.happenTime),
                                            item.name),
                                    new DialogListener() {
                                        boolean confirm = false;

                                        @Override
                                        public void onDone(Object... result) {
                                            confirm = (boolean) result[0];
                                            if (confirm) {
                                                mToBeDelete.add(item);
                                                particleLayout.setOnClickListener(null);
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
                    final SwipeLayout swipeLayout = (SwipeLayout) holder.getView(R.id.timeline_node_swipe_layout);
                    // 是自己的或已接收的回忆
                    if (item.inGift == null) {
                        holder.getView(R.id.timeline_sender_name).setVisibility(View.GONE);
                        holder.getView(R.id.timeline_node_lock).setVisibility(View.GONE);
                        holder.getView(R.id.timeline_node_unlock_layout).setVisibility(View.GONE);
                        swipeLayout.setSwipeEnabled(false);
                        swipeLayout.removeSwipeListener(holder.getTag(KEY_LISTENER, MySwipeListener.class));
                        holder.getView(R.id.timeline_node_card_layout).setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(getActivity(), MemoryDetailActivity.class);
                                        intent.putExtra("mid", item.mid);
                                        startActivity(intent);
                                    }
                                });
                    }
                    // 是未接收的回忆
                    else {
                        holder.getView(R.id.timeline_sender_name).setVisibility(View.VISIBLE);
                        holder.getView(R.id.timeline_sender_name, TextView.class).setText(
                                String.format(getResources().getString(R.string.info_memory_sender),
                                        item.inGift.senderName));
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
                                            // 第一次点击进去表示解锁并接收Memory,通知服务器
                                            receiveMemory(item, item.inGift.gid, null,
                                                    new ResultListener<Memory>() {
                                                        @Override
                                                        public void onResult(boolean result, Memory data) {
                                                            if (result) {
                                                                Intent intent = new Intent(getActivity(),
                                                                        MemoryDetailActivity.class);
                                                                intent.putExtra("mid", data.mid);
                                                                startActivity(intent);
                                                            }
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
                                    // 解锁并接收Memory
                                    receiveMemory(item, item.inGift.gid, answer, null);
                                }
                            });
                            holder.getView(R.id.timeline_node_ignore_btn).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // TODO 忽略此Memory
                                    swipeLayout.toggle(true);
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
