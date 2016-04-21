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

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.MemoryGift;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/4/20.
 */
public class ActivateAccountFragment extends Fragment {

    public static final String KEY_GIFT = "gift";
    public static final String KEY_ANSWER = "answer";

    private FragmentInteractListener mListener;
    private MemoryGift mGift;
    private String mAnswer;

    private EditText mEmailInputView;
    private EditText mPasswordInputView;
    private EditText mPasswordConfirmInputView;
    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mListener = (FragmentInteractListener) getArguments().getSerializable(FragmentInteractListener.KEY);
            mGift = (MemoryGift) getArguments().getSerializable(KEY_GIFT);
            mAnswer = getArguments().getString(KEY_ANSWER);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_activate_account, container, false);
        ImageView accountHeadView = (ImageView) rootView.findViewById(R.id.account_head_img);
        TextView accountNameView = (TextView) rootView.findViewById(R.id.account_name_txt);
        TextView accountDesView = (TextView) rootView.findViewById(R.id.account_des_txt);
        mEmailInputView = (EditText) rootView.findViewById(R.id.email_input);
        mPasswordInputView = (EditText) rootView.findViewById(R.id.password_input);
        mPasswordConfirmInputView = (EditText) rootView.findViewById(R.id.password_confirm_input);
        Button activateBtn = (Button) rootView.findViewById(R.id.activate_btn);

        if (mGift != null) {
            accountNameView.setText(mGift.receiverName);
            accountDesView.setText(XStringUtil.list2String(mGift.receiverDescription, " ,"));
        }
        // 设置键盘Enter键动作响应
        mPasswordConfirmInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.activate_ime || id == EditorInfo.IME_NULL) {
                    tryActivateAccount();
                    return true;
                }
                return false;
            }
        });
        activateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryActivateAccount();
            }
        });
        return rootView;
    }

    private void tryActivateAccount() {
        if (mWaiting)
            return;
        mWaiting = true;
        // Reset errors.
        mEmailInputView.setError(null);
        final String email = mEmailInputView.getText().toString();
        if (XStringUtil.isEmpty(email) && !XStringUtil.isEmail(email)) {
            mEmailInputView.setError(getString(R.string.error_invalid_email));
            mEmailInputView.requestFocus();
            return;
        }
        mPasswordInputView.setError(null);
        final String password1 = mPasswordInputView.getText().toString();
        if (XStringUtil.isEmpty(password1)) {
            mPasswordInputView.setError(getString(R.string.error_field_required));
            mPasswordInputView.requestFocus();
            return;
        }
        if (password1.length() < GlobalSource.PASSWORD_MIN_SIZE) {
            mPasswordInputView.setError(getString(R.string.error_invalid_password));
            mPasswordInputView.requestFocus();
            return;
        }
        mPasswordConfirmInputView.setError(null);
        final String password2 = mPasswordConfirmInputView.getText().toString();
        if (XStringUtil.isEmpty(password2)) {
            mPasswordConfirmInputView.setError(getString(R.string.error_field_required));
            mPasswordConfirmInputView.requestFocus();
            return;
        }
        if (password2.length() < GlobalSource.PASSWORD_MIN_SIZE) {
            mPasswordConfirmInputView.setError(getString(R.string.error_invalid_password));
            mPasswordConfirmInputView.requestFocus();
            return;
        }
        if (!password1.equals(password2)) {
            mPasswordConfirmInputView.setError(getString(R.string.error_inconsistent_password));
            mPasswordConfirmInputView.requestFocus();
            return;
        }
        if (mGift == null || XStringUtil.isEmpty(mAnswer)) {
            Toast.makeText(getActivity(), R.string.error_exception, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mListener != null) {
            mListener.onLoading();
        }
        MyApplication.getAsyncHttp().execute(
                ApiUtil.activateUser(mGift.receiverId, mGift.bid, password2, email, mAnswer),
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
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
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
                            // 如果返回的json结果中没有token或user，则代表激活成功但自动登录失败，需要再手动登录
                            mListener.onFinish(true, null);
                        } else {
                            // 激活并自动登录成功
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
