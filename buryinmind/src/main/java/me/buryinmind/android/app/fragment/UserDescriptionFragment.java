package me.buryinmind.android.app.fragment;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XHttpResponse;

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.adapter.DescriptionAdapter;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.uicontrol.XAutoGridLayoutManager;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/6/15.
 */
public class UserDescriptionFragment extends XFragment {

    private RecyclerView mDescriptionList;
    private DescriptionAdapter mDescriptionAdapter;
    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化data
        final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        mDescriptionAdapter = new DescriptionAdapter(getActivity(),
                new DescriptionAdapter.Listener() {
                    @Override
                    public void onAdd(String des) {}

                    @Override
                    public void onDelete(String des) {}

                    @Override
                    public void onSelect(int pos, String des) {}
                });
        mDescriptionAdapter.setData(user.descriptions);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_user_description, container, false);
        mDescriptionList = (RecyclerView) rootView.findViewById(R.id.account_des_list);
        // 初始化描述列表
        mDescriptionList.setLayoutManager(new XAutoGridLayoutManager(getActivity(), 2000));
        mDescriptionList.setAdapter(mDescriptionAdapter);
        return rootView;
    }

    @Override
    public boolean onBackHandle() {
        return mDescriptionAdapter.cancelSelected();
    }

    public void confirm() {
        ViewUtil.hideInputMethod(getActivity());
        List<String> des = mDescriptionAdapter.getData();
        if (des.size() < GlobalSource.DES_MIN_SIZE) {
            Toast.makeText(getActivity(), getResources().getString
                            (R.string.error_insufficient_des, GlobalSource.DES_MIN_SIZE),
                    Toast.LENGTH_SHORT).show();
            ViewUtil.animateShake(mDescriptionList);
            return;
        } else if (des.size() > GlobalSource.DES_MAX_SIZE){
            Toast.makeText(getActivity(), getResources().getString
                            (R.string.error_excessive_des, GlobalSource.DES_MAX_SIZE),
                    Toast.LENGTH_SHORT).show();
            ViewUtil.animateShake(mDescriptionList);
            return;
        }
        // 上传服务器
        updateDescription(mDescriptionAdapter.getData());
    }

    private void updateDescription(final List<String> descriptions) {
        if (mWaiting)
            return;
        mWaiting = true;
        notifyLoading(true);
        User newInfo = new User();
        newInfo.descriptions = descriptions;
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.updateUser(newInfo),
                new XAsyncHttp.Listener() {
                    @Override
                    public void onCancelled() {
                        mWaiting = false;
                        notifyLoading(false);
                    }

                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        notifyFinish(false, null);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        notifyFinish(false, null);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                        mWaiting = false;
                        User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                        user.descriptions = new ArrayList<String>(descriptions);
                        notifyFinish(true, descriptions);
                    }
                }));
    }
}
