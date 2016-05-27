package me.buryinmind.android.app.model;

import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.annotation.XId;

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

    public List<Secret> secrets = new ArrayList<Secret>();

    public int age;// 临时属性，计算得出发生时的年龄


    public void calculateAge() {
        GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
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
}
