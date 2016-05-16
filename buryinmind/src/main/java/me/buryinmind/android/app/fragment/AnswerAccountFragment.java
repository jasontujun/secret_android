package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONObject;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.model.MemoryGift;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/4/20.
 */
public class AnswerAccountFragment extends Fragment {

    public static final String KEY_GIFT = "gift";

    private FragmentInteractListener mListener;
    private MemoryGift mGift;

    private TextView mQuestionView;
    private EditText mAnswerInputView;
    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mListener = (FragmentInteractListener) getArguments().getSerializable(FragmentInteractListener.KEY);
            mGift = (MemoryGift) getArguments().getSerializable(KEY_GIFT);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_answer_question, container, false);
        ImageView friendHeadView = (ImageView) rootView.findViewById(R.id.friend_head_img);
        TextView friendNameView = (TextView) rootView.findViewById(R.id.friend_name_txt);
        ImageView accountHeadView = (ImageView) rootView.findViewById(R.id.account_head_img);
        TextView accountNameView = (TextView) rootView.findViewById(R.id.account_name_txt);
        TextView accountDesView = (TextView) rootView.findViewById(R.id.account_des_txt);
        mQuestionView = (TextView) rootView.findViewById(R.id.account_question_txt);
        mAnswerInputView = (EditText) rootView.findViewById(R.id.answer_input);
        Button checkAnswerBtn = (Button) rootView.findViewById(R.id.check_answer_btn);

        if (mGift != null) {
            Glide.with(getActivity())
                    .load(ApiUtil.getHeadUrl(mGift.senderId))
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.headicon_active)
                    .error(R.drawable.headicon_active)
                    .into(friendHeadView);
            friendNameView.setText(mGift.senderName);
            accountNameView.setText(mGift.receiverName);
            accountDesView.setText(XStringUtil.list2String(mGift.receiverDescription, " ,"));
            mQuestionView.setText(mGift.question);
        }
        // 设置键盘Enter键动作响应
        mAnswerInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.answer_ime || id == EditorInfo.IME_NULL) {
                    tryAnswerAccount();
                    return true;
                }
                return false;
            }
        });
        checkAnswerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryAnswerAccount();
            }
        });
        return rootView;
    }

    private void tryAnswerAccount() {
        if (mWaiting)
            return;
        mWaiting = true;
        // Reset errors.
        mAnswerInputView.setError(null);
        final String answer = mAnswerInputView.getText().toString();
        if (XStringUtil.isEmpty(answer)) {
            mAnswerInputView.setError(getString(R.string.error_field_required));
            mAnswerInputView.requestFocus();
            mWaiting = false;
            return;
        }
        ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(mAnswerInputView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        mAnswerInputView.clearFocus();
        if (mGift == null) {
            Toast.makeText(getActivity(), R.string.error_exception, Toast.LENGTH_SHORT).show();
            mWaiting = false;
            return;
        }
        if (mListener != null) {
            mListener.onLoading();
        }
        MyApplication.getAsyncHttp().execute(
                ApiUtil.answerActivateQuestion(mGift.receiverId, mGift.bid, answer),
                new XAsyncHttp.Listener() {
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
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object obj) {
                        mWaiting = false;
                        if (mListener != null) {
                            mListener.onFinish(true, answer);
                        }
                    }
                });
    }
}
