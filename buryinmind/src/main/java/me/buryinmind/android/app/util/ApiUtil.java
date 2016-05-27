package me.buryinmind.android.app.util;

import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XHttpRequest;

import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.Secret;

/**
 * Created by jason on 2016/4/20.
 */
public abstract class ApiUtil {
    private static final String DOMAIN = "http://192.168.1.104:3000";
    private static final String PUBLIC_DOMAIN = "http://o76ab22vz.bkt.clouddn.com";

    public static String getIdUrl(String uid) {
        return PUBLIC_DOMAIN + "/" + uid + "?_=" + MyApplication.getImageTimestamp();
    }

    public static XHttpRequest searchSeedUser(String name, List<String> des) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/search")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("name", name)
                .addStringParam("des", JsonUtil.List2JsonString(des));
    };

    public static XHttpRequest searchActiveUser(String name, List<String> des) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/search")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("name", name)
                .addStringParam("des", JsonUtil.List2JsonString(des));
    };

    public static XHttpRequest getSeedUserDetail(String uid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/detail")
                .setMethod(XHttpRequest.Method.GET)
                .addStringParam("uid", uid);
    }

    public static XHttpRequest answerActivateQuestion(String uid, String bid, String answer) {
        String salt = "" + System.currentTimeMillis();
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/answer")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", uid)
                .addStringParam("bid", bid)
                .addStringParam("answer", CryptoUtil.toMd5(CryptoUtil.toMd5(answer), salt))
                .addStringParam("sa", salt);
    };

    public static XHttpRequest activateUser(String uid, String bid, String password, String email, String answer) {
        String salt = "" + System.currentTimeMillis();
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/activate")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", uid)
                .addStringParam("bid", bid)
                .addStringParam("email", email)
                .addStringParam("password", CryptoUtil.toMd5(password))
                .addStringParam("answer", CryptoUtil.toMd5(CryptoUtil.toMd5(answer), salt))
                .addStringParam("sa", salt);
    };

    public static XHttpRequest loginUser(String uid, String password) {
        String salt = "" + System.currentTimeMillis();
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/login")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", uid)
                .addStringParam("password", CryptoUtil.toMd5(CryptoUtil.toMd5(CryptoUtil.toMd5(password), uid), salt))
                .addStringParam("sa", salt);
    };

    public static XHttpRequest checkToken(String uid, String token) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/check")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", token);
    };

    private static String getLocalToken() {
        GlobalSource source = (GlobalSource) XDefaultDataRepo
                .getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        return source.getUserToken();
    }

    public static XHttpRequest updateBornTime(String uid, long bornTime) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/update")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("btime", String.valueOf(bornTime));
    }

    public static XHttpRequest getHeadToken(String uid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/head/uptoken")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken());
    }

    public static XHttpRequest getMemoryList(String uid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/list")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken());
    }

    public static XHttpRequest addMemory(String uid, String name, long happenTime) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/add")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("name", name)
                .addStringParam("ha", String.valueOf(happenTime));
    }

    public static XHttpRequest deleteMemory(String uid, String mid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/delete")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid);
    }

    public static XHttpRequest getMemoryDetail(String uid, String mid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/detail")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid);
    }

    public static XHttpRequest addSecret(String uid, String mid, Secret localSecret) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/add")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("size", String.valueOf(localSecret.size))
                .addStringParam("width", String.valueOf(localSecret.width))
                .addStringParam("height", String.valueOf(localSecret.height))
                .addStringParam("mime", localSecret.mime);
    }

    public static XHttpRequest deleteSecret(String uid, String mid, String sid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/delete")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid);
    }

    public static XHttpRequest getSecretDownloadUrl(String uid, String mid, String sid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/downurl")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid);
    }

    public static XHttpRequest getSecretUploadToken(String uid, String mid, String sid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/uptoken")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid);
    }

    public static XHttpRequest callbackSecretUpload(String uid, String mid, String sid,
                                                    String key, int dfs) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/callback")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid)
                .addStringParam("key", key)
                .addStringParam("dfs", String.valueOf(dfs));
    }
}
