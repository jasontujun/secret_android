package me.buryinmind.android.app.model;

import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
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
        if (jo == null)
            return null;
        MemoryGift gift = new MemoryGift();
        try {
            gift.bid = jo.getString("logic_id");
            gift.mid = jo.getString("memory_id");
            gift.senderId = jo.getString("sender_id");
            gift.senderName = jo.getString("sender_name");
            gift.receiverId = jo.getString("receiver_id");
            gift.receiverName = jo.getString("receiver_name");
            String desStr = jo.getString("receiver_description");
            if (XStringUtil.isEmpty(desStr)) {
                gift.receiverDescription = new ArrayList<String>();
            } else {
                gift.receiverDescription = new ArrayList<String>();
                JSONArray ja = new JSONArray(desStr);
                for (int i = 0; i < ja.length(); i++) {
                    gift.receiverDescription.add(ja.getString(i));
                }
            }
            gift.question = jo.getString("question");
            gift.createTime = jo.getLong("create_time");
            return gift;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<MemoryGift> fromJson(JSONArray ja) {
        if (ja == null)
            return null;
        List<MemoryGift> gifts = new ArrayList<MemoryGift>();
        try {
            for (int i = 0; i < ja.length(); i++) {
                MemoryGift gift = fromJson(ja.getJSONObject(i));
                if (gift == null) {
                    // 不应该出现！
                    return null;
                }
                gifts.add(gift);
            }
            return gifts;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
