package me.buryinmind.android.app.controller;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;

import com.tj.xengine.android.download.XHttpDownloadTask;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.download.XBaseHttpDownloader;
import com.tj.xengine.core.download.XDownloadBean;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgr;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialMgrImpl;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/5/24.
 */
public class SecretImageDownloader {

    private static class SecretDownloadBean extends XDownloadBean {
        public Secret secret;

        public SecretDownloadBean(Secret secret) {
            super();
            this.secret = secret;
        }

        @Override
        public String getId() {
            return secret.sid;
        }
    }

    private static final String TAG = SecretImageDownloader.class.getSimpleName();
    private static final int MSG_FINISH = 1;
    private static final int MSG_ERROR = 2;

    private XBaseHttpDownloader mDownloader;
    private String mSaveFolder;
    private Map<String, Secret> mSecrets;
    private Map<String, ResultListener<Secret>> mListener;

    public SecretImageDownloader(XHttp httpClient, String folder) {
        mSecrets = new HashMap<String, Secret>();
        mListener = new HashMap<String, ResultListener<Secret>>();
        mSaveFolder = folder;
        mDownloader = new SecretImageDownloadMgr(httpClient);
        mDownloader.registerListener(new InnerListener(
                new Handler(Looper.getMainLooper(),
                        new Handler.Callback() {
                            @Override
                            public boolean handleMessage(Message msg) {
                                String sid = (String) msg.obj;
                                Secret secret = mSecrets.get(sid);
                                ResultListener<Secret> listener = mListener.get(sid);
                                switch (msg.what) {
                                    case MSG_FINISH:
                                        mSecrets.remove(sid);
                                        mListener.remove(sid);
                                        listener.onResult(true, secret);
                                        break;
                                    case MSG_ERROR:
                                        mSecrets.remove(sid);
                                        mListener.remove(sid);
                                        listener.onResult(false, secret);
                                        break;
                                }
                                return true;
                            }
                        })));
    }

    @MainThread
    public void download(Secret secret, ResultListener<Secret> listener) {
        if (!XStringUtil.isEmpty(secret.localPath)) {
            File file = new File(secret.localPath);
            if (file.exists()) {
                // Secret文件已下载，直接回调
                if (listener != null) {
                    listener.onResult(true, secret);
                }
                return;
            }
        }
        SecretDownloadBean bean = new SecretDownloadBean(secret);
        bean.setFolder(mSaveFolder);
        if (Secret.MIME_JPEG.equals(secret.mime)) {
            bean.setFileName(secret.sid + ".jpg");
        } else if (Secret.MIME_PNG.equals(secret.mime)) {
            bean.setFileName(secret.sid + ".png");
        } else {
            bean.setFileName(secret.sid);
        }
        if (mDownloader.addTask(bean)) {
            mSecrets.put(secret.sid, secret);
            mListener.put(secret.sid, listener);
            mDownloader.startDownload();
        }
    }

    public void pause() {
        mDownloader.pauseDownload();
    }

    public void resume() {
        mDownloader.resumeDownload();
    }


    private static class InnerListener implements XBaseHttpDownloader.Listener {

        Handler handler;

        public InnerListener(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onStart(String id) {}

        @Override
        public void onStop(String id) {}

        @Override
        public void onStopAll() {}

        @Override
        public void onDownloading(String id, long completeSize, long totalSize) {}

        @Override
        public void onSpeedUpdate(String id, long speed) {}

        @Override
        public void onComplete(String id, File file) {
            if (file != null) {
                file.deleteOnExit();
            }
            Message msg = handler.obtainMessage(MSG_FINISH);
            msg.obj = id;
            msg.sendToTarget();
        }

        @Override
        public void onError(String id, String errorCode, File file) {
            if (file != null) {
                file.deleteOnExit();
            }
            Message msg = handler.obtainMessage(MSG_ERROR);
            msg.obj = id;
            msg.sendToTarget();
        }

        @Override
        public void onFinishAll() {}
    }


    private static class SecretImageDownloadMgr extends XBaseHttpDownloader {

        public SecretImageDownloadMgr(XHttp http) {
            super(http);
        }

        @Override
        protected XTaskMgr<XMgrTaskExecutor<XDownloadBean>, XDownloadBean> createTaskMgr() {
            // 不添加速度计算器SpeedCalculator，因为不关心下载速度，节省一个速度计算线程
            return new XSerialMgrImpl<XDownloadBean>();
        }

        @Override
        protected XMgrTaskExecutor<XDownloadBean> createTask(XDownloadBean bean) {
            return new SecretImageDownloadTask(bean, mHttpClient);
        }
    }


    private static class SecretImageDownloadTask extends XHttpDownloadTask {

        private XJsonObjectHandler httpHandler;

        public SecretImageDownloadTask(XDownloadBean bean, XHttp httpClient) {
            super(bean, httpClient);
            httpHandler = new XJsonObjectHandler();
        }

        public SecretImageDownloadTask(XDownloadBean bean, int status, XHttp httpClient) {
            super(bean, status, httpClient);
            httpHandler = new XJsonObjectHandler();
        }

        @Override
        protected boolean backToDownloadMgr() {
            return false;
        }

        @Override
        protected String requestRealUrl(XDownloadBean bean) {
            if (!(bean instanceof SecretDownloadBean)) {
                return null;
            }
            GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance()
                    .getSource(MyApplication.SOURCE_GLOBAL);
            final User user = source.getUser();
            if (user == null) {
                return null;
            }
            SecretDownloadBean secretBean = (SecretDownloadBean) bean;
            XHttpResponse response = mHttpClient.execute(ApiUtil.getSecretDownloadUrl
                    (user.uid, secretBean.secret.mid, secretBean.secret.sid));
            if (response == null) {
                XLog.d(TAG, "获取secret下载地址返回的response为空");
                return null;
            }
            if (response.getStatusCode() < 200 && response.getStatusCode() >= 300) {
                XLog.d(TAG, "获取secret下载地址错误，返回码：" + response.getStatusCode());
                return null;
            }
            JSONObject jo = httpHandler.handleResponse(response);
            if (jo == null) {
                XLog.d(TAG, "获取secret下载地址返回的response的内容为空");
                return null;
            }
            String url = null;
            try {
                url = jo.getString("url");
                XLog.d(TAG, "获取secret下载地址成功，url=" + url);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return url;
        }
    }

}
