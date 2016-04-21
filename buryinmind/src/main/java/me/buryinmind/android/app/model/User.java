package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据类型User.
 * Created by jason on 2016/4/19.
 */
public class User implements Serializable {

    @XId
    public String uid;

    public String name;

    public List<String> descriptions;

    public int state;

    public long bornTime;

    public long createTime;

    // [本地属性]
    public List<Memory> ownMemoryList;
    // [本地属性]
    public List<Memory> otherMemoryList;

    public static User fromJson(JSONObject jo) {
        if (jo == null)
            return null;
        User user = new User();
        try {
            user.uid = jo.getString("logic_id");
            user.name = jo.getString("name");
            String desStr = jo.getString("description");
            if (XStringUtil.isEmpty(desStr)) {
                user.descriptions = new ArrayList<String>();
            } else {
                user.descriptions = new ArrayList<String>();
                JSONArray ja = new JSONArray(desStr);
                for (int i = 0; i < ja.length(); i++) {
                    user.descriptions.add(ja.getString(i));
                }
            }
            user.state = jo.getInt("state");
            Object bornObj = jo.get("born_time");
            if (bornObj != null && !bornObj.equals(JSONObject.NULL)) {
                user.bornTime = jo.getLong("born_time");
            }
            user.createTime = jo.getLong("create_time");
            return user;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<User> fromJson(JSONArray ja) {
        if (ja == null)
            return null;
        List<User> users = new ArrayList<User>();
        try {
            for (int i = 0; i < ja.length(); i++) {
                User user = fromJson(ja.getJSONObject(i));
                if (user == null) {
                    // 不应该出现！
                    return null;
                }
                users.add(user);
            }
            return users;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
