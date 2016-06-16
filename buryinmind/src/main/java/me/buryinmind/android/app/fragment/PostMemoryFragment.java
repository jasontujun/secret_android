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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/6/7.
 */
public class PostMemoryFragment extends Fragment {

    public static final String TAG = PostMemoryFragment.class.getSimpleName();

    public static final String KEY_RECEIVER = "receiver";
    public static final String KEY_MID = "mid";

    private FragmentInteractListener mListener;
    private User mReceiver;
    private Memory mMemory;
    private String mQuestion;
    private String mAnswer;

    private View mProgressView;
    private SwipeLayout mSwipeLayout;
    private EditText mQuestionInputView;
    private EditText mAnswerInputView;
    private ImageButton mLockBtn;
    private Button mPostBtn;

    private boolean mWaiting;
    private boolean mQuestionShow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        Bundle argument = getArguments();
        if (argument != null) {
            mReceiver = (User) argument.getSerializable(KEY_RECEIVER);
            String mid = argument.getString(KEY_MID);
            XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                    XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
            mMemory = source.getById(mid);
            mQuestionShow = XStringUtil.isEmpty(mReceiver.uid);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        XLog.d(TAG, "onCreateView()");
        View rootView = inflater.inflate(R.layout.fragment_post_memory, container, false);
        mProgressView = rootView.findViewById(R.id.loading_progress);
        mQuestionInputView = (EditText) rootView.findViewById(R.id.question_input);
        mAnswerInputView = (EditText) rootView.findViewById(R.id.answer_input);
        mLockBtn = (ImageButton) rootView.findViewById(R.id.lock_btn);
        mPostBtn = (Button) rootView.findViewById(R.id.post_btn);
        ImageView accountHeadView = (ImageView) rootView.findViewById(R.id.account_head_img);
        TextView accountNameView = (TextView) rootView.findViewById(R.id.account_name_txt);
        TextView accountDesView = (TextView) rootView.findViewById(R.id.account_des_txt);

        accountNameView.setText(mReceiver.name);
        if (mReceiver.descriptions != null) {
            accountDesView.setText(XStringUtil.list2String(mReceiver.descriptions, " ,"));
        }
        if (!XStringUtil.isEmpty(mReceiver.uid)) {
            Glide.with(getActivity())
                    .load(ApiUtil.getIdUrl(mReceiver.uid))
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.headicon_active)
                    .error(R.drawable.headicon_active)
                    .into(accountHeadView);
        }
        // 设置问答输入框的监听
        mQuestionInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_NEXT) {
                    mQuestion = mQuestionInputView.getText().toString().trim();
                    mAnswerInputView.requestFocus();
                    return true;
                }
                return false;
            }
        });
        mAnswerInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_GO) {
                    ViewUtil.hideInputMethod(getActivity());
                    mLockBtn.performClick();
                    return true;
                }
                return false;
            }
        });
        // 问题加锁
        mSwipeLayout = (SwipeLayout)rootView.findViewById(R.id.memory_swipe_layout);
        mSwipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
        mSwipeLayout.addOnLayoutListener(new SwipeLayout.OnLayout() {
            @Override
            public void onLayout(SwipeLayout v) {
                XLog.d(TAG, "SwipeLayout onLayout()");
                if (mQuestionShow) {
                    mSwipeLayout.open(false, false);
                } else {
                    mSwipeLayout.close(false, false);
                }
            }
        });
        mSwipeLayout.addSwipeListener(new SimpleSwipeListener() {
            @Override
            public void onOpen(SwipeLayout layout) {
                XLog.d(TAG, "SwipeLayout onOpen()");
                mQuestionShow = true;
            }
            @Override
            public void onClose(SwipeLayout layout) {
                XLog.d(TAG, "SwipeLayout onClose()");
                mQuestionShow = false;
            }
        });
        // 设置按钮
        mLockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuestion = mQuestionInputView.getText().toString().trim();
                mAnswer = mAnswerInputView.getText().toString().trim();
                mSwipeLayout.toggle(true);
                lockerTip();
            }
        });
        mPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuestion = mQuestionInputView.getText().toString().trim();
                mAnswer = mAnswerInputView.getText().toString().trim();
                if (XStringUtil.isEmpty(mReceiver.uid) &&
                        (XStringUtil.isEmpty(mQuestion) || XStringUtil.isEmpty(mAnswer))) {
                    Toast.makeText(getActivity(), R.string.error_invalid_name, Toast.LENGTH_SHORT).show();
                    return;
                }
                postMemory();
            }
        });
        // 如果是新增用户,提示加锁
        lockerTip();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        XLog.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onStart() {
        XLog.d(TAG, "onStart()");
        super.onStart();
        if (mQuestionShow) {
            mSwipeLayout.open(false, false);
        } else {
            mSwipeLayout.close(false, false);
        }
    }

    @Override
    public void onResume() {
        XLog.d(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onPause() {
        XLog.d(TAG, "onResume()");
        super.onPause();
    }

    @Override
    public void onStop() {
        XLog.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        XLog.d(TAG, "onDestroyView()");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        XLog.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    public void setListener(FragmentInteractListener listener) {
        mListener = listener;
    }

    private void showProgress(boolean show) {
        ViewUtil.animateFadeInOut(mSwipeLayout, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private void lockerTip() {
        if (XStringUtil.isEmpty(mReceiver.uid) &&
                (XStringUtil.isEmpty(mQuestion) || XStringUtil.isEmpty(mAnswer))) {
            ViewUtil.animateShine(mLockBtn);
        } else {
            mLockBtn.setAnimation(null);
        }
    }

    private void postMemory() {
        if (mWaiting)
            return;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.postMemory(mMemory.mid, mReceiver.uid,
                        mReceiver.name, mReceiver.descriptions,
                        mQuestion, mAnswer, 0),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
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
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jo) {
                        mWaiting = false;
                        showProgress(false);
                        String gid = null;
                        try {
                            gid = jo.getString("gid");
                            XLog.d(TAG, "寄送memory成功!gid=" + gid);
                            mMemory.editable = false;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (mListener != null) {
                            mListener.onFinish(true, gid);
                        }
                    }
                });
    }
}
