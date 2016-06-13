package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.util.TimeUtil;

/**
 * 数据类型Memory.
 * Created by jason on 2016/4/19.
 */
public class Memory {

    public static final Comparator<Memory> comparator = new Comparator<Memory>() {
        @Override
        public int compare(Memory lhs, Memory rhs) {
            if (lhs.happenTime < rhs.happenTime){
                return -1;
            } else if (lhs.happenTime > rhs.happenTime){
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

    @XId
    public String mid;

    public String name;

    public String authorId;

    public String authorName;

    public String ownerId;

    public String ownerName;

    public long happenTime;

    public long createTime;

    public boolean editable;

    public String coverUrl;

    public int coverWidth;

    public int coverHeight;


    // 本地属性，非服务器端属性
    public List<Secret> secrets = new ArrayList<Secret>();
    public List<MemoryGift> outGifts = new ArrayList<MemoryGift>();// 作为礼物赠送出去的记录
    public MemoryGift inGift;// 作为被赠送对象的记录
    public int age;// 回忆者当时的年龄

    public void calculateAge() {
        GlobalSource source = (GlobalSource) XDefaultDataRepo
                .getInstance().getSource(MyApplication.SOURCE_GLOBAL);
        final User user = source.getUser();
        if (user != null) {
            calculateAge(user.bornTime);
        }
    }

    public void calculateAge(long userBornTime) {
        this.age = TimeUtil.calculateAge(userBornTime, this.happenTime);
    }

    public static Memory fromJson(JSONObject jo) {
        if (jo == null)
            return null;
        Memory memory = new Memory();
        try {
            memory.mid = jo.getString("logic_id");
            memory.name = jo.getString("name");
            memory.authorId = jo.getString("author_id");
            memory.authorName = jo.getString("author_name");
            memory.ownerId = jo.getString("owner_id");
            memory.ownerName = jo.getString("owner_name");
            memory.happenTime = jo.getLong("happen_time");
            memory.createTime = jo.getLong("create_time");
            memory.editable = jo.getInt("editable") != 0;
            if (jo.has("cover_url") && !jo.isNull("cover_url")) {
                memory.coverUrl = jo.getString("cover_url");
            }
            if (jo.has("cover_width") && !jo.isNull("cover_width")) {
                memory.coverWidth = jo.getInt("cover_width");
            }
            if (jo.has("cover_height") && !jo.isNull("cover_height")) {
                memory.coverHeight = jo.getInt("cover_height");
            }
            memory.calculateAge();
            return memory;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Memory> fromJson(JSONArray ja) {
        if (ja == null)
            return null;
        List<Memory> memories = new ArrayList<Memory>();
        try {
            for (int i = 0; i < ja.length(); i++) {
                Memory memory = fromJson(ja.getJSONObject(i));
                if (memory == null) {
                    // 不应该出现！
                    return null;
                }
                memories.add(memory);
            }
            return memories;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Memory fromGiftJson(JSONObject jo) {
        if (jo == null)
            return null;
        if (!jo.has("memory_name") || jo.isNull("memory_name"))
            return null;
        Memory memory = new Memory();
        MemoryGift gift = new MemoryGift();
        try {
            // construct gift object
            gift.gid = jo.getString("logic_id");
            gift.mid = jo.getString("memory_id");
            gift.senderId = jo.getString("sender_id");
            gift.senderName = jo.getString("sender_name");
            if (jo.has("receiver_id") && !jo.isNull("receiver_id")) {
                gift.receiverId = jo.getString("receiver_id");
            }
            if (jo.has("receiver_name") && !jo.isNull("receiver_name")) {
                gift.receiverName = jo.getString("receiver_name");
            }
            String desStr = null;
            if (jo.has("receiver_description") && !jo.isNull("receiver_description")) {
                desStr = jo.getString("receiver_description");
            }
            if (XStringUtil.isEmpty(desStr)) {
                gift.receiverDescription = new ArrayList<String>();
            } else {
                gift.receiverDescription = new ArrayList<String>();
                JSONArray ja = new JSONArray(desStr);
                for (int i = 0; i < ja.length(); i++) {
                    gift.receiverDescription.add(ja.getString(i));
                }
            }
            if (jo.has("question") && !jo.isNull("question")) {
                gift.question = jo.getString("question");
            }
            if (jo.has("create_time") && !jo.isNull("create_time")) {
                gift.createTime = jo.getLong("create_time");
            }
            if (jo.has("take_time") && !jo.isNull("take_time")) {
                gift.takeTime = jo.getLong("take_time");
            }
            // construct memory obj
            memory.mid = gift.mid + gift.senderId + gift.receiverId;// 临时id
            memory.authorId = gift.senderId;
            memory.authorName = gift.senderName;
            memory.name = jo.getString("memory_name");
            if (jo.has("happen_time") && !jo.isNull("happen_time")) {
                memory.happenTime =jo.getLong("happen_time");
                memory.calculateAge();
            }
            if (jo.has("cover_url") && !jo.isNull("cover_url")) {
                memory.coverUrl =jo.getString("cover_url");
            }
            if (jo.has("cover_width") && !jo.isNull("cover_width")) {
                memory.coverWidth =jo.getInt("cover_width");
            }
            if (jo.has("cover_height") && !jo.isNull("cover_height")) {
                memory.coverHeight =jo.getInt("cover_height");
            }
            memory.inGift = gift;
            return memory;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Memory> fromGiftJson(JSONArray ja) {
        if (ja == null)
            return null;
        List<Memory> memories = new ArrayList<Memory>();
        try {
            for (int i = 0; i < ja.length(); i++) {
                Memory memory = fromGiftJson(ja.getJSONObject(i));
                if (memory == null) {
                    // 不应该出现！
                    return null;
                }
                memories.add(memory);
            }
            return memories;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
