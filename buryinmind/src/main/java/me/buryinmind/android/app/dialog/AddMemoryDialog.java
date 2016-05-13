package me.buryinmind.android.app.dialog;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.utils.XStringUtil;

import java.util.Calendar;

import me.buryinmind.android.app.MainActivity;
import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.TimeUtil;

/**
 * Created by jasontujun on 2016/5/10.
 */
public class AddMemoryDialog extends DialogFragment {

    public static final String TAG = "AddMemoryDialog";
    public static final String KEY_HAPPEN = "happen";
    public static final String KEY_NAME = "name";

    private Calendar mHappenDate;
    private String mName;
    private DialogListener mListener;
    private boolean showDateDialog;

    private DateDialog mDateDialog;
    private DateDialog.DateListener mDateDialogListener;
    private EditText mNameInputView;

    public static AddMemoryDialog newInstance(DialogListener listener) {
        AddMemoryDialog dialog = new AddMemoryDialog();
        dialog.mListener = listener;
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mName = getArguments().getString(KEY_NAME);
            mHappenDate = (Calendar) getArguments().getSerializable(KEY_HAPPEN);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_input, container);
        TextView ageDesView = (TextView) rootView.findViewById(R.id.dialog_input_txt);
        Button doneBtn = (Button) rootView.findViewById(R.id.dialog_input_btn);
        mNameInputView = (EditText) rootView.findViewById(R.id.dialog_input_edit);

        mNameInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.memory_ime || id == EditorInfo.IME_NULL) {
                    clickBtn();
                    return true;
                }
                return false;
            }
        });
        GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        final User user = source.getUser();
        final Calendar nowCal = Calendar.getInstance();
        final Calendar birthCal = TimeUtil.getCalendar(user.bornTime);
        mDateDialogListener = new DateDialog.DateListener() {
            private FragmentManager manager = getFragmentManager();
            private boolean hasSet = false;

            @Override
            public void onDismiss() {
                showDateDialog = false;
                if (!hasSet) {
                    AddMemoryDialog.this.show(manager, TAG);
                }
            }

            @Override
            public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                hasSet = true;
                Calendar targetCal = Calendar.getInstance();
                targetCal.set(year, month, day);

                // 检查所选日期是否合法
                if (targetCal.before(birthCal) || targetCal.after(Calendar.getInstance())) {
                    // 选择日期早于出生日期，或晚于当前日期，重选！
                    Toast.makeText(datePickerDialog.getActivity(), R.string.error_invalid_date, Toast.LENGTH_SHORT).show();
                } else {
                    Bundle arguments = new Bundle();
                    arguments.putSerializable(KEY_HAPPEN, targetCal);
                    AddMemoryDialog.this.setArguments(arguments);
                }
                AddMemoryDialog.this.show(datePickerDialog.getFragmentManager(), TAG);
            }
        };
        mDateDialog = DateDialog.newInstance(mDateDialogListener,
                nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH),
                nowCal.get(Calendar.DAY_OF_MONTH), false);
        mDateDialog.setYearRange(birthCal.get(Calendar.YEAR), Calendar.getInstance().get(Calendar.YEAR));
        if (mHappenDate != null) {
            doneBtn.setText(R.string.button_done);
            String info = String.format(getResources().getString(R.string.info_add_memory_date),
                    TimeUtil.calculateAge(user.bornTime, mHappenDate.getTimeInMillis()));
            ageDesView.setText(info);
            ageDesView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDateDialog = true;
                    mDateDialog.initialize(mDateDialogListener,
                            mHappenDate.get(Calendar.YEAR), mHappenDate.get(Calendar.MONTH),
                            mHappenDate.get(Calendar.DAY_OF_MONTH), false);
                    mDateDialog.show(getFragmentManager(), MainActivity.TAG_DATE_PICKER);
                    dismiss();
                }
            });
        } else {
            doneBtn.setText(R.string.button_set_date);
            ageDesView.setText(R.string.info_add_memory);
        }
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickBtn();
            }
        });
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (mHappenDate != null) {
            bundle.putString(KEY_NAME, mName);
            bundle.putSerializable(KEY_HAPPEN, mHappenDate);
        }
    }

    private void clickBtn() {
        if (mHappenDate != null) {
            final String name = mNameInputView.getText().toString();
            if (XStringUtil.isEmpty(name)) {
                mNameInputView.setError(getString(R.string.error_field_required));
                mNameInputView.requestFocus();
                return;
            }
            mName = name;
            if (mListener != null) {
                mListener.onDone(mName, mHappenDate);
            }
        } else {
            showDateDialog = true;
            mDateDialog.show(getFragmentManager(), MainActivity.TAG_DATE_PICKER);
        }
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!showDateDialog) {
            if (mListener != null) {
                mListener.onDismiss();
            }
        }
    }
}
