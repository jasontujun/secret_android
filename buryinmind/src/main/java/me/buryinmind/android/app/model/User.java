package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 数据类型User.
 * Created by jason on 2016/4/19.
 */
public class User implements Serializable {

    public static final Comparator<User> comparator = new Comparator<User>() {
        @Override
        public int compare(User lhs, User rhs) {
            if (lhs.isFriend && !rhs.isFriend) {
                return  -1;
            } else if (!lhs.isFriend && rhs.isFriend) {
                return  1;
            } else {
                return lhs.name.compareTo(rhs.name);
            }
        }
    };

    public static final String HERITAGE_DEFAULT = "h_default";
    public static final String HERITAGE_DESTROY = "h_destroy";
    public static final String HERITAGE_PUBLIC = "h_public";

    @XId
    public String uid;

    public String name;

    public List<String> descriptions;

    public int state;

    public Long bornTime;// 如果用户没有设置生日，该值为null

    public long createTime;

    public String heritage;

    // 本地属性.标识此用户是否是好友
    public boolean isFriend;

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
            if (jo.has("born_time") && !jo.isNull("born_time")) {
                user.bornTime = jo.getLong("born_time");
            }
            if (jo.has("create_time") && !jo.isNull("create_time")) {
                user.createTime = jo.getLong("create_time");
            }
            if (jo.has("heritage") && !jo.isNull("heritage")) {
                user.heritage = jo.getString("heritage");
            }
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
