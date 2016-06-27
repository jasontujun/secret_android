package me.buryinmind.android.app.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;

import com.qiniu.android.storage.UploadManager;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgr;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgrListener;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialMgrImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.model.Secret;

/**
 * Created by jasontujun on 2016/5/24.
 */
public class SecretImageUploader {

    private static final int MSG_FINISH = 1;
    private static final int MSG_ERROR = 2;
    private static final int MSG_PROGRESS = 3;

    private XHttp mHttpClient;
    private XTaskMgr<XMgrTaskExecutor<SecretUploadBean>, SecretUploadBean> mTaskMgr;
    private List<ProgressListener<Secret>> mListener;
    private File mUploadDir;// 用于加密解密的临时文件夹
    private UploadManager mQiniuUploadMgr;

    public SecretImageUploader(Context context, XHttp httpClient, UploadManager qiniuUploadMgr) {
        mHttpClient = httpClient;
        mQiniuUploadMgr = qiniuUploadMgr;
        mListener = new ArrayList<ProgressListener<Secret>>();
        mUploadDir = MyApplication.getCacheDirectory();;
        mTaskMgr = new XSerialMgrImpl<SecretUploadBean>();
        mTaskMgr.registerListener(new InnerListener(
                new Handler(Looper.getMainLooper(),
                        new Handler.Callback() {
                            @Override
                            public boolean handleMessage(Message msg) {
                                Secret secret = (Secret) msg.obj;
                                    switch (msg.what) {
                                        case MSG_PROGRESS:
                                            for (ProgressListener<Secret> listener : mListener) {
                                                listener.onProgress(secret, secret.completeSize, secret.size);
                                            }
                                            break;
                                        case MSG_FINISH:
                                            secret.completeSize = -1;
                                            for (ProgressListener<Secret> listener : mListener) {
                                                listener.onResult(true, secret);
                                            }
                                            break;
                                        case MSG_ERROR:
                                            for (ProgressListener<Secret> listener : mListener) {
                                                listener.onResult(false, secret);
                                            }
                                            break;
                                    }
                                return true;
                            }
                        })));
    }


    protected XMgrTaskExecutor<SecretUploadBean> createTask(SecretUploadBean bean) {
        return new SecretImageUploadTask(bean, mHttpClient, mUploadDir, mQiniuUploadMgr);
    }

    @MainThread
    public void registerListener(ProgressListener<Secret> listener) {
        if (!mListener.contains(listener))
            mListener.add(listener);
    }

    @MainThread
    public void unregisterListener(ProgressListener<Secret> listener) {
        mListener.remove(listener);
    }

    @MainThread
    public void upload(Secret secret) {
        if (!secret.needUpload) {
            return;
        }

        SecretUploadBean bean = new SecretUploadBean(secret);
        if (mTaskMgr.addTask(createTask(bean))) {
            mTaskMgr.start();
        }
    }

    public void pause() {
        mTaskMgr.pause();
    }

    public void resume() {
        mTaskMgr.resume();
    }


    private static class InnerListener implements XTaskMgrListener<SecretUploadBean> {

        Handler handler;

        public InnerListener(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onAdd(SecretUploadBean bean) {

        }

        @Override
        public void onAddAll(List<SecretUploadBean> list) {

        }

        @Override
        public void onRemove(SecretUploadBean bean) {

        }

        @Override
        public void onRemoveAll(List<SecretUploadBean> list) {

        }

        @Override
        public void onStart(SecretUploadBean bean) {

        }

        @Override
        public void onStop(SecretUploadBean bean) {

        }

        @Override
        public void onStopAll() {}

        @Override
        public void onComplete(SecretUploadBean bean) {
            Message msg = handler.obtainMessage(MSG_FINISH);
            msg.obj = bean.secret;
            msg.sendToTarget();
        }

        @Override
        public void onError(SecretUploadBean bean, String errorCode) {
            Message msg = handler.obtainMessage(MSG_ERROR);
            msg.obj = bean.secret;
            msg.sendToTarget();
        }

        @Override
        public void onFinishAll() {}

        @Override
        public void onDoing(SecretUploadBean bean, long completeSize) {
            Message msg = handler.obtainMessage(MSG_PROGRESS);
            msg.obj = bean.secret;
            bean.secret.completeSize = completeSize;
            msg.sendToTarget();
        }

        @Override
        public void onSpeedUpdate(SecretUploadBean bean, long l) {

        }
    }
}
