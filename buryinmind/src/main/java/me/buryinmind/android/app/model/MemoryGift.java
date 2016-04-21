package me.buryinmind.android.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

/**
 * Created by jasontujun on 2016/4/20.
 */
public class MemoryGift implements Serializable {

    public String bid;

    public String mid;

    public String senderId;

    public String senderName;

    public String receiverId;

    public String receiverName;

    public List<String> receiverDescription;

    public String question;

    public long createTime;


    public static MemoryGift fromJson(JSONObject jo) {
        return null;
    }

    public static List<MemoryGift> fromJson(JSONArray ja) {
        return null;
    }
}
