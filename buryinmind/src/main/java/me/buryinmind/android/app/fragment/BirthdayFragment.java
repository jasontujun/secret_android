package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;

import java.util.Calendar;
import java.util.TimeZone;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/6/15.
 */
public class BirthdayFragment extends Fragment {

    public static final String TAG_DATE_PICKER = "datepicker";

    private FragmentInteractListener mListener;
    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthday, container, false);
        Button birthdayBtn = (Button) rootView.findViewById(R.id.timeline_birthday_btn);
        birthdayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(Calendar.getInstance(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                        Calendar birthCal = Calendar.getInstance();
                        birthCal.set(year, month, day);
                        // 统一改成GMT时区,再上传服务器
                        birthCal.setTimeZone(TimeZone.getTimeZone("GMT"));
                        final long bornTime = birthCal.getTimeInMillis();
                        updateBornTime(bornTime);
                    }
                });
            }
        });
        return rootView;
    }

    private DialogFragment showDatePicker(Calendar initCal, DatePickerDialog.OnDateSetListener listener) {
        if (initCal == null) {
            initCal = Calendar.getInstance();
        }
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(listener,
                initCal.get(Calendar.YEAR), initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH), false);
        datePickerDialog.setYearRange(1902, Calendar.getInstance().get(Calendar.YEAR));
        datePickerDialog.show(((AppCompatActivity)getActivity()).getSupportFragmentManager(), TAG_DATE_PICKER);
        return datePickerDialog;
    }


    private void updateBornTime(final long bornTime) {
        if (mWaiting)
            return;
        mWaiting = true;
        if (mListener != null) {
            mListener.onLoading(true);
        }
        MyApplication.getAsyncHttp().execute(
                ApiUtil.updateBornTime(bornTime),
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
                    public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                        mWaiting = false;
                        User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                        user.bornTime = bornTime;
                        // TODO back
                        if (mListener != null) {
                            mListener.onFinish(true, bornTime);
                        }
                    }
                });
    }

    public void setListener(FragmentInteractListener listener) {
        mListener = listener;
    }
}
