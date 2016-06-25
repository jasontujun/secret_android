package me.buryinmind.android.app.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ProgressListener;
import me.buryinmind.android.app.data.SecretSource;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.MemoryGift;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.util.ViewUtil;
import se.emilsjolander.flipview.FlipView;


/**
 * Created by jasontujun on 2016/6/24.
 */
public class MemoryGiftActivity extends AppCompatActivity {

    private static final String TAG = MemoryGiftActivity.class.getSimpleName();

    private View mProgressView;
    private Toolbar mToolBar;
    private TextView mToolBarTitle;
    private FlipView mSecretFlip;
    private FlipAdapter mAdapter;

    private Memory mMemory;
    private boolean mWaiting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_gift);

        Intent intent = getIntent();
        String memoryId = intent.getStringExtra("mid");
        if (XStringUtil.isEmpty(memoryId)) {
            // TODO error
            return;
        }
        XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
        mMemory = source.getById(memoryId);
        if (mMemory == null) {
            // TODO error
            return;
        }

        mProgressView = findViewById(R.id.loading_progress);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBarTitle = (TextView) findViewById(R.id.toolbar_title);
        mSecretFlip = (FlipView) findViewById(R.id.secret_flip);


        // init toolbar
        mToolBar.setTitle("");
        mToolBarTitle.setText(mMemory.name);
        setSupportActionBar(mToolBar);

        // init List
        mAdapter = new FlipAdapter(this);
        mAdapter.setData(mMemory.secrets);
        mSecretFlip.setAdapter(mAdapter);

        requestDetail();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        XLog.d(TAG, "onBackPressed()");
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            supportFinishAfterTransition();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        XLog.d(TAG, "onConfigurationChanged()");
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // land do nothing is ok
            mAdapter.relayout();
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // port do nothing is ok
            mAdapter.relayout();
        }
    }

    private void showProgress(boolean show) {
        ViewUtil.animateFadeInOut(mSecretFlip, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private boolean requestDetail() {
        if (mWaiting)
            return false;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryDetail(mMemory.mid),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(MemoryGiftActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        showProgress(false);
                        Toast.makeText(MemoryGiftActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jo) {
                        mWaiting = false;
                        showProgress(false);
                        List<Secret> secrets = null;
                        if (jo.has("secrets") && !jo.isNull("secrets")) {
                            try {
                                JSONArray secretArr = jo.getJSONArray("secrets");
                                secrets = Secret.fromJson(secretArr);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        XLog.d(TAG, "详情，secrets:" + (secrets == null ? 0 : secrets.size()));
                        if (secrets != null && secrets.size() > 0) {
                            SecretSource source = (SecretSource) XDefaultDataRepo.getInstance()
                                    .getSource(MyApplication.SOURCE_SECRET);
                            source.addAll(secrets);
                            mMemory.secrets = source.getByMemoryId(mMemory.mid);
                            mAdapter.setData(mMemory.secrets);
                            // 刷新列表
                            for (Secret secret : mMemory.secrets) {
                                // Secret文件已下载，直接显示
                                if (!XStringUtil.isEmpty(secret.localPath)) {
                                    File file = new File(secret.localPath);
                                    if (file.exists() && file.length() == secret.size) {
                                        mAdapter.notifyDataSetChanged();
                                        continue;
                                    }
                                }
                                // 否则，重新下载
                                MyApplication.getSecretDownloader().download(secret,
                                        new ProgressListener<Secret>() {
                                            @Override
                                            public void onProgress(Secret secret,
                                                                   long completeSize,
                                                                   long totalSize) {
                                                mAdapter.notifyDataSetChanged();
                                            }

                                            @Override
                                            public void onResult(boolean result, Secret secret) {
                                                if (result) {
                                                    // 本地缓存文件路径，同步到数据库
                                                    SecretSource source = (SecretSource) XDefaultDataRepo.getInstance()
                                                            .getSource(MyApplication.SOURCE_SECRET);
                                                    source.saveToDatabase(secret);
                                                    // 局部刷新列表的对应一项
                                                    XLog.d(TAG, "下载secret成功.id=" + secret.sid);
                                                    secret.completeSize = -1;
                                                    mAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
        return true;
    }


    public class FlipAdapter extends BaseAdapter {

        private static final int ITEM_COVER = 0;
        private static final int ITEM_SECRET = 1;

        private LayoutInflater inflater;
        private List<Secret> items = new ArrayList<Secret>();
        private List<ViewHolder> holders = new ArrayList<ViewHolder>();

        public FlipAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        public void setData(List<Secret> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        public void relayout() {
            for (ViewHolder holder : holders) {
                holder.needReLayout = true;
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? ITEM_COVER : ITEM_SECRET;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public Object getItem(int position) {
            return position == 0 ? null : items.get(position - 1);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return items.size() + 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (getItemViewType(position) == ITEM_COVER) {
                if (convertView == null) {
                    XLog.d(TAG, "getView().convertView == null.position=" + position);
                    holder = new ViewHolder();
                    convertView = inflater.inflate(R.layout.item_gift_cover, parent, false);
                    holder.mImageView = (ImageView) convertView.findViewById(R.id.memory_cover_view);
                    holder.mHeadView = (ImageView) convertView.findViewById(R.id.memory_author_head_img);
                    holder.mNameView = (TextView) convertView.findViewById(R.id.memory_name_txt);
                    holder.mAuthorNameView = (TextView) convertView.findViewById(R.id.memory_author_name_txt);
                    holder.mDateView = (TextView) convertView.findViewById(R.id.memory_time_view);
                    convertView.setTag(holder);
                    if (!holders.contains(holder)) {
                        holders.add(holder);
                    }
                } else {
                    holder = (ViewHolder) convertView.getTag();
                    if (holder.needReLayout) {
                        XLog.d(TAG, "getView().convertView needReLayout.position=" + position);
                        convertView = inflater.inflate(R.layout.item_gift_cover, parent, false);
                        holder.mImageView = (ImageView) convertView.findViewById(R.id.memory_cover_view);
                        holder.mHeadView = (ImageView) convertView.findViewById(R.id.memory_author_head_img);
                        holder.mNameView = (TextView) convertView.findViewById(R.id.memory_name_txt);
                        holder.mAuthorNameView = (TextView) convertView.findViewById(R.id.memory_author_name_txt);
                        holder.mDateView = (TextView) convertView.findViewById(R.id.memory_time_view);
                        holder.needReLayout = false;
                        convertView.setTag(holder);
                        if (!holders.contains(holder)) {
                            holders.add(holder);
                        }
                    } else {
                        XLog.d(TAG, "getView().convertView != null.position=" + position);
                    }
                }
                if (XStringUtil.isEmpty(mMemory.coverUrl)) {
                    holder.mImageView.setImageResource(R.color.darkGray);
                } else {
                    Glide.with(MemoryGiftActivity.this)
                            .load(mMemory.coverUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .priority(Priority.HIGH)
                            .error(R.color.darkGray)
                            .into(holder.mImageView);
                }
                Glide.with(MemoryGiftActivity.this)
                        .load(ApiUtil.getIdUrl(mMemory.authorId))
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.headicon_default)
                        .into(holder.mHeadView);
                holder.mNameView.setText(mMemory.name);
                holder.mAuthorNameView.setText(mMemory.authorName);
                holder.mDateView.setText(String.format(getResources().getString(R.string.info_memory_time),
                        XStringUtil.calendar2str(TimeUtil.getCalendar(mMemory.happenStartTime), ".")));
            } else {
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = inflater.inflate(R.layout.item_gift_secret, parent, false);
                    holder.mImageView = (ImageView) convertView.findViewById(R.id.secret_item_img);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                final Secret item = (Secret) getItem(position);
                Glide.with(MemoryGiftActivity.this)
                        .load(item.localPath)
                        .error(R.drawable.profile_default)
                        .into(holder.mImageView);
            }
            return convertView;
        }

        private class ViewHolder {
            ImageView mImageView;
            ImageView mHeadView;
            TextView mNameView;
            TextView mAuthorNameView;
            TextView mDateView;
            boolean needReLayout = false;
        }

    }
}
