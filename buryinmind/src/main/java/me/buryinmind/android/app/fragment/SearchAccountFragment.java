package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/4/20.
 */
public class SearchAccountFragment extends Fragment {

    private FragmentInteractListener mListener;

    private AutoCompleteTextView mAccountInputView;
    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_account, container, false);
        mAccountInputView = (AutoCompleteTextView) rootView.findViewById(R.id.account_name_input);
        Button mLoginButton = (Button) rootView.findViewById(R.id.login_btn);
        Button mRegisterButton = (Button) rootView.findViewById(R.id.register_btn);
        // 设置输入姓名时的历史纪录提示
        GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        List<String> records = source.getUserNameRecords();
        if (records != null && records.size() > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, records);
            mAccountInputView.setAdapter(adapter);
        }
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryGetActiveAccountList(false);
            }
        });
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryGetActiveAccountList(true);
            }
        });
        return rootView;
    }

    public void setListener(FragmentInteractListener listener) {
        mListener = listener;
    }

    private void tryGetActiveAccountList(final boolean isRegister) {
        if (mWaiting)
            return;
        mWaiting = true;
        // Reset errors.
        mAccountInputView.setError(null);
        String name = mAccountInputView.getText().toString().trim();
        if (XStringUtil.isEmpty(name)) {
            mAccountInputView.setError(getString(R.string.error_field_required));
            mAccountInputView.requestFocus();
            mWaiting = false;
            return;
        }
        if (name.length() < GlobalSource.NAME_MIN_SIZE) {
            mAccountInputView.setError(getString(R.string.error_invalid_name));
            mAccountInputView.requestFocus();
            mWaiting = false;
            return;
        }
        ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(mAccountInputView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        mAccountInputView.clearFocus();
        if (mListener != null) {
            mListener.onLoading(true);
        }
        MyApplication.getAsyncHttp().execute(
                isRegister ? ApiUtil.searchSeedUser(name, null)
                        : ApiUtil.searchActiveUser(name, null),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                        if (mListener != null) {
                            mListener.onFinish(false, null);
                        }
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        if (mListener != null) {
                            mListener.onFinish(false, null);
                        }
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        mWaiting = false;
                        List<User> users = User.fromJson(jsonArray);
                        Pair<Boolean, List<User>> data = new Pair<Boolean, List<User>>(isRegister, users);
                        if (mListener != null) {
                            mListener.onFinish(true, data);
                        }
                    }
                });
    }
}
