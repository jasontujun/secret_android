package me.buryinmind.android.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.tj.xengine.core.data.XDataSource;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.JsonUtil;

/**
 * 保存全局变量的数据源。
 * Created by jason on 2016/4/19.
 */
public class GlobalSource implements XDataSource {

    private static final String PREF_NAME = "buryinmind.global";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_DES = "userDescription";
    private static final String KEY_USER_TOKEN = "userToken";
    private static final String KEY_USER_TOKEN_TIME = "userTokenTime";
    private static final String KEY_USER_RECORD = "userRecord";

    public static final int NAME_MIN_SIZE = 2;// 密码最少位数
    public static final int PASSWORD_MIN_SIZE = 6;// 密码最少位数
    public static final long DEFAULT_TOKEN_DURATION = 24 * 60 * 60 *1000;// token默认的有效期:1天

    SharedPreferences pref;
    private String name;
    private List<String> userNameRecords;
    private User user;
    private String lastUserId;// 持久化到数据库的属性
    private String lastUserName;// 持久化到数据库的属性
    private List<String> lastUserDescriptions;// 持久化到数据库的属性
    private long lastTokenTime;// 持久化到数据库的属性
    private String userToken;// 持久化到数据库的属性

    public GlobalSource(Context context, String name) {
        this.name = name;
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.userToken = pref.getString(KEY_USER_TOKEN, null);
        this.lastTokenTime = pref.getLong(KEY_USER_TOKEN_TIME, 0);
        this.lastUserId = pref.getString(KEY_USER_ID, null);
        this.lastUserName = pref.getString(KEY_USER_NAME, null);
        this.lastUserDescriptions = new ArrayList<String>();
        String lastDesStr = pref.getString(KEY_USER_DES, null);
        if (!XStringUtil.isEmpty(lastDesStr)) {
            try {
                JSONArray ja = new JSONArray(lastDesStr);
                for (int i = 0; i < ja.length(); i++) {
                    lastUserDescriptions.add(ja.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        this.userNameRecords = new ArrayList<String>();
        String recordsStr = pref.getString(KEY_USER_RECORD, null);
        if (!XStringUtil.isEmpty(recordsStr)) {
            try {
                JSONArray ja = new JSONArray(recordsStr);
                for (int i = 0; i < ja.length(); i++) {
                    userNameRecords.add(ja.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public User getUser() {
        return user;
    }

    public String getUserToken() {
        return userToken;
    }

    public List<String> getUserNameRecords() {
        return userNameRecords;
    }

    public String getLastUserId() {
        return lastUserId;
    }

    public String getLastUserName() {
        return lastUserName;
    }

    public List<String> getLastUserDescriptions() {
        return lastUserDescriptions;
    }

    public long getLastTokenTime() {
        return lastTokenTime;
    }

    public void loginSuccess(User user, String userToken) {
        // 更新内存变量
        this.user = user;
        this.userToken = userToken;
        this.lastUserId = user.uid;
        this.lastUserName = user.name;
        this.lastUserDescriptions = user.descriptions;
        this.lastTokenTime = System.currentTimeMillis();
        // 更新到数据库
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_USER_TOKEN, userToken);// 更新UserToken
        editor.putLong(KEY_USER_TOKEN_TIME, this.lastTokenTime);// 更新UserTokenTime
        editor.putString(KEY_USER_ID, user.uid); // 更新UserId
        editor.putString(KEY_USER_NAME, user.name);// 更新UserName
        if (user.descriptions != null && user.descriptions.size() > 0) {// 更新UserDescriptions
            editor.putString(KEY_USER_DES, JsonUtil.List2JsonString(user.descriptions));
        } else {
            editor.putString(KEY_USER_DES, "");
        }
        if (user.name != null && !userNameRecords.contains(name)) {// 更新userNameRecords
            userNameRecords.add(name);
            editor.putString(KEY_USER_RECORD, JsonUtil.List2JsonString(userNameRecords));
        }
        editor.apply();
    }

    @Override
    public String getSourceName() {
        return name;
    }
}
