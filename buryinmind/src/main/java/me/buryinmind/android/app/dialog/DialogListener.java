package me.buryinmind.android.app.dialog;

/**
 * Created by jasontujun on 2016/5/11.
 */
public interface DialogListener {

    void onDone(Object... result);

    void onDismiss();
}
