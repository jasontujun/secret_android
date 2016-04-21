package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.annotation.XId;

import org.json.JSONArray;
import org.json.JSONObject;

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

    public int width;

    public int height;

    public long size;

    public long createTime;

    public String mime;

    /**
     * DSF供应商类型。
     */
    public int dsf;

    /**
     * 下载url(可能因为鉴权过期，需要重新从服务器获取)
     */
    public String url;

    // 本地属性，非服务器端属性
    public String localPath;


    public static Secret fromJson(JSONObject jo) {
        return null;
    }

    public static List<Secret> fromJson(JSONArray ja) {
        return null;
    }
}
