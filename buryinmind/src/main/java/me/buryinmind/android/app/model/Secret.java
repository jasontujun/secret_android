package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.util.ImageUtil;

/**
 * 数据类型Secret.
 * Created by jason on 2016/4/19.
 */
public class Secret {

    public static final String MIME_JPEG = "image/jpeg";
    public static final String MIME_PNG = "image/png";

    @XId
    public String sid;// sid为空，表示本地创建的Secret对象，还未添加到服务器

    public String mid;

    public int order;

    public int width;

    public int height;

    public long size;

    public long createTime;

    public long uploadTime;

    public String mime;

    public int dfs;// DSF供应商类型

    public String key;// DSF中key值


    // 本地属性，非服务器端属性
    public boolean needUpload;// 是否需要同步上传文件
    public String localPath;// 本地存储的路径


    public static Secret createLocal(String mid, String localPath) {
        Secret secret = new Secret();
        secret.needUpload = true;
        secret.mid = mid;
        secret.localPath = localPath;
        secret.size = new File(localPath).length();
        int[] dimension = ImageUtil.getDimension(localPath);
        secret.width = dimension[0];
        secret.height = dimension[1];
        if (localPath.toLowerCase().endsWith("png")) {
            secret.mime = MIME_PNG;
        } else if (localPath.toLowerCase().endsWith("jpg") ||
                localPath.toLowerCase().endsWith("jpeg")) {
            secret.mime = MIME_JPEG;
        }
        return secret;
    }

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
            secret.uploadTime = jo.getLong("upload_time");
            secret.needUpload = false;
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
