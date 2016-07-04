package me.buryinmind.android.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XHttpResponse;

import org.json.JSONArray;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.dialog.ListDialog;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/7/1.
 */
public class WillFragment extends XFragment {

    private static final String TAG = WillFragment.class.getSimpleName();

    private static final long MONTH = 30 * 24 * 60 * 60 * 1000l;
    private static final long YEAR = 365 * 24 * 60 * 60 * 1000l;
    private static final long[] THRESHOLDS = {MONTH, 2 * MONTH, 3 * MONTH, 6 * MONTH,
            YEAR, 2 * YEAR, 5 * YEAR, 10 * YEAR};
    private static final int PROGRESS_UNIT = 10;
    private static final int PROGRESS_TOTAL = THRESHOLDS.length * PROGRESS_UNIT;

    private TextView mChoicePublic;
    private TextView mChoiceDestroy;
    private TextView mChoiceExtend;
    private TextView mThresholdMax;
    private TextView mThresholdMin;
    private TextView mThresholdCurrent;
    private SeekBar mSeekBar;

    private String mChooseHeritage;
    private long mChooseThreshold;
    private boolean mWaiting;
    private User mUser;
    private XListIdDataSourceImpl<User> mFriendSource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFriendSource = (XListIdDataSourceImpl<User>) XDefaultDataRepo
                .getInstance().getSource(MyApplication.SOURCE_FRIEND);
        mUser = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_will, container, false);
        mChoicePublic = (TextView) rootView.findViewById(R.id.will_choice_public);
        mChoiceDestroy = (TextView) rootView.findViewById(R.id.will_choice_destroy);
        mChoiceExtend = (TextView) rootView.findViewById(R.id.will_choice_extend);
        mThresholdMax = (TextView) rootView.findViewById(R.id.death_check_max);
        mThresholdMin = (TextView) rootView.findViewById(R.id.death_check_min);
        mThresholdCurrent = (TextView) rootView.findViewById(R.id.death_check_current);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.death_check);
        mChooseHeritage = mUser.heritage;
        refreshHeritage();

        // 设置遗嘱策略
        mChoicePublic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChooseHeritage = User.HERITAGE_PUBLIC;
                refreshHeritage();
            }
        });
        mChoiceDestroy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChooseHeritage = User.HERITAGE_DESTROY;
                refreshHeritage();
            }
        });
        mChoiceExtend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWaiting) {
                    Toast.makeText(getActivity(), R.string.error_loading, Toast.LENGTH_SHORT).show();
                    return;
                }
                ListDialog.newInstance(getResources().getString(R.string.info_choose_heritage_people),
                        new DialogListener() {
                            @Override
                            public void onDone(Object... result) {
                                User friend = (User) result[0];
                                if (friend != null) {
                                    mChooseHeritage = friend.uid;
                                    refreshHeritage();
                                }
                            }

                            @Override
                            public void onDismiss() {}
                        }).show(getFragmentManager(), ListDialog.TAG);
            }
        });
        // 设置检测阀值
        mChooseThreshold = mUser.threshold;
        mThresholdMin.setText(getResources().getString(R.string.info_death_threshold_month, 1));
        mThresholdMax.setText(getResources().getString(R.string.info_death_threshold_year, 10));
        mSeekBar.setEnabled(true);
        mSeekBar.setMax(PROGRESS_TOTAL);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                XLog.d(TAG, "onProgressChanged().progress=" + progress + ",fromUser=" + fromUser);
                mThresholdCurrent.setText(getThresholdString(calValueByProgress(progress)));
                float moveStep = (float) mSeekBar.getWidth() / (float) PROGRESS_TOTAL;
                mThresholdCurrent.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int width = mThresholdCurrent.getMeasuredWidth();
                int height = mThresholdCurrent.getMeasuredHeight();
                int left = 0;
                int right = 0;
                if ((progress * moveStep) - width / 2 < 0) {
                    left = 0;
                    right = left + width;
                } else if ((progress * moveStep) + width / 2 > mSeekBar.getWidth()) {
                    right = mSeekBar.getWidth();
                    left = right - width;
                } else {
                    left = (int) (progress * moveStep) - width / 2;
                    right = left + width;
                }
                mThresholdCurrent.layout(left, 0, right, height);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 让界面绘制完成后，再设置进度
        rootView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSeekBar.setProgress(calProgressByValue(mChooseThreshold));
            }
        }, 50);
        return rootView;
    }

    private long calValueByProgress(int progress) {
        int index = progress / PROGRESS_UNIT;
        if (index == THRESHOLDS.length) {
            index = THRESHOLDS.length - 1;
        }
        mChooseThreshold = THRESHOLDS[index];
        return mChooseThreshold;
    }

    private int calProgressByValue(long threshold) {
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (threshold <= THRESHOLDS[i]) {
                return i * PROGRESS_UNIT;
            }
        }
        return PROGRESS_TOTAL;
    }

    private String getThresholdString(long threshold) {
        for (long t : THRESHOLDS) {
            if (threshold <= t) {
                if (t < 12 * MONTH) {
                    return getResources().getString(R.string.info_death_threshold_month, t / MONTH);
                } else {
                    return getResources().getString(R.string.info_death_threshold_year, t / 12 / MONTH);
                }
            }
        }
        return "??";
    }

    private void refreshHeritage() {
        mChoiceDestroy.setBackgroundResource(R.color.transparentGray);
        mChoicePublic.setBackgroundResource(R.color.transparentGray);
        mChoiceExtend.setBackgroundResource(R.color.transparentGray);
        mChoiceExtend.setText(getResources().getString(R.string.button_will_extend, ".."));
        if (User.HERITAGE_DESTROY.equals(mChooseHeritage)) {
            mChoiceDestroy.setBackgroundResource(R.color.darkGray);
        } else if (User.HERITAGE_PUBLIC.equals(mChooseHeritage)) {
            mChoicePublic.setBackgroundResource(R.color.darkGray);
        } else {
            mChoiceExtend.setBackgroundResource(R.color.darkGray);
            User friend = mFriendSource.getById(mChooseHeritage);
            if (friend != null) {
                mChoiceExtend.setText(getResources().getString
                        (R.string.button_will_extend, "\"" + friend.name + "\""));
            } else {
                // 如果friend数据不存在，请求服务器
                requestFriends(new Runnable() {
                    @Override
                    public void run() {
                        User friend = mFriendSource.getById(mChooseHeritage);
                        if (friend != null) {
                            mChoiceExtend.setText(getResources().getString
                                    (R.string.button_will_extend, "\"" + friend.name + "\""));
                        }
                    }
                });
            }
        }
    }


    private void requestFriends(final Runnable listener) {
        if (mWaiting) {
            return;
        }
        mWaiting = true;
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.getFriendList(),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onCancelled() {
                        mWaiting = false;
                    }

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
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray jsonArray) {
                        mWaiting = false;
                        List<User> users = User.fromJson(jsonArray);
                        if (users != null && users.size() > 0) {
                            for (User user : users) {
                                user.isFriend = true;
                            }
                            mFriendSource.addAll(users);
                        }
                        if (listener != null) {
                            listener.run();
                        }
                    }
                }));
    }


    public void confirm() {
        if (mWaiting) {
            Toast.makeText(getActivity(), R.string.error_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        mWaiting = true;
        notifyLoading(true);
        User newInfo = new User();
        newInfo.heritage = mChooseHeritage;
        newInfo.threshold = mChooseThreshold;
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
                        user.heritage = mChooseHeritage;
                        user.threshold = mChooseThreshold;
                        notifyFinish(true, user);
                    }
                }));
    }
}
