package me.buryinmind.android.app.activity;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.data.XListIdDataSourceImpl;
import com.tj.xengine.core.utils.XStringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.dialog.ConfirmDialog;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.Secret;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.uicontrol.XViewHolder;

/**
 * Created by jasontujun on 2016/5/15.
 */
public class MemoryDetailActivity extends AppCompatActivity {

    private static final String TAG = MemoryDetailActivity.class.getSimpleName();
    private static final int IMAGE_REQUEST_CODE = 9900;

    private CollapsingToolbarLayout mCollapsedLayout;
    private AppBarLayout mAppBar;
    private Toolbar mToolBar;
    private ImageView mMemoryProfileView;
    private RecyclerView mEditableView;
    private EditAdapter mEditAdapter;
    private ItemTouchHelper mItemTouchHelper;

    private boolean mCollapsed = false;
    private boolean mListTop = true;
    private boolean mAutoExpand = true;
    private Memory mMemory;
    private int mScreenWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_detail);

        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;

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

        mCollapsedLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        mMemoryProfileView = (ImageView) findViewById(R.id.memory_profile_bg);
        mEditableView = (RecyclerView) findViewById(R.id.secret_edit_list);
        TextView mMemoryTime = (TextView) findViewById(R.id.memory_time_view);
        View mAuthorLayout = findViewById(R.id.memory_author_layout);
        ImageView mAuthorHeader = (ImageView) findViewById(R.id.account_head_img);
        TextView mAuthorName = (TextView) findViewById(R.id.account_name_txt);

        // init toolbar
        mCollapsedLayout.setTitle(mMemory.name);
        setSupportActionBar(mToolBar);
        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset <= mToolBar.getHeight() - mCollapsedLayout.getHeight()) {
                    // 已经彻底折叠
                    XLog.d(TAG, "mAppBar already collapsed");
                    mCollapsed = true;
                } else {
                    mCollapsed = false;
                }
            }
        });

        // init profile and author
        Glide.with(MemoryDetailActivity.this)
                .load(ApiUtil.getIdUrl(memoryId))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .error(R.color.darkGray)
                .into(mMemoryProfileView);
        mMemoryProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO 修改Memory封面
            }
        });
        mMemoryTime.setText(String.format(getResources().getString(R.string.info_memory_time),
                XStringUtil.calendar2str(TimeUtil.getCalendar(mMemory.happenTime), ".")));
        if (!mMemory.ownerId.equals(mMemory.authorId)) {
            mAuthorLayout.setVisibility(View.VISIBLE);
            Glide.with(MemoryDetailActivity.this)
                    .load(ApiUtil.getIdUrl(mMemory.authorId))
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.headicon_default)
                    .into(mAuthorHeader);
            mAuthorName.setText(mMemory.authorName);
        } else {
            mAuthorLayout.setVisibility(View.GONE);
        }

        // init recycler view
        mEditableView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 已经滑到顶部，不能再向上滑了
                if (!ViewCompat.canScrollVertically(recyclerView, -1)) {
                    if (!mListTop) {
                        mListTop = true;
                        if (mAutoExpand) {
                            mAppBar.setExpanded(true, true);
                        }
                        XLog.d(TAG, "mEditableView scroll to top! expand appbar!");
                    } else {
                        XLog.d(TAG, "mEditableView already scroll to top! not expand appbar!");
                    }
                } else {
                    mListTop = false;
                }
            }
        });
        mEditableView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mEditAdapter = new EditAdapter(mMemory.secrets);
        mEditableView.setAdapter(mEditAdapter);
        ItemTouchHelper.Callback mCallback = new ItemTouchHelper.SimpleCallback
                (ItemTouchHelper.UP|ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                XLog.d(TAG, "onMove");
                int fromPosition = viewHolder.getAdapterPosition();//得到拖动ViewHolder的position
                int toPosition = target.getAdapterPosition();//得到目标ViewHolder的position
                XLog.d(TAG, "from=" + fromPosition + ",to="
                        + toPosition + ",total=" + mMemory.secrets.size());
                if (fromPosition < toPosition) {
                    //分别把中间所有的item的位置重新交换
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(mMemory.secrets, i, i + 1);
                        Collections.swap(mEditAdapter.mItems, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(mMemory.secrets, i, i - 1);
                        Collections.swap(mEditAdapter.mItems, i, i - 1);
                    }
                }
                mEditAdapter.notifyItemMoved(fromPosition, toPosition);
                //返回true表示执行拖动
                return true;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                XLog.d(TAG, "onSwiped, direction=" + direction);
                ConfirmDialog.newInstance(
                        getResources().getString(R.string.info_delete_secret),
                        new DialogListener() {
                            boolean confirm = false;

                            @Override
                            public void onDone(Object... result) {
                                confirm = (boolean) result[0];
                                if (confirm) {
                                    XViewHolder holder = (XViewHolder) viewHolder;
                                    mEditAdapter.deleteData((Secret) holder.getData());
                                }
                            }

                            @Override
                            public void onDismiss() {
                                if (!confirm) {
                                    mEditAdapter.notifyDataSetChanged();
                                }
                            }
                        }).show(getSupportFragmentManager(), ConfirmDialog.TAG);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }


            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    //滑动时改变Item的透明度
                    final float alpha = 1 - Math.abs(dX) / (float)viewHolder.itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder,
                                          int actionState) {
                XLog.d(TAG, "onSelectedChanged, actionState=" + actionState);
                // item拖拽被触发时
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    mAutoExpand = false;
                    if (!mCollapsed) {
                        mAppBar.setExpanded(false, true);
                    }
                    XViewHolder holder = (XViewHolder) viewHolder;
                    holder.getView(R.id.secret_item_cover_layout).setVisibility(View.VISIBLE);
                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder) {
                XLog.d(TAG, "clearView");
                super.clearView(recyclerView, viewHolder);
                mAutoExpand = true;
                XViewHolder holder = (XViewHolder) viewHolder;
                holder.getView(R.id.secret_item_cover_layout).setVisibility(View.GONE);
                holder.itemView.setAlpha(1);
            }
        };
        mItemTouchHelper = new ItemTouchHelper(mCallback);
        mItemTouchHelper.attachToRecyclerView(mEditableView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_memory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                XLog.d(TAG, "click back btn!");
                onBackPressed();
                return true;
            case R.id.action_add:
                XLog.d(TAG, "click add btn!");
                Intent intent = new Intent(MemoryDetailActivity.this, ImageSelectorActivity.class);
                startActivityForResult(intent, IMAGE_REQUEST_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case IMAGE_REQUEST_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    List<String> selectedImages = (List<String>) data.getExtras().getSerializable("result");
                    if (selectedImages == null)
                        break;
                    List<Secret> newSecrets = new ArrayList<Secret>();
                    for (String imagePath : selectedImages) {
                        Secret se = Secret.createLocal(mMemory.mid, imagePath);
                        se.order = mMemory.secrets.size();
                        mMemory.secrets.add(se);
                        newSecrets.add(se);
                    }
                    mEditAdapter.addData(newSecrets);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private class EditAdapter extends RecyclerView.Adapter<XViewHolder> {
        private final List<Secret> mItems;

        public EditAdapter(List<Secret> items) {
            mItems = new ArrayList<Secret>();
            if (items != null)
                mItems.addAll(items);
        }

        public void addData(Secret data) {
            int position = mItems.size();
            mItems.add(data);
            notifyItemInserted(position);
        }

        public void addData(List<Secret> data) {
            int position = mItems.size();
            mItems.addAll(data);
            notifyItemRangeInserted(position, data.size());
        }

        public void deleteData(Secret data) {
            int pos = mItems.indexOf(data);
            if (pos == -1) {
                return;
            }
            mItems.remove(data);
            notifyItemRemoved(pos);
        }


        @Override
        public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new XViewHolder(LayoutInflater.from(MemoryDetailActivity.this)
                    .inflate(R.layout.item_edit_secret, parent, false));
        }

        @Override
        public void onBindViewHolder(final XViewHolder holder, final int position) {
            final Secret item = mItems.get(position);
            holder.bindData(item);
            // set image
            ImageView imageView = (ImageView) holder.getView(R.id.secret_item_img);
            int imageViewWidth = mScreenWidth;
            int imageViewHeight = imageViewWidth  * item.height / item.width;
            XLog.d(TAG, "imageWidth=" + imageViewWidth + ",imageHeight=" + imageViewHeight);
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.width = imageViewWidth;
            params.height = imageViewHeight;
            imageView.setLayoutParams(params);
            Glide.with(MemoryDetailActivity.this)
                    .load(item.localPath)
                    .error(R.drawable.icon_image_default_grey)
                    .into(imageView);
            // set drag handle
            holder.getView(R.id.secret_item_handle).setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (MotionEventCompat.getActionMasked(event) ==
                                    MotionEvent.ACTION_DOWN) {
                                mItemTouchHelper.startDrag(holder);
                            }
                            return false;
                        }
                    });;
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MemoryDetailActivity.this,
                            "click image " + item.localPath, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }
}
