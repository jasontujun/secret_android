package me.buryinmind.android.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;

import org.chenglei.widget.datepicker.DatePicker;

import java.util.Calendar;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.TimeUtil;

/**
 * Created by jasontujun on 2016/6/15.
 */
public class BirthdayFragment extends XFragment {

    private DatePicker mDatePicker;
    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthday, container, false);
        mDatePicker = (DatePicker) rootView.findViewById(R.id.birthday_picker);
        mDatePicker.setTextColor(getResources().getColor(R.color.darkGray))
                .setFlagTextColor(getResources().getColor(R.color.darkGray))
                .setTextSize(40)
                .setFlagTextSize(30)
                .setBackground(getResources().getColor(R.color.white))
                .setSoundEffectsEnabled(false);
        User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        if (user.bornTime != null) {
            // 用户设置过生日
            Calendar birth = TimeUtil.getCalendar(user.bornTime);
            mDatePicker.setDate(birth.getTime());
        } else {
            // 用户没设置过生日，显示当前时间
            mDatePicker.setDate(Calendar.getInstance().getTime());
        }
        return rootView;
    }

    public void confirm() {
        int year = mDatePicker.getYear();
        int month = mDatePicker.getMonth();
        int day = mDatePicker.getDayOfMonth();
        Calendar birthCal = Calendar.getInstance();
        birthCal.set(year, month - 1, day);
        // 统一改成GMT时区,再上传服务器
        final long bornTime = TimeUtil.changeTimeZoneToUTC(birthCal.getTimeInMillis());
        updateBornTime(bornTime);
    }


    private void updateBornTime(final long bornTime) {
        if (mWaiting)
            return;
        mWaiting = true;
        notifyLoading(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.updateBornTime(bornTime),
                new XAsyncHttp.Listener() {
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
                        user.bornTime = bornTime;
                        notifyFinish(true, bornTime);
                    }
                });
    }
}
