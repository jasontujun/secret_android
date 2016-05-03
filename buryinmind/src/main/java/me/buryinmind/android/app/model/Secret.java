package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.annotation.XId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据类型Secret.
 * Created by jason on 2016/4/19.
 */
public class Secret {

    public static final int DSF_QINIU = 76;

    @XId
    public String sid;

    public String mid;

    public int order;

    public int width;

    public int height;

    public long size;

    public long createTime;

    public String mime;

    /**
     * DSF供应商类型。
     */
    public int dfs;

    /**
     * 下载url(可能因为鉴权过期，需要重新从服务器获取)
     */
    public String url;

    // 本地属性，非服务器端属性
    public String localPath;


    public static Secret fromJson(JSONObject jo) {
        if (jo == null)
            return null;
        Secret secret = new Secret();
        try {
            secret.sid = jo.getString("logic_id");
            secret.mid = jo.getString("memory_id");
            secret.order = jo.getInt("s_order");
            secret.dfs = jo.getInt("dfs");
            secret.width = jo.getInt("width");
            secret.height = jo.getInt("height");
            secret.size = jo.getLong("size");
            secret.mime = jo.getString("mime");
            secret.createTime = jo.getLong("create_time");
            return secret;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Secret> fromJson(JSONArray ja) {
        List<Secret> secrets = new ArrayList<Secret>();
        try {
            for (int i = 0; i < ja.length(); i++) {
                Secret secret = fromJson(ja.getJSONObject(i));
                if (secret == null) {
                    // 不应该出现！
                    return null;
                }
                secrets.add(secret);
            }
            return secrets;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
