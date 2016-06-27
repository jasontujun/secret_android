package me.buryinmind.android.app.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
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
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.TimeUtil;
import se.emilsjolander.flipview.FlipView;

/**
 * Created by jasontujun on 2016/6/27.
 */
public class MemoryGiftFragment extends XFragment {

    private static final String TAG = MemoryGiftFragment.class.getSimpleName();
    public static final String KEY_MID = "mid";
    public static final int REFRESH_UP = 10;
    public static final int REFRESH_DOWN = 11;
    public static final int REFRESH_DATA = 12;

    private FlipView mSecretFlip;
    private FlipAdapter mAdapter;
    private int mScreenWidth;
    private int mScreenHeight;

    private Memory mMemory;
    private boolean mWaiting;
    private int mLastPage;
    private ProgressListener<Secret> mDownloadListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        XLog.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        // 获取Memory
        Bundle argument = getArguments();
        if (argument != null) {
            String memoryId = argument.getString(KEY_MID);
            XListIdDataSourceImpl<Memory> source = (XListIdDataSourceImpl<Memory>)
                    XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_MEMORY);
            mMemory = source.getById(memoryId);
        }
        // 创建Adapter
        mAdapter = new FlipAdapter(getActivity());
        mLastPage = -1;
        // 在onCreate时请求数据，可以避免fragment切换时过于频繁请求
        requestDetail();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        XLog.d(TAG, "onCreateView()");
        // 获取屏幕高度宽度
        DisplayMetrics outMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;

        View rootView = inflater.inflate(R.layout.fragment_memory_gift, container, false);
        mSecretFlip = (FlipView) rootView.findViewById(R.id.secret_flip);
        mSecretFlip.setAdapter(mAdapter);
        mSecretFlip.setOnFlipListener(new FlipView.OnFlipListener() {
            @Override
            public void onFlippedToPage(FlipView v, int position, long id) {
                XLog.d(TAG, "onFlippedToPage().page=" + position);
                if (position < mLastPage) {
                    // 向上翻
                    mLastPage = position;
                    notifyRefresh(REFRESH_UP, null);
                } else if (position > mLastPage) {
                    // 向下翻
                    mLastPage = position;
                    notifyRefresh(REFRESH_DOWN, null);
                }
            }
        });
        if (mLastPage != -1) {
            mSecretFlip.flipTo(mLastPage);
        }
        return rootView;
    }

    @Override
    public void onStart() {
        XLog.d(TAG, "onStart()");
        super.onStart();
        if (mDownloadListener == null) {
            mDownloadListener = new ProgressListener<Secret>() {
                @Override
                public void onProgress(Secret secret,
                                       long completeSize,
                                       long totalSize) {
                    if (mMemory.secrets.contains(secret)) {
                        mAdapter.refreshView(secret, true);
                    }
                }

                @Override
                public void onResult(boolean result, Secret secret) {
                    if (result && mMemory.secrets.contains(secret)) {
                        // 本地缓存文件路径，同步到数据库
                        SecretSource source = (SecretSource) XDefaultDataRepo.getInstance()
                                .getSource(MyApplication.SOURCE_SECRET);
                        source.saveToDatabase(secret);
                        // 局部刷新列表的对应一项
                        XLog.d(TAG, "下载secret成功.id=" + secret.sid);
                        mAdapter.refreshView(secret, false);
                    }
                }
            };
        }
        MyApplication.getSecretDownloader().registerListener(mDownloadListener);
    }

    @Override
    public void onStop() {
        XLog.d(TAG, "onStop()");
        super.onStop();
        MyApplication.getSecretDownloader().unregisterListener(mDownloadListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        XLog.d(TAG, "onConfigurationChanged()");
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mAdapter.relayout();
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mAdapter.relayout();
        }
    }

    private boolean requestDetail() {
        if (mWaiting)
            return false;
        mWaiting = true;
        notifyLoading(true);
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.getMemoryDetail(mMemory.mid),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onCancelled() {
                        XLog.d(TAG, "cancel request Detail!");
                        mWaiting = false;
                        notifyLoading(false);
                    }

                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        notifyLoading(false);
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        notifyLoading(false);
                        Toast.makeText(getActivity(), R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jo) {
                        mWaiting = false;
                        notifyLoading(false);
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
                            mAdapter.setData(mMemory, mMemory.secrets);
                            notifyRefresh(REFRESH_DATA, null);
                            // 缓存Secret文件
                            for (Secret secret : mMemory.secrets) {
                                if (XStringUtil.isEmpty(secret.localPath) || (secret.size != 0 &&
                                        new File(secret.localPath).length() != secret.size)) {
                                    MyApplication.getSecretDownloader().download(secret);
                                }
                            }
                            mSecretFlip.peakNext(true);
                        }
                    }
                }));
        return true;
    }


    private class FlipAdapter extends BaseAdapter {

        private static final int ITEM_COVER = 0;
        private static final int ITEM_SECRET = 1;

        private LayoutInflater inflater;
        private Memory memory;
        private List<Secret> items = new ArrayList<Secret>();
        private List<ViewHolder> holders = new ArrayList<ViewHolder>();

        public FlipAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        public void refreshView(Secret item, boolean part) {
            int dataPosition = items.indexOf(item);
            if (dataPosition == -1) {
                return;
            }
            // 因为首位置是个封面，所以item的page位置为：pagePosition = dataPosition + 1
            int pagePosition = dataPosition + 1;
            int currentPage = mSecretFlip.getCurrentPage();
            View view = null;
            if (pagePosition == currentPage) {
                // item就在当前页
                view = mSecretFlip.getChildAt(1);
            } else if (pagePosition == currentPage + 1) {
                // item在下一页
                view = mSecretFlip.getChildAt(2);
            } else if (pagePosition == currentPage - 1) {
                // item就在上一页
                view = mSecretFlip.getChildAt(0);
            }
            if (view != null) {
                ViewHolder holder = (ViewHolder) view.getTag();
                // set progress
                if (item.completeSize >= 0) {
                    float percent = item.size == 0 ? 0 : ((float)item.completeSize / (float)item.size);
                    holder.mProgressView.setText(XStringUtil.percent2str(percent));
                    holder.mProgressView.setVisibility(View.VISIBLE);
                } else {
                    holder.mProgressView.setVisibility(View.GONE);
                }
                if (!part) {
                    // show image
                    if (XStringUtil.isEmpty(item.localPath)) {
                        holder.mImageView.setImageResource(R.color.darkGray);
                    } else {
                        Glide.with(MemoryGiftFragment.this)
                                .load(item.localPath)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(R.color.darkGray)
                                .into(holder.mImageView);
                    }
                }
            }
        }

        public void setData(Memory m, List<Secret> data) {
            memory = m;
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
            return items.size() + (memory == null ? 0 : 1);
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
                if (XStringUtil.isEmpty(memory.coverUrl)) {
                    holder.mImageView.setImageResource(R.color.darkGray);
                } else {
                    Glide.with(MemoryGiftFragment.this)
                            .load(memory.coverUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .priority(Priority.HIGH)
                            .error(R.color.darkGray)
                            .into(holder.mImageView);
                }
                Glide.with(MemoryGiftFragment.this)
                        .load(ApiUtil.getIdUrl(memory.authorId))
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.headicon_default)
                        .into(holder.mHeadView);
                holder.mNameView.setText(memory.name);
                holder.mAuthorNameView.setText(memory.authorName);
                holder.mDateView.setText(String.format(getResources().getString(R.string.info_memory_time),
                        XStringUtil.calendar2str(TimeUtil.getCalendar(memory.happenStartTime), ".")));
            } else {
                if (convertView == null) {
                    holder = new ViewHolder();
                    convertView = inflater.inflate(R.layout.item_gift_secret, parent, false);
                    holder.mImageView = (ImageView) convertView.findViewById(R.id.secret_item_img);
                    holder.mImageLayout = convertView.findViewById(R.id.secret_item_img_layout);
                    holder.mProgressView = (TextView) convertView.findViewById(R.id.secret_item_progress);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                final Secret item = (Secret) getItem(position);
                // set layout
                int imageViewWidth;
                int imageViewHeight;
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    imageViewWidth = mScreenWidth;
                    imageViewHeight = imageViewWidth * item.height / item.width;
                } else {
                    imageViewHeight = mScreenHeight;
                    imageViewWidth = imageViewHeight * item.width / item.height;;
                }
                ViewGroup.LayoutParams params = holder.mImageLayout.getLayoutParams();
                params.width = imageViewWidth;
                params.height = imageViewHeight;
                holder.mImageLayout.setLayoutParams(params);
                // show image
                if (XStringUtil.isEmpty(item.localPath)) {
                    holder.mImageView.setImageResource(R.color.darkGray);
                } else {
                    Glide.with(MemoryGiftFragment.this)
                            .load(item.localPath)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.color.darkGray)
                            .error(R.color.darkGray)
                            .into(holder.mImageView);
                }
                // set progress
                if (item.completeSize >= 0) {
                    float percent = item.size == 0 ? 0 : ((float)item.completeSize / (float)item.size);
                    holder.mProgressView.setText(XStringUtil.percent2str(percent));
                    holder.mProgressView.setVisibility(View.VISIBLE);
                } else {
                    holder.mProgressView.setVisibility(View.GONE);
                }
                // set click listener
                holder.mImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(),
                                "click image " + item.localPath, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return convertView;
        }

        private class ViewHolder {
            ImageView mImageView;
            ImageView mHeadView;
            TextView mNameView;
            TextView mAuthorNameView;
            TextView mDateView;
            View mImageLayout;
            TextView mProgressView;
            boolean needReLayout = false;
        }

    }
}
