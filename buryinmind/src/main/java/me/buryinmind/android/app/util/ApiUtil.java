package me.buryinmind.android.app.util;

import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XHttpRequest;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.model.User;

/**
 * Created by jason on 2016/4/20.
 */
public abstract class ApiUtil {
    private static final String DOMAIN = "http://192.168.1.107:3000";
    public static final String PUBLIC_DOMAIN = "http://o76ab22vz.bkt.clouddn.com";

    public static final int SCOPE_PRIVATE = 1;
    public static final int SCOPE_PUBLIC = 10;

    private static String getLocalUserId() {
        User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
        return user == null ? null : user.uid;
    }

    private static String getLocalToken() {
        GlobalSource source = (GlobalSource) XDefaultDataRepo
                .getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        return source.getUserToken();
    }

    public static String getIdUrl(String uid) {
        if (XStringUtil.isEmpty(uid))
            return null;
        return PUBLIC_DOMAIN + "/" + uid + "?_=" + MyApplication.getImageTimestamp();
    }

    public static XHttpRequest searchSeedUser(String name, List<String> des) {
        XHttpRequest request = MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/search")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("name", name);
        if (des != null && des.size() > 0) {
            request.addStringParam("des", JsonUtil.List2JsonString(des));
        }
        return request;
    }

    public static XHttpRequest searchActiveUser(String name, List<String> des) {
        XHttpRequest request = MyApplication.getHttp().newRequest(DOMAIN + "/users/search")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("name", name);
        if (des != null && des.size() > 0) {
            request.addStringParam("des", JsonUtil.List2JsonString(des));
        }
        return request;
    }

    public static XHttpRequest searchAllUser(String name, List<String> des) {
        XHttpRequest request = MyApplication.getHttp().newRequest(DOMAIN + "/users/all/search")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("name", name);
        if (des != null && des.size() > 0) {
            request.addStringParam("des", JsonUtil.List2JsonString(des));
        }
        return request;
    }

    public static XHttpRequest getFriendList() {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/friends")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken());
    }

    public static XHttpRequest getSeedUserDetail(String uid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/detail")
                .setMethod(XHttpRequest.Method.GET)
                .addStringParam("uid", uid);
    }

    public static XHttpRequest answerActivateQuestion(String uid, String gid, String answer) {
        String salt = "" + System.currentTimeMillis();
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/answer")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", uid)
                .addStringParam("gid", gid)
                .addStringParam("answer", CryptoUtil.toMd5(CryptoUtil.toMd5(answer), salt))
                .addStringParam("sa", salt);
    }

    public static XHttpRequest activateUser(String uid, String gid, String password,
                                            String email, String answer) {
        String salt = "" + System.currentTimeMillis();
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/seed/activate")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", uid)
                .addStringParam("gid", gid)
                .addStringParam("email", email)
                .addStringParam("password", CryptoUtil.toMd5(password))
                .addStringParam("answer", CryptoUtil.toMd5(CryptoUtil.toMd5(answer), salt))
                .addStringParam("sa", salt);
    }

    public static XHttpRequest loginUser(String uid, String password) {
        String salt = "" + System.currentTimeMillis();
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/login")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", uid)
                .addStringParam("password", CryptoUtil.toMd5(CryptoUtil.toMd5(CryptoUtil.toMd5(password), uid), salt))
                .addStringParam("sa", salt);
    }

    public static XHttpRequest logoutUser() {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/logout")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken());
    }

    public static XHttpRequest checkToken(String uid, String token) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/check")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", uid)
                .addStringParam("token", token);
    }

    public static XHttpRequest updateUser(User userInfo) {
        XHttpRequest request = MyApplication.getHttp().newRequest(DOMAIN + "/users/update")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken());
        if (userInfo.bornTime != null) {
            request.addStringParam("btime", String.valueOf(userInfo.bornTime));
        }
        if (userInfo.descriptions != null && userInfo.descriptions.size() > 0) {
            request.addStringParam("des", JsonUtil.List2JsonString(userInfo.descriptions));
        }
        if (!XStringUtil.isEmpty(userInfo.email)) {
            request.addStringParam("email", String.valueOf(userInfo.bornTime));
        }
        if (!XStringUtil.isEmpty(userInfo.heritage)) {
            request.addStringParam("heritage", userInfo.heritage);
        }
        if (userInfo.threshold != 0) {
            request.addStringParam("dt", String.valueOf(userInfo.threshold));
        }
        return request;
    }

    public static XHttpRequest updateMemoryInfo(String mid, String name,
                                                String heritage,
                                                long[] happenTime) {
        XHttpRequest request = MyApplication.getHttp().newRequest(DOMAIN + "/memory/update")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid);
        if (!XStringUtil.isEmpty(name)) {
            request.addStringParam("name", name);
        }
        if (!XStringUtil.isEmpty(heritage)) {
            request.addStringParam("heritage", heritage);
        }
        if (happenTime != null &&
                happenTime[0] <= happenTime[1]) {
            request.addStringParam("hstart", String.valueOf(happenTime[0]));
            request.addStringParam("hend", String.valueOf(happenTime[1]));
        }
        return request;
    }

    public static XHttpRequest updateMemoryCover(String mid, String coverUrl,
                                                 int coverWidth, int coverHeight) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/update")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("curl", coverUrl)
                .addStringParam("cw", String.valueOf(coverWidth))
                .addStringParam("ch", String.valueOf(coverHeight));
    }

    public static XHttpRequest getHeadToken() {
        return MyApplication.getHttp().newRequest(DOMAIN + "/users/head/uptoken")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken());
    }

    public static XHttpRequest getMemoryCoverToken(String mid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/cover/uptoken")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid);
    }

    public static XHttpRequest getMemoryList() {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/list")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken());
    }

    public static XHttpRequest addMemory(String name,
                                         long happenStartTime,
                                         long happenEndTime) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/add")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("name", name)
                .addStringParam("hstart", String.valueOf(happenStartTime))
                .addStringParam("hend", String.valueOf(happenEndTime));
    }

    public static XHttpRequest inMemory() {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/in")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken());
    }

    public static XHttpRequest deleteMemory(String mid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/delete")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid);
    }

    public static XHttpRequest postMemory(String mid, String receiverId,
                                          String receiverName, List<String> receiverDes,
                                          String question, String answer,
                                          int scope, Long receiveTime) {
        JSONArray ja = new JSONArray();
        for (String des : receiverDes) {
            ja.put(des);
        }
        XHttpRequest request = MyApplication.getHttp().newRequest(DOMAIN + "/memory/post")
                .setMethod(XHttpRequest.Method.POST)
                .setCharset("UTF-8")
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("rid", receiverId)
                .addStringParam("rname", receiverName)
                .addStringParam("rdes", ja.toString());
        if (scope == SCOPE_PUBLIC || scope == SCOPE_PRIVATE) {
            request.addStringParam("scope", String.valueOf(scope));
        }
        if (receiveTime != null) {
            request.addStringParam("future", String.valueOf(receiveTime));
        }
        if (!XStringUtil.isEmpty(question) && !XStringUtil.isEmpty(answer)) {
            request.addStringParam("question", question)
                    .addStringParam("answer", CryptoUtil.toMd5(answer));
        }
        return request;
    }

    public static XHttpRequest receiveMemory(String giftId, String answer) {
        XHttpRequest request = MyApplication.getHttp().newRequest(DOMAIN + "/memory/receive")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("gid", giftId);
        if (!XStringUtil.isEmpty(answer)) {
            String salt = "" + System.currentTimeMillis();
            request.addStringParam("answer", CryptoUtil.toMd5(CryptoUtil.toMd5(answer), salt))
                    .addStringParam("sa", salt);
        }
        return request;
    }

    public static XHttpRequest rejectMemory(String giftId) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/reject")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("gid", giftId);
    }

    public static XHttpRequest cancelMemory(String giftId) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/cancel")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("gid", giftId);
    }

    public static XHttpRequest getMemoryDetail(String mid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/detail")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid);
    }

    public static XHttpRequest addSecret(String mid, Secret secret) {
        List<Secret> secrets = new ArrayList<Secret>();
        secrets.add(secret);
        return addSecret(mid, secrets);
    }

    public static XHttpRequest addSecret(String mid, Collection<Secret> secrets) {
        JSONArray ja = new JSONArray();
        for (Secret se : secrets) {
            JSONObject jo = Secret.toJson(se);
            if (jo != null) {
                ja.put(jo);
            }
        }
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/add")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("secret", ja.toString());
    }


    public static XHttpRequest deleteSecret(String mid, String sid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/delete")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid);
    }

    public static XHttpRequest orderSecret(String mid, Collection<Secret> secrets) {
        JSONArray ja = new JSONArray();
        for (Secret se : secrets) {
            JSONObject jo = new JSONObject();
            try {
                jo.put("id", se.sid);
                jo.put("order", se.order);
                ja.put(jo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/order")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("order", ja.toString());
    }

    public static XHttpRequest getSecretDownloadUrl(String mid, String sid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/downurl")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid);
    }

    public static XHttpRequest getSecretUploadToken(String mid, String sid) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/uptoken")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid);
    }

    public static XHttpRequest callbackSecretUpload(String mid, String sid,
                                                    String key, int dfs) {
        return MyApplication.getHttp().newRequest(DOMAIN + "/memory/secret/callback")
                .setMethod(XHttpRequest.Method.POST)
                .addStringParam("uid", getLocalUserId())
                .addStringParam("token", getLocalToken())
                .addStringParam("mid", mid)
                .addStringParam("sid", sid)
                .addStringParam("key", key)
                .addStringParam("dfs", String.valueOf(dfs));
    }
}
