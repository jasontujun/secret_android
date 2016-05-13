package me.buryinmind.android.app.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import me.buryinmind.android.app.R;

/**
 * Created by jasontujun on 2016/5/13.
 */
public class ConfirmDialog extends DialogFragment {

    public static final String TAG = "ConfirmDialog";
    private String mTxt;
    private DialogListener mListener;

    public static ConfirmDialog newInstance(String txt, DialogListener listener) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.mTxt = txt;
        dialog.mListener = listener;
        return dialog;
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
        View rootView = inflater.inflate(R.layout.dialog_confirm, container);
        TextView confirmTxt = (TextView) rootView.findViewById(R.id.dialog_confirm_txt);
        Button yesBtn = (Button) rootView.findViewById(R.id.dialog_confirm_yes_btn);
        Button noBtn = (Button) rootView.findViewById(R.id.dialog_confirm_no_btn);
        confirmTxt.setText(mTxt);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onDone(true);
                }
                dismiss();
            }
        });
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onDone(false);
                }
                dismiss();
            }
        });
        return rootView;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onDismiss();
        }
    }
}
