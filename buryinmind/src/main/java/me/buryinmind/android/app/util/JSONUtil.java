package me.buryinmind.android.app.util;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by jason on 2016/4/20.
 */
public abstract class JsonUtil {

    public static String List2JsonString(List list) {
        if (list == null)
            return "[]";
        JSONArray ja = new JSONArray();
        for (Object record : list) {
            ja.put(record);
        }
        return ja.toString();
    }
}
