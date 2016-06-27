package me.buryinmind.android.app.controller;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;

import com.tj.xengine.android.network.download.XHttpDownloadTask;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.android.utils.XStorageUtil;
import com.tj.xengine.core.network.download.XBaseHttpDownloader;
import com.tj.xengine.core.network.download.XDownloadBean;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgr;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialMgrImpl;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.CryptoUtil;

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
    private static final int MSG_PROGRESS = 3;

    private XBaseHttpDownloader mDownloader;
    private String mSaveFolder;// 下载的文件夹
    private Map<String, Secret> mSecrets;
    private List<ProgressListener<Secret>> mListener;

    public SecretImageDownloader(XHttp httpClient, String folder) {
        mSecrets = new HashMap<String, Secret>();
        mListener = new ArrayList<ProgressListener<Secret>>();
        mSaveFolder = folder;
        mDownloader = new SecretImageDownloadMgr(httpClient);
        mDownloader.registerListener(new InnerListener(
                new Handler(Looper.getMainLooper(),
                        new Handler.Callback() {
                            @Override
                            public boolean handleMessage(Message msg) {
                                String sid = (String) msg.obj;
                                Secret secret = mSecrets.get(sid);
                                switch (msg.what) {
                                    case MSG_PROGRESS:
                                        secret.completeSize = msg.arg1;
                                        for (ProgressListener<Secret> listener : mListener) {
                                            listener.onProgress(secret, secret.completeSize, secret.size);
                                        }
                                        break;
                                    case MSG_FINISH:
                                        secret.completeSize = -1;
                                        mSecrets.remove(sid);
                                        for (ProgressListener<Secret> listener : mListener) {
                                            listener.onResult(true, secret);
                                        }
                                        break;
                                    case MSG_ERROR:
                                        mSecrets.remove(sid);
                                        for (ProgressListener<Secret> listener : mListener) {
                                            listener.onResult(false, secret);
                                        }
                                        break;
                                }
                                return true;
                            }
                        })));
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
    public boolean download(Secret secret) {
        SecretDownloadBean bean = new SecretDownloadBean(secret);
        bean.setFolder(mSaveFolder);
        bean.setFileName(secret.getCacheFileName());
        if (mDownloader.addTask(bean)) {
            mSecrets.put(secret.sid, secret);
            mDownloader.startDownload();
            return true;
        } else {
            return false;
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
        public void onDownloading(String id, long completeSize, long totalSize) {
            Message msg = handler.obtainMessage(MSG_PROGRESS);
            msg.obj = id;
            msg.arg1 = (int) completeSize;
            msg.sendToTarget();
        }

        @Override
        public void onSpeedUpdate(String id, long speed) {}

        @Override
        public void onComplete(String id, File file) {
            // 下载完成，需要解密
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
            SecretDownloadBean secretBean = (SecretDownloadBean) bean;
            XHttpResponse response = mHttpClient.execute(ApiUtil.getSecretDownloadUrl
                    (secretBean.secret.mid, secretBean.secret.sid));
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

        @Override
        protected String postDownload(XDownloadBean bean) {
            if (!(bean instanceof SecretDownloadBean)) {
                return "-0100";
            }
            SecretDownloadBean secretBean = (SecretDownloadBean) bean;
            File srcFile = new File(secretBean.getFolder(), secretBean.getFileName());
            File deFile = decrypt(secretBean.secret, srcFile);
            if (deFile != null && deFile.exists()) {
                secretBean.secret.localPath = deFile.getAbsolutePath();// 设置localPath!
                srcFile.delete();
                XLog.d(TAG, "secret文件解密成功!" + deFile.getAbsolutePath());
                return null;
            } else {
                XLog.d(TAG, "secret文件解密失败!");
                return "-0100";
            }
        }

        /**
         * 解密文件
         */
        private File decrypt(Secret secret, File srcFile) {
            if (XStringUtil.isEmpty(secret.sid))
                return null;
            String decryptFileName = "d_" + srcFile.getName();
            File decryptFile = new File(srcFile.getParentFile(), decryptFileName);
            if (decryptFile.exists()) {
                // 已经存在，则删除重新解密
                decryptFile.delete();
            }
            if (XStorageUtil.isFull(srcFile.getParentFile().getAbsolutePath(), secret.size)) {
                return null;// 容量不足
            }
            String keyStr = CryptoUtil.toMd5(secret.sid, 16);
            String ivStr = CryptoUtil.toMd5(String.valueOf(secret.createTime), 16);
            decryptFile = CryptoUtil.aesDecryptFile(srcFile.getAbsolutePath(),
                    keyStr.getBytes(), ivStr.getBytes(), decryptFile.getAbsolutePath());
            return decryptFile;
        }
    }

}
