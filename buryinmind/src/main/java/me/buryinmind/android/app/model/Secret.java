package me.buryinmind.android.app.model;

import com.tj.xengine.android.db.annotation.XColumn;
import com.tj.xengine.android.db.annotation.XTable;
import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import me.buryinmind.android.app.util.ImageUtil;

/**
 * 数据类型Secret.
 * Created by jason on 2016/4/19.
 */
@XTable(name = "secret")
public class Secret {

    public static final Comparator<Secret> comparator = new Comparator<Secret>() {
        @Override
        public int compare(Secret lhs, Secret rhs) {
            if (lhs.order < rhs.order){
                return -1;
            } else if (lhs.order > rhs.order){
                return 1;
            } else {
                if (lhs.createTime < rhs.createTime)
                    return -1;
                else if (lhs.createTime > rhs.createTime)
                    return 1;
                else
                    return 0;
            }
        }
    };

    public static final String MIME_JPEG = "image/jpeg";
    public static final String MIME_PNG = "image/png";

    @XColumn(name = "sid")
    public String sid;// sid为空，表示本地创建的Secret对象，还未添加到服务器

    @XColumn(name = "mid")
    public String mid;

    @XColumn(name = "s_order")
    public int order;

    @XColumn(name = "s_width")
    public int width;

    @XColumn(name = "s_height")
    public int height;

    @XColumn(name = "s_size")
    public long size;

    @XColumn(name = "create_time")
    public long createTime;

    @XColumn(name = "upload_time")
    public long uploadTime;

    @XColumn(name = "s_mime")
    public String mime;

    @XColumn(name = "s_dfs")
    public int dfs;// DSF供应商类型


    // =========== 本地属性，非服务器端属性 =========== //

    @XId
    @XColumn(name = "id")
    private String id;// id=mid+sid

    @XColumn(name = "s_need_upload")
    public boolean needUpload;// 是否需要同步上传文件

    @XColumn(name = "s_local_path")
    public String localPath;// 本地存储的路径

    public long completeSize;// 已完成大小(上传或下载)


    public Secret() {
        completeSize = -1;
    }

    public String getId() {
        return id;
    }

    public void setId(String mid, String sid) {
        id = mid + sid;
    }

    public String getCacheFileName() {
        String fileName = sid + uploadTime;
        if (MIME_JPEG.equals(mime)) {
            return fileName + ".jpg";
        } else if (MIME_PNG.equals(mime)) {
            return fileName + ".png";
        } else {
            return fileName;
        }
    }

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

    public static JSONObject toJson(Secret secret) {
        if (secret == null || XStringUtil.isEmpty(secret.mid))
            return null;
        try {
            JSONObject jo = new JSONObject();
            jo.put("mid", secret.mid);
            jo.put("order", secret.order);
            jo.put("size", secret.size);
            jo.put("width", secret.width);
            jo.put("height", secret.height);
            jo.put("mime", secret.mime);
            return jo;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Secret fromJson(JSONObject jo) {
        if (jo == null)
            return null;
        Secret secret = new Secret();
        try {
            secret.sid = jo.getString("logic_id");
            secret.mid = jo.getString("memory_id");
            secret.setId(secret.mid, secret.sid);
            secret.order = jo.getInt("s_order");
            secret.dfs = jo.getInt("dfs");
            secret.width = jo.getInt("width");
            secret.height = jo.getInt("height");
            secret.size = jo.getLong("size");
            secret.mime = jo.getString("mime");
            secret.createTime = jo.getLong("create_time");
            if (jo.has("upload_time") && !jo.isNull("upload_time"))
                secret.uploadTime = jo.getLong("upload_time");
            secret.needUpload = false;
            return secret;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Secret> fromJson(JSONArray ja) {
        if (ja == null)
            return null;
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
