package me.buryinmind.android.app.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.adapter.XListAdapter;
import me.buryinmind.android.app.adapter.XViewHolder;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/7/3.
 */
public class ListDialog extends DialogFragment {
    public static final String TAG = ListDialog.class.getSimpleName();

    private RecyclerView mList;
    private TextView mNoDataTxt;
    private ProgressBar mProgressView;

    private String mTxt;
    private DialogListener mListener;
    private XListAdapter<User> mAdapter;
    private XListIdDataSourceImpl<User> mFriendSource;
    private AsyncTask mTask;

    public static ListDialog newInstance(String txt, DialogListener listener) {
        ListDialog dialog = new ListDialog();
        dialog.mTxt = txt;
        dialog.mListener = listener;
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mFriendSource = (XListIdDataSourceImpl<User>) XDefaultDataRepo
                .getInstance().getSource(MyApplication.SOURCE_FRIEND);
        mAdapter = new XListAdapter<User>(R.layout.item_account) {
            @Override
            public void onBindViewHolder(XViewHolder holder, int position) {
                final User item = getData().get(position);
                holder.getView(R.id.account_name_txt, TextView.class).setText(item.name);
                holder.getView(R.id.account_des_txt, TextView.class)
                        .setText(XStringUtil.list2String(item.descriptions, ", "));
                Glide.with(getActivity())
                        .load(ApiUtil.getIdUrl(item.uid))
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.headicon_default)
                        .error(R.drawable.headicon_default)
                        .into(holder.getView(R.id.account_head_img, ImageView.class));

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.onDone(item);
                        }
                        dismiss();
                    }
                });
            }
        };
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_list, container);
        TextView msgTxt = (TextView) rootView.findViewById(R.id.dialog_txt);
        mList = (RecyclerView) rootView.findViewById(R.id.dialog_list_view);
        mNoDataTxt = (TextView) rootView.findViewById(R.id.dialog_list_no_data);
        mProgressView = (ProgressBar) rootView.findViewById(R.id.loading_progress);
        msgTxt.setText(mTxt);
        mList.setAdapter(mAdapter);
        requestFriends();
        return rootView;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
        if (mListener != null) {
            mListener.onDismiss();
        }
    }

    private void showProgress(boolean show) {
        ViewUtil.animateFadeInOut(mList, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private void requestFriends() {
        if (mTask != null)
            return;
        showProgress(true);
        mTask = MyApplication.getAsyncHttp().execute(
                ApiUtil.getFriendList(),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onCancelled() {
                        mTask = null;
                        showProgress(false);
                    }

                    @Override
                    public void onNetworkError() {
                        mTask = null;
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mTask = null;
                        showProgress(false);
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        mTask = null;
                        showProgress(false);
                        List<User> users = User.fromJson(jsonArray);
                        if (users != null && users.size() > 0) {
                            for (User user : users) {
                                user.isFriend = true;
                            }
                            mFriendSource.addAll(users);
                            if (mFriendSource.size() == 0) {
                                mNoDataTxt.setVisibility(View.VISIBLE);
                                mList.setVisibility(View.GONE);
                            } else {
                                mNoDataTxt.setVisibility(View.GONE);
                                mList.setVisibility(View.VISIBLE);
                                mAdapter.setData(mFriendSource.copyAll());
                            }
                        }
                    }
                });
    }
}
