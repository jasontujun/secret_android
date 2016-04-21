package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.annotation.XId;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据类型Memory.
 * Created by jason on 2016/4/19.
 */
public class Memory {

    @XId
    public String mid;

    public String name;

    public String authorId;

    public String authorName;

    public String ownerId;

    public String ownerName;

    public long happenTime;

    public long createTime;

    public List<Secret> secrets = new ArrayList<Secret>();


    public static Memory fromJson(JSONObject jo) {
        return null;
    }

    public static List<Memory> fromJson(JSONArray ja) {
        return null;
    }
}
