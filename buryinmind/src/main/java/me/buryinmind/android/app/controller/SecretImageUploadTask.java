package me.buryinmind.android.app.controller;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.android.utils.XStorageUtil;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpRequest;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.toolkit.task.runnable.XFiniteRetryRunnable;
import com.tj.xengine.core.toolkit.taskmgr.XBaseMgrTaskExecutor;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.Future;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.CryptoUtil;

/**
 * Created by jasontujun on 2016/5/26.
 */
public class SecretImageUploadTask extends XBaseMgrTaskExecutor<SecretUploadBean> {

    private static final String TAG = SecretImageUploadTask.class.getSimpleName();

    // 进度长时间没变且文件读写测试失败，IO异常
    public static final String ERROR_IO_ERROR = "-0001";
    // 存储空间不足，导致无法继续写文件
    public static final String ERROR_NO_SPACE = "-0002";
    // response为空
    public static final String ERROR_NO_RESPONSE = "-0005";
    // 返回码不是200或200+
    public static final String ERROR_STATUS_CODE = "-0006";
    // JSON解析异常
    public static final String ERROR_JSON_EXCEPTION = "-0020";
    // JSON解析的字段为空
    public static final String ERROR_JSON_FIELD_NULL = "-0021";
    // dfs的SDK上传失败
    public static final String ERROR_UPLOAD_FAILED = "-0030";

    private UploadFileRunnable mRunnable;
    protected XHttp mHttpClient;
    protected File mDir;
    private XJsonObjectHandler mHttpHandler;
    private UploadManager mQiniuUploadMgr;

    public SecretImageUploadTask(SecretUploadBean bean,
                                 XHttp httpClient, File dir,
                                 UploadManager qiniuUploadMgr) {
        super(bean);
        mHttpClient = httpClient;
        mDir = dir;
        mQiniuUploadMgr = qiniuUploadMgr;
        mHttpHandler = new XJsonObjectHandler();
    }

    public SecretImageUploadTask(SecretUploadBean bean,
                                 int status, XHttp httpClient, File dir) {
        super(bean, status);
        mHttpClient = httpClient;
        mDir = dir;
    }

    /**
     * 异步执行下载任务。
     * 子类可以重写此方法，实现自定义的异步执行方式。
     * @return 返回异步执行任务的Future队形，可以空。
     * 如果返回的Future不为空，则会用于暂停Runnable执行。
     */
    protected Future asyncExecute(Runnable runnable) {
//        if (mThreadPool != null) {
//            return mThreadPool.submit(runnable);
//        }
        new Thread(runnable).start();
        return null;
    }

    @Override
    protected boolean onStart() {
        if (mRunnable != null)
            return false;

        // 创建Runnable但不执行
        mRunnable = new UploadFileRunnable(5);
        Future future = asyncExecute(mRunnable);
        mRunnable.setFuture(future);
        return true;
    }

    @Override
    protected boolean onPause() {
        if (mRunnable == null)
            return false;

        mRunnable.cancel();
        mRunnable = null;
        return true;
    }

    @Override
    protected boolean onAbort() {
        if (mRunnable != null) {
            mRunnable.cancel();
            mRunnable = null;
        }
        return true;
    }

    @Override
    protected boolean onEndSuccess() {
        mRunnable = null;
        return true;
    }

    @Override
    protected boolean onEndError(String s, boolean b) {
        mRunnable = null;
        return true;
    }

    @Override
    public long getCompleteSize() {
        return mRunnable == null ? 0 : mRunnable.completeSize;
    }

    /**
     * 加密本地文件
     */
    private static File encrypt(Secret secret, File dir) {
        if (XStringUtil.isEmpty(secret.sid))
            return null;
        String encryptFileName = "s_" + secret.sid + ".tmp";
        File encryptFile = new File(dir, encryptFileName);
        if (encryptFile.exists()) {
            // 加密的临时文件已经存在，则复用(可能由于上次暂停时遗留的)
            return encryptFile;
        }
        if (XStorageUtil.isFull(dir.getAbsolutePath(), secret.size)) {
            return null;// 容量不足
        }
        String keyStr = CryptoUtil.toMd5(secret.sid, 16);
        String ivStr = CryptoUtil.toMd5(String.valueOf(secret.createTime), 16);
        encryptFile = CryptoUtil.aesEncryptFile(secret.localPath,
                keyStr.getBytes(), ivStr.getBytes(), encryptFile.getAbsolutePath());
        return encryptFile;
    }

    private class UploadFileRunnable extends XFiniteRetryRunnable<SecretUploadBean> {

        private Future mFuture;// 在暂停线程时用于中断阻塞的Future对象
        private String errorCode;// 错误码
        private boolean success;
        private String token;
        private String key;
        private long completeSize;

        protected UploadFileRunnable(long max) {
            super(max);
            success = false;
        }

        public void setFuture(Future future) {
            mFuture = future;
        }

        @Override
        public void cancel() {
            super.cancel();
            if (mFuture != null)
                mFuture.cancel(true);
        }

        @Override
        public long getRetryInterval(long l) {
            return 30 * 1000;
        }

        @Override
        public SecretUploadBean getBean() {
            return SecretImageUploadTask.this.getBean();
        }

        @Override
        public boolean onPreExecute(SecretUploadBean bean) {
            // 判断存储空间是否已满，如果已满则结束下载
            if (XStorageUtil.isFull(mDir.getAbsolutePath(), bean.secret.size)) {
                XLog.d(TAG, "存储已满，无法下载...");
                errorCode = ERROR_NO_SPACE;
                return false;
            }
            return true;
        }

        @Override
        public void onPreExecuteError(SecretUploadBean bean) {
            SecretImageUploadTask.this.endError(errorCode, false);
        }

        @Override
        public boolean onRepeatExecute(SecretUploadBean bean) {
            completeSize = 0;
            SecretImageUploadTask.this.notifyDoing(completeSize);
            // 如果secret还未添加到服务器端，则调用AddSecret接口，返回sid和上传凭证;
            // 如果secret已经添加到服务器，则直接调用获取上传凭证接口，返回上传凭证。
            XHttpRequest request = XStringUtil.isEmpty(bean.secret.sid) ?
                    ApiUtil.addSecret(bean.secret.mid, bean.secret) :
                    ApiUtil.getSecretUploadToken(bean.secret.mid, bean.secret.sid);
            XHttpResponse response = mHttpClient.execute(request);
            if (response == null) {
                XLog.d(TAG, "获取secret上传凭证返回的response为空");
                errorCode = ERROR_NO_RESPONSE;
                return false;
            }
            // 如果被中断
            if (!isRunning()) {
                XLog.d(TAG, "Is Cancelled2");
                return false;
            }
            if (response.getStatusCode() < 200 && response.getStatusCode() >= 300) {
                XLog.d(TAG, "获取secret上传凭证错误，返回码：" + response.getStatusCode());
                errorCode = ERROR_STATUS_CODE;
                return false;
            }
            JSONObject jo = mHttpHandler.handleResponse(response);
            if (jo == null) {
                XLog.d(TAG, "获取secret上传凭证返回的response的内容为空");
                errorCode = ERROR_JSON_EXCEPTION;
                return false;
            }
            try {
                token = jo.getString("token");
                key = jo.getString("key");
                if (XStringUtil.isEmpty(token) || XStringUtil.isEmpty(key)) {
                    errorCode = ERROR_JSON_FIELD_NULL;
                    return false;
                }
                XLog.d(TAG, "secret上传凭证，token=" + token + ",key=" + key);
                if (XStringUtil.isEmpty(bean.secret.sid)) {
                    bean.secret.sid = jo.getString("sid");
                    bean.secret.dfs = jo.getInt("dfs");
                    bean.secret.createTime = jo.getLong("ctime");
                    bean.secret.setId(bean.secret.mid, bean.secret.sid);
                }
                success = true;
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
                errorCode = ERROR_JSON_EXCEPTION;
                return false;
            }
        }

        @Override
        public void onPostExecute(final SecretUploadBean bean) {
            if (success) {
                // 加密本地文件
                final File encryptFile = encrypt(bean.secret, mDir);
                if (encryptFile == null) {
                    SecretImageUploadTask.this.endError(ERROR_IO_ERROR, false);
                    return;
                }
                XLog.d(TAG, "加密本地文件成功!开始上传文件");
                mQiniuUploadMgr.put(encryptFile, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                if (info.isOK()) {
                                    XLog.d(TAG, "上传成功! " + info.toString());
                                    // 本地回调服务器，文件上传成功
                                    MyApplication.getAsyncHttp().execute(
                                            ApiUtil.callbackSecretUpload(bean.secret.mid,
                                                    bean.secret.sid, key, bean.secret.dfs),
                                            new XAsyncHttp.Listener() {
                                                @Override
                                                public void onNetworkError() {
                                                    XLog.d(TAG, "上传回调失败1!");
                                                    errorCode = ERROR_NO_RESPONSE;
                                                    SecretImageUploadTask.this.endError(errorCode, false);
                                                }

                                                @Override
                                                public void onFinishError(XHttpResponse xHttpResponse) {
                                                    XLog.d(TAG, "上传回调失败2!");
                                                    errorCode = ERROR_STATUS_CODE;
                                                    SecretImageUploadTask.this.endError(errorCode, false);
                                                }

                                                @Override
                                                public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                                                    XLog.d(TAG, "上传回调成功!");
                                                    bean.secret.needUpload = false;
                                                    SecretImageUploadTask.this.endSuccess();
                                                }
                                            });
                                } else {
                                    XLog.d(TAG, "上传失败! " + info.toString());
                                    errorCode = ERROR_UPLOAD_FAILED;
                                    SecretImageUploadTask.this.endError(errorCode, false);
                                }
                                if (encryptFile.delete()) {
                                    XLog.d(TAG, "上传结束!已经删除加密后的临时文件"
                                            + encryptFile.getAbsolutePath());
                                } else {
                                    XLog.d(TAG, "上传结束!没能删除加密后的临时文件"
                                            + encryptFile.getAbsolutePath());
                                }
                            }
                        },
                        new UploadOptions(null, null, false,
                                new UpProgressHandler() {
                                    public void progress(String key, double percent) {
                                        XLog.d(TAG, "上传进度:" + key + "=" + percent);
                                        completeSize = (long) (bean.secret.size * percent);
                                        SecretImageUploadTask.this.notifyDoing(completeSize);
                                    }
                                },
                                new UpCancellationSignal() {
                                    public boolean isCancelled() {
                                        return !isRunning();
                                    }
                                }));
            } else {
                SecretImageUploadTask.this.endError(errorCode, false);
            }
        }

        @Override
        public void onCancelled(SecretUploadBean bean) {

        }
    }
}
