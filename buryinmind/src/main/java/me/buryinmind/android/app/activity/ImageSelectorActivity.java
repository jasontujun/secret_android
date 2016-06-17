package me.buryinmind.android.app.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.tj.xengine.android.utils.XLog;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.buryinmind.android.app.R;
import me.buryinmind.android.app.dialog.DirPopupWindow;
import me.buryinmind.android.app.model.ImageFolder;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.uicontrol.DividerGridItemDecoration;
import me.buryinmind.android.app.adapter.XViewHolder;

/**
 * Created by jasontujun on 2016/5/19.
 */
public class ImageSelectorActivity extends AppCompatActivity {

    private static final String TAG = ImageSelectorActivity.class.getSimpleName();
    private static final String ALL_IMAGE_DIR = "../";

    private DirPopupWindow mListImageDirPopupWindow;
    private ProgressDialog mProgressDialog;
    private RecyclerView mGridView;
    private RelativeLayout mBottomLy;
    private TextView mChooseDirBtn;
    private TextView mChooseImageDoneBtn;

    // 所有的图片文件夹
    private List<ImageFolder> mImageFolders = new ArrayList<ImageFolder>();
    private ImageFolder mSelectedFolder;
    private List<String> mSelectedImages = new ArrayList<String>();
    private int mScreenHeight;

    private AsyncTask mLoadLocalImageTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_image);

        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        mScreenHeight = outMetrics.heightPixels;

        Toolbar mToolBar = (Toolbar) findViewById(R.id.toolbar);
        TextView mToolBarTitle = (TextView) findViewById(R.id.toolbar_title);
        assert mToolBar != null;
        mToolBar.setTitle("");
        assert mToolBarTitle != null;
        mToolBarTitle.setText(R.string.info_choose_image);
        setSupportActionBar(mToolBar);

        mGridView = (RecyclerView) findViewById(R.id.img_grid);
        mChooseDirBtn = (TextView) findViewById(R.id.choose_dir_btn);
        mChooseImageDoneBtn = (TextView) findViewById(R.id.choose_image_done_btn);
        mBottomLy = (RelativeLayout) findViewById(R.id.bottom_layout);

        final GridLayoutManager manager = new GridLayoutManager(this, 3);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? manager.getSpanCount() : 1;
            }
        });
        mGridView.setLayoutManager(manager);
        mGridView.addItemDecoration(new DividerGridItemDecoration(this, 1, 0));

        // 为底部的布局设置点击事件，弹出popupWindow
        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListImageDirPopupWindow.setAnimationStyle(R.style.anim_popup_dir);
                mListImageDirPopupWindow.showAsDropDown(mBottomLy, 0, 0);
                // 设置背景颜色变暗
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 0.3f;
                getWindow().setAttributes(lp);
            }
        });
        mChooseImageDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if (mSelectedImages.size() == 0) {
                    setResult(RESULT_CANCELED, intent);
                } else {
                    intent.putExtra("result", (Serializable)mSelectedImages);
                    setResult(RESULT_OK, intent);
                }
                finish();
            }
        });
        loadImages();
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
        if (mLoadLocalImageTask != null) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            mLoadLocalImageTask.cancel(true);
        } else {
            supportFinishAfterTransition();
        }
    }

    private void selectImageFolder(ImageFolder folder) {
        if (mSelectedFolder == folder)
            return;
        mSelectedFolder = folder;
        mGridView.setAdapter(new ImageAdapter(folder.getImages()));
        mChooseDirBtn.setText(folder.getName());
        mChooseImageDoneBtn.setText(String.format(getResources()
                .getString(R.string.info_choose_image_done), mSelectedImages.size()));
    }

    /**
     * 初始化展示文件夹的popupWindow
     */
    private void initPopupWindw() {
        mListImageDirPopupWindow = new DirPopupWindow(
                LayoutInflater.from(getApplicationContext()).inflate(R.layout.fragment_dir, null),
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (mScreenHeight * 0.7),
                true, mImageFolders);

        mListImageDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // 设置背景颜色变暗
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1.0f;
                getWindow().setAttributes(lp);
            }
        });
        // 设置选择文件夹的回调
        mListImageDirPopupWindow.setListener(new DirPopupWindow.Listener() {
            @Override
            public void onSelected(ImageFolder folder) {
                selectImageFolder(folder);
                mListImageDirPopupWindow.dismiss();
            }
        });
    }

    /**
     * 利用ContentProvider扫描手机中的图片，此方法在运行在子线程中 完成图片的扫描，最终获得jpg最多的那个文件夹
     */
    private void loadImages() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "暂无外部存储", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mLoadLocalImageTask != null) {
            return;
        }
        XLog.e(TAG, "loadImages()!");
        mSelectedImages.clear();
        mImageFolders.clear();
        // 显示进度条
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, null, "正在加载...");
        } else {
            mProgressDialog.show();
        }
        mLoadLocalImageTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                XLog.e(TAG, "start search!");
                ContentResolver mContentResolver = ImageSelectorActivity.this
                        .getContentResolver();
                // 只查询jpeg和png的图片
                Cursor cursor = mContentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Images.Media.MIME_TYPE + "=? or "
                                + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[] {Secret.MIME_JPEG, Secret.MIME_PNG},
                        MediaStore.Images.Media.DATE_MODIFIED);

                XLog.e(TAG, "search count=" + cursor.getCount());
                Map<String, ImageFolder> dirPathMap = new HashMap<String, ImageFolder>();
                ImageFolder allImage = new ImageFolder(ALL_IMAGE_DIR, "所有照片");
                mImageFolders.add(allImage);
                // 遍历所有图片，按文件夹归类
                while (cursor.moveToNext()) {
                    String imagePath = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Images.Media.DATA));
                    allImage.addImage(imagePath);// 直接加入所有图片中
                    XLog.e(TAG, "image path=" + imagePath);
                    // 按照文件夹归档
                    File dirFile = new File(imagePath).getParentFile();
                    if (dirFile == null)
                        continue;
                    String dirPath = dirFile.getAbsolutePath();
                    ImageFolder imageFolder = dirPathMap.get(dirPath);
                    if (imageFolder == null) {
                        imageFolder = new ImageFolder(dirPath);
                        imageFolder.addImage(imagePath);
                        dirPathMap.put(dirPath, imageFolder);
                        mImageFolders.add(imageFolder);
                    } else {
                        imageFolder.addImage(imagePath);
                    }
                }
                cursor.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                XLog.d(TAG, "本地图片数量:" + mImageFolders.get(0).getCount());
                mLoadLocalImageTask = null;
                mProgressDialog.dismiss();
                if (mImageFolders.get(0).getCount() == 0) {
                    return;
                }
                selectImageFolder(mImageFolders.get(0));
                // 初始化展示文件夹的popupWindw
                initPopupWindw();
            }

            @Override
            protected void onCancelled(Void result) {
                XLog.e(TAG, "onCancelled!");
            }
        }.execute();
    }


    private class ImageAdapter extends RecyclerView.Adapter<XViewHolder> {
        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        private List<String> mData;

        public ImageAdapter(List<String> data) {
            mData = data;
        }

        private boolean isHeader(int position) {
            return position == 0;
        }

        @Override
        public int getItemViewType(int position) {
            return isHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
        }

        @Override
        public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_VIEW_TYPE_HEADER) {
                return new XViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_recyclerview_empty_header, parent, false));
            }
            return new XViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_grid, parent, false));
        }

        @Override
        public void onBindViewHolder(XViewHolder holder, int position) {
            if (isHeader(position)) {
                return;
            }
            final String item = mData.get(position - 1);
            //设置no_selected
            holder.getView(R.id.item_select_tip, ImageView.class).setImageResource(R.drawable.icon_check_box_blank_white);
            //设置图片
            Glide.with(ImageSelectorActivity.this)
                    .load(item)
                    .error(R.drawable.icon_image_default_grey)
                    .into(holder.getView(R.id.item_image, ImageView.class));

            final ImageView mImageView = holder.getView(R.id.item_image, ImageView.class);
            final ImageView mSelect = holder.getView(R.id.item_select_tip, ImageView.class);

            mImageView.setColorFilter(null);
            //设置ImageView的点击事件
            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //选择，则将图片变暗，反之则反之
                    if (mSelectedImages.contains(item)) {
                        // 已经选择过该图片
                        mSelectedImages.remove(item);
                        mSelect.setImageResource(R.drawable.icon_check_box_blank_white);
                        mImageView.setColorFilter(null);
                        mChooseImageDoneBtn.setText(String.format(getResources()
                                .getString(R.string.info_choose_image_done), mSelectedImages.size()));
                    } else {
                        // 未选择该图片
                        mSelectedImages.add(item);
                        mSelect.setImageResource(R.drawable.icon_check_box_white);
                        mImageView.setColorFilter(Color.parseColor("#77000000"));
                        mChooseImageDoneBtn.setText(String.format(getResources()
                                .getString(R.string.info_choose_image_done), mSelectedImages.size()));
                    }

                }
            });
            /**
             * 已经选择过的图片，显示出选择过的效果
             */
            if (mSelectedImages.contains(item)) {
                mSelect.setImageResource(R.drawable.icon_check_box_white);
                mImageView.setColorFilter(Color.parseColor("#77000000"));
            }
        }

        @Override
        public int getItemCount() {
            return mData.size() + 1;
        }
    }
}
