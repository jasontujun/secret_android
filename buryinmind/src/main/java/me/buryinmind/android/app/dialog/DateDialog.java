package me.buryinmind.android.app.dialog;

import android.content.DialogInterface;

import com.fourmob.datetimepicker.date.DatePickerDialog;

/**
 * Created by jasontujun on 2016/5/11.
 */
public class DateDialog extends DatePickerDialog {

    public interface DateListener extends OnDateSetListener {
        void onDismiss();
    }

    private DateListener mListener;

    public DateDialog() {
        // Empty constructor required for dialog fragment. DO NOT REMOVE
        super();
    }

    public static DateDialog newInstance(DateListener listener, int year, int month, int day) {
        return newInstance(listener, year, month, day, true);
    }

    public static DateDialog newInstance(DateListener listener, int year, int month, int day, boolean vibrate) {
        DateDialog datePickerDialog = new DateDialog();
        datePickerDialog.mListener = listener;
        datePickerDialog.initialize(listener, year, month, day, vibrate);
        return datePickerDialog;
    }

    public void setListener(DateListener listener) {
        super.setOnDateSetListener(listener);
        mListener = listener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onDismiss();
        }
    }
}
