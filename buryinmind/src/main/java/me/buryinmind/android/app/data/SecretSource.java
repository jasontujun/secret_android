package me.buryinmind.android.app.data;

import com.tj.xengine.android.data.XListIdDBDataSourceImpl;
import com.tj.xengine.android.data.listener.XAsyncDatabaseListener;
import com.tj.xengine.core.data.XWithId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.buryinmind.android.app.model.Secret;

/**
 * Created by jasontujun on 2016/6/2.
 */
public class SecretSource extends XListIdDBDataSourceImpl<Secret> {

    private XWithId.Listener<Secret> mSyncDbListener;

    public SecretSource(String sourceName) {
        super(Secret.class, sourceName);
        // 添加实时同步数据库的功能
        mSyncDbListener = new XAsyncDatabaseListener<Secret>(Secret.class, this);
        registerListener(mSyncDbListener);
    }

    @Override
    protected Secret replace(int index, Secret newItem) {
        Secret oldItem = get(index);
        oldItem.dfs = newItem.dfs;
        oldItem.size = newItem.size;
        oldItem.width = newItem.width;
        oldItem.height = newItem.height;
        oldItem.mime = newItem.mime;
        oldItem.order = newItem.order;
        oldItem.createTime = newItem.createTime;
        oldItem.uploadTime = newItem.uploadTime;
        return oldItem;
    }

    public void saveToDatabase(Secret secret) {
        List<Secret> secrets = new ArrayList<Secret>();
        secrets.add(secret);
        saveToDatabase(secrets);
    }

    public void saveToDatabase(List<Secret> secrets) {
        mSyncDbListener.onReplace(secrets, null);
    }

    public List<Secret> getByMemoryId(String mid) {
        List<Secret> secrets = new ArrayList<Secret>();
        for (Secret secret : mItemList) {
            if (secret.mid.equals(mid)) {
                secrets.add(secret);
            }
        }
        Collections.sort(secrets, Secret.comparator);
        return secrets;
    }
}
