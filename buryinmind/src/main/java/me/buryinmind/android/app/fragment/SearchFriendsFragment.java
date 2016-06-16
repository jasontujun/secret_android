package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListFilteredIdSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.toolkit.filter.XBaseFilter;
import com.tj.xengine.core.toolkit.filter.XFilter;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.adapter.DescriptionAdapter;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.adapter.XViewHolder;
import me.buryinmind.android.app.uicontrol.XAutoGridLayoutManager;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/6/5.
 */
public class SearchFriendsFragment extends Fragment {
    private static final String TAG = SearchFriendsFragment.class.getSimpleName();

    private FragmentInteractListener mListener;

    private View mProgressView;
    private EditText mNameInputView;
    private RecyclerView mDescriptionList;
    private RecyclerView mFriendList;

    private DescriptionAdapter mDescriptionAdapter;
    private UserAdapter mFriendAdapter;
    private XListFilteredIdSourceImpl<User> mFriendSource;
    private XFilter<User> mFriendFilter;
    private User mNewUser;

    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNewUser = new User();
        mFriendSource = (XListFilteredIdSourceImpl<User>) XDefaultDataRepo
                .getInstance().getSource(MyApplication.SOURCE_FRIEND);
        mFriendFilter = new XBaseFilter<User>() {
            @Override
            public User doFilter(User user) {
                if (!XStringUtil.isEmpty(mNewUser.name) &&
                        !user.name.startsWith(mNewUser.name)) {
                    return null;
                }
                if (mNewUser.descriptions != null
                        && user.descriptions != null) {
                    for (String inputDes : mNewUser.descriptions) {
                        boolean ok = false;
                        for (String userDes : user.descriptions) {
                            if (userDes.contains(inputDes)) {
                                ok = true;
                                break;
                            }
                        }
                        if (!ok) {
                            return null;
                        }
                    }
                }
                return user;
            }
        };
        mFriendSource.setFilter(mFriendFilter);
        mFriendAdapter = new UserAdapter(mFriendSource);
        mDescriptionAdapter = new DescriptionAdapter(getActivity(),
                new DescriptionAdapter.Listener() {
                    @Override
                    public void onAdd(String des) {
                        refreshFriendsList();
                    }

                    @Override
                    public void onDelete(String des) {
                        refreshFriendsList();
                    }

                    @Override
                    public void onSelect(int pos, String des) {}
                });
        // 请求最新的好友列表
        // 在onCreate时请求数据，可以避免fragment切换时过于频繁请求
        requestFriends();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_frends, container, false);
        mProgressView = rootView.findViewById(R.id.loading_progress);
        mNameInputView = (EditText) rootView.findViewById(R.id.receiver_name_input);
        mDescriptionList = (RecyclerView) rootView.findViewById(R.id.receiver_des_list);
        mFriendList = (RecyclerView) rootView.findViewById(R.id.friend_list);

        // 初始化朋友列表
        mFriendList.setAdapter(mFriendAdapter);
        // 初始化描述列表
        mDescriptionList.setLayoutManager(new XAutoGridLayoutManager(getActivity(), 2000));
        mDescriptionList.setAdapter(mDescriptionAdapter);
        // 初始化名字输入框
        mNameInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                XLog.d(TAG, "beforeTextChanged()");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                XLog.d(TAG, "onTextChanged().s=" + s);
                final String name = s.toString().trim();
                mNewUser.name = name;
                if (XStringUtil.isEmpty(name)) {
                    mFriendAdapter.setNewUser(null);
                    ViewUtil.animateFadeInOut(mDescriptionList, true);
                } else {
                    mFriendAdapter.setNewUser(mNewUser);
                    ViewUtil.animateFadeInOut(mDescriptionList, false);
                }
                // 刷新用户列表
                refreshFriendsList();
            }

            @Override
            public void afterTextChanged(Editable s) {
                XLog.d(TAG, "beforeTextChanged().s=" + s);
            }
        });
        mNameInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == EditorInfo.IME_ACTION_SEARCH) {
                    XLog.d(TAG, "mNameInputView键入enter键");
                    ViewUtil.hideInputMethod(getActivity());
                    // TODO 联网搜索相关名字的用户
                    return true;
                }
                return false;
            }
        });
        if (XStringUtil.isEmpty(mNameInputView.getText().toString())) {
            mDescriptionList.setAlpha(0f);
        } else {
            mDescriptionList.setAlpha(1f);
        }
        return rootView;
    }

    public void setListener(FragmentInteractListener listener) {
        mListener = listener;
    }

    private void showProgress(boolean show) {
        ViewUtil.animateFadeInOut(mFriendList, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private void requestFriends() {
        if (mWaiting)
            return;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getFriendList(),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        mWaiting = false;
                        showProgress(false);
                        List<User> users = User.fromJson(jsonArray);
                        if (users != null && users.size() > 0) {
                            for (User user : users) {
                                user.isFriend = true;
                            }
                            mFriendSource.addAll(users);
                            mFriendSource.sortOrigin(User.comparator);
                            mFriendAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void refreshFriendsList() {
        mNewUser.descriptions = mDescriptionAdapter.getData();
        mFriendSource.doFilter();
        mFriendAdapter.notifyDataSetChanged();
    }

    private class UserAdapter extends RecyclerView.Adapter<XViewHolder> {

        private static final int TYPE_NEWER = 1;
        private static final int TYPE_FRIEND = 2;

        private User newUser;
        private XListFilteredIdSourceImpl<User> mSource;

        public UserAdapter(XListFilteredIdSourceImpl<User> source) {
            mSource = source;
        }

        public void setNewUser(User user) {
            newUser = user;
        }

        @Override
        public int getItemViewType(int position) {
            if (newUser != null && position == 0)
                return TYPE_NEWER;
            else
                return TYPE_FRIEND;
        }

        @Override
        public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resId;
            if (viewType == TYPE_NEWER)
                resId = R.layout.item_new_user;
            else
                resId = R.layout.item_account;
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(resId, parent, false);
            return new XViewHolder(view);
        }

        @Override
        public void onBindViewHolder(XViewHolder holder, int position) {
            int type = getItemViewType(position);
            if (type == TYPE_NEWER) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 创建新用户，检查des数量
                        if (newUser.descriptions == null
                                || newUser.descriptions.size() < 3) {
                            Toast.makeText(getActivity(), R.string.error_insufficient_des,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 新增一个用户
                        if (mListener != null) {
                            ViewUtil.hideInputMethod(getActivity());
                            mListener.onFinish(true, newUser);
                        }
                    }
                });
            } else {
                final User user = mSource.get(newUser != null ? position - 1 : position);
                holder.getView(R.id.account_name_txt, TextView.class).setText(user.name);
                holder.getView(R.id.account_des_txt, TextView.class)
                        .setText(XStringUtil.list2String(user.descriptions, ", "));
                if (user.isFriend) {
                    holder.getView(R.id.account_favorite).setVisibility(View.VISIBLE);
                } else {
                    holder.getView(R.id.account_favorite).setVisibility(View.GONE);
                }
                Glide.with(getActivity())
                        .load(ApiUtil.getIdUrl(user.uid))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.headicon_default)
                        .error(R.drawable.headicon_default)
                        .into(holder.getView(R.id.account_head_img, ImageView.class));

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 选择一个用户
                        if (mListener != null) {
                            ViewUtil.hideInputMethod(getActivity());
                            mListener.onFinish(true, user);
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mFriendSource.size() + (newUser == null ? 0 : 1);
        }
    }

}
