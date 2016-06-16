package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tj.xengine.core.data.XDefaultDataRepo;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.adapter.DescriptionAdapter;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.uicontrol.XAutoGridLayoutManager;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/6/15.
 */
public class EditDescriptionFragment extends Fragment {

    private FragmentInteractListener mListener;

    private View mDesLayout;
    private RecyclerView mDescriptionList;
    private DescriptionAdapter mDescriptionAdapter;

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
        View rootView = inflater.inflate(R.layout.fragment_edit_description , container, false);
        mDesLayout = rootView.findViewById(R.id.account_des_layout);
        mDescriptionList = (RecyclerView) rootView.findViewById(R.id.account_des_list);
        // 初始化描述列表
        mDescriptionList.setLayoutManager(new XAutoGridLayoutManager(getActivity(), 2000));
        mDescriptionList.setAdapter(mDescriptionAdapter);
        // 点击背景，则退出该fragment
        mDesLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewUtil.hideInputMethod(getActivity());
                List<String> des = mDescriptionAdapter.getData();
                if (des.size() < 3) {
                    Toast.makeText(getActivity(), R.string.error_insufficient_des, Toast.LENGTH_SHORT).show();
                    if (mListener != null) {
                        mListener.onFinish(false, null);
                    }
                } else if (des.size() > 7){
                    Toast.makeText(getActivity(), R.string.error_excessive_des, Toast.LENGTH_SHORT).show();
                    if (mListener != null) {
                        mListener.onFinish(false, null);
                    }
                } else {
                    // TODO 上传服务器
                    if (mListener != null) {
                        mListener.onFinish(true, mDescriptionAdapter.getData());
                    }
                }
            }
        });
        return rootView;
    }

    public void setListener(FragmentInteractListener listener) {
        mListener = listener;
    }
}
