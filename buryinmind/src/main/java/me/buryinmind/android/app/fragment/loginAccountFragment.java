package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/4/20.
 */
public class LoginAccountFragment extends Fragment {

    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_DESCRIPTION = "user_description";

    private FragmentInteractListener mListener;
    private String userId;
    private String userName;
    private List<String> userDescription;

    private EditText mPasswordInputView;
    private boolean mWaiting;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mListener = (FragmentInteractListener) getArguments().getSerializable(FragmentInteractListener.KEY);
            userId = getArguments().getString(KEY_USER_ID);
            userName = getArguments().getString(KEY_USER_NAME);
            userDescription = (List<String>) getArguments().getSerializable(KEY_USER_DESCRIPTION);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login_account, container, false);
        ImageView accountHeadView = (ImageView) rootView.findViewById(R.id.account_head_img);
        TextView accountNameView = (TextView) rootView.findViewById(R.id.account_name_txt);
        TextView accountDesView = (TextView) rootView.findViewById(R.id.account_des_txt);
        mPasswordInputView = (EditText) rootView.findViewById(R.id.password_input);
        Button loginButton = (Button) rootView.findViewById(R.id.login_btn);

        accountHeadView.setImageResource(R.drawable.headicon_active);
        if (!XStringUtil.isEmpty(userName)) {
            accountNameView.setText(userName);
        }
        if (userDescription != null) {
            accountDesView.setText(XStringUtil.list2String(userDescription, " ,"));
        }
        // 设置键盘Enter键动作响应
        mPasswordInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login_ime || id == EditorInfo.IME_NULL) {
                    tryLogin();
                    return true;
                }
                return false;
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryLogin();
            }
        });
        return rootView;
    }

    private void tryLogin() {
        if (mWaiting)
            return;
        mWaiting = true;
        // Reset errors.
        mPasswordInputView.setError(null);
        final String password = mPasswordInputView.getText().toString();
        if (XStringUtil.isEmpty(password)) {
            mPasswordInputView.setError(getString(R.string.error_field_required));
            mPasswordInputView.requestFocus();
            return;
        }
        if (password.length() < GlobalSource.PASSWORD_MIN_SIZE) {
            mPasswordInputView.setError(getString(R.string.error_invalid_password));
            mPasswordInputView.requestFocus();
            return;
        }
        if (XStringUtil.isEmpty(userId)) {
            Toast.makeText(getActivity(), R.string.error_exception, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mListener != null) {
            mListener.onLoading();
        }
        MyApplication.getAsyncHttp().execute(
                ApiUtil.loginUser(userId, password),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
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
                        Toast.makeText(getActivity(), R.string.error_login_fail, Toast.LENGTH_SHORT).show();
                        if (mListener != null) {
                            mListener.onFinish(false, null);
                        }
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject obj) {
                        mWaiting = false;
                        String token = null;
                        User user = null;
                        try {
                            if (obj != null) {
                                token = obj.getString("token");
                                user = User.fromJson(obj.getJSONObject("user"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (XStringUtil.isEmpty(token) || user == null) {
                            Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                            mListener.onFinish(false, null);
                        } else {
                            GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
                            source.loginSuccess(user, token);
                            if (mListener != null) {
                                mListener.onFinish(true, user);
                            }
                        }
                    }
                });
    }
}
