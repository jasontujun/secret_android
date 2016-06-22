package me.buryinmind.android.app;

import android.app.Application;

import com.qiniu.android.storage.UploadManager;
import com.tj.xengine.android.db.XDatabase;
import com.tj.xengine.android.network.http.XAsyncHttpClient;
import com.tj.xengine.android.network.http.java.XJavaHttpClient;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListDataSource;
import com.tj.xengine.core.data.XListFilteredIdSourceImpl;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpConfig;

import java.io.File;

import me.buryinmind.android.app.controller.SecretImageDownloader;
import me.buryinmind.android.app.controller.SecretImageUploader;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.data.SecretSource;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;

/**
 * 自定义的Application。
 * 用来执行一些初始化操作，以及保存一些全局公用对象。
 * Created by jason on 2016/4/19.
 */
public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    public static final String SOURCE_GLOBAL = "global";
    public static final String SOURCE_MEMORY = "memory";
    public static final String SOURCE_SECRET = "secret";
    public static final String SOURCE_FRIEND = "friend";

    public static final long MAX_IMAGE_SIZE = 100 * 1024;// 图片大小上限100K

    private static File mCacheDir;
    private static XAsyncHttp mAsyncHttp;
    private static XHttp mHttp;
    private static UploadManager mUploadManager;
    private static long mImageTimestamp;
    private static SecretImageDownloader mSecretDownloader;
    private static SecretImageUploader mSecretUploader;

    @Override
    public void onCreate() {
        super.onCreate();
        XLog.d(TAG, "onCreate()");

        mCacheDir = getCacheDir();
        // 初始化Http模块
        mHttp = new XJavaHttpClient(XHttpConfig.builder()
                .setUserAgent("BuryInMind_Android")
                .build());
        mAsyncHttp = new XAsyncHttpClient(mHttp);
        mUploadManager = new UploadManager();
        mSecretUploader = new SecretImageUploader(this, mHttp, mUploadManager);
        mSecretDownloader = new SecretImageDownloader(mHttp, mCacheDir.getAbsolutePath());

        // 初始化数据库
        XDatabase.getInstance().init(getApplicationContext(), "buryinmind", 1);

        // 初始化数据源
        XDefaultDataRepo repo = XDefaultDataRepo.getInstance();
        repo.registerDataSource(new GlobalSource(this, SOURCE_GLOBAL));
        repo.registerDataSource(new XListIdDataSourceImpl<Memory>(Memory.class, SOURCE_MEMORY));
        repo.registerDataSource(new XListFilteredIdSourceImpl<User>(User.class, SOURCE_FRIEND));
        SecretSource secretSource = new SecretSource(SOURCE_SECRET);
        secretSource.setReplaceOverride(true);
        secretSource.loadFromDatabase();
        repo.registerDataSource(secretSource);
    }

    public static File getCacheDirectory() {
        return mCacheDir;
    }

    public static XHttp getHttp() {
        return mHttp;
    }

    public static XAsyncHttp getAsyncHttp() {
        return mAsyncHttp;
    }

    public static UploadManager getUploadManager() {
        return mUploadManager;
    }

    public static long getImageTimestamp() {
        return mImageTimestamp;
    }

    public static void updateImageTimestamp() {
        mImageTimestamp = System.currentTimeMillis();
    }

    public static SecretImageDownloader getSecretDownloader() {
        return mSecretDownloader;
    }

    public static SecretImageUploader getSecretUploader() {
        return mSecretUploader;
    }

    public static void clearDataSource() {
        ((XListDataSource) XDefaultDataRepo.getInstance().getSource(SOURCE_MEMORY)).clear();
        ((XListDataSource) XDefaultDataRepo.getInstance().getSource(SOURCE_FRIEND)).clear();
        ((XListDataSource) XDefaultDataRepo.getInstance().getSource(SOURCE_SECRET)).clear();
    }
}
