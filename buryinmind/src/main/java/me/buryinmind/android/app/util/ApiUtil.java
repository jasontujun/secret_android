package me.buryinmind.android.app.util;

import com.tj.xengine.core.network.http.XHttpRequest;

import java.util.List;

import me.buryinmind.android.app.MyApplication;

/**
 * Created by jason on 2016/4/20.
 */
public abstract class ApiUtil {
//    private static final String DOMAIN = "http://127.0.0.1";
    private static final String DOMAIN = "http://192.168.1.58:3000";

    public static XHttpRequest searchSeedUser(String name, List<String> des) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/search")
                .setCharset("UTF-8")
                .addStringParam("name", name)
                .addStringParam("description", JsonUtil.List2JsonString(des));
    };

    public static XHttpRequest searchActiveUser(String name, List<String> des) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/search")
                .setCharset("UTF-8")
                .addStringParam("name", name)
                .addStringParam("description", JsonUtil.List2JsonString(des));
    };

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
}
