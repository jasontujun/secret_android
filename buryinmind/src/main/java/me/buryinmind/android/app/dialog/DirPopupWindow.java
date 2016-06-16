package me.buryinmind.android.app.dialog;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import me.buryinmind.android.app.R;
import me.buryinmind.android.app.model.ImageFolder;
import me.buryinmind.android.app.adapter.XListAdapter;
import me.buryinmind.android.app.adapter.XViewHolder;

public class DirPopupWindow extends PopupWindow {

    public interface Listener {
        void onSelected(ImageFolder item);
    }

    private Context mContext;
    private Listener mListener;

    public DirPopupWindow(View contentView, int width, int height,
                          boolean focusable) {
        this(contentView, width, height, focusable, null);
    }

    public DirPopupWindow(View contentView, int width, int height,
                          boolean focusable, List<ImageFolder> data) {
        super(contentView, width, height, focusable);
        mContext = contentView.getContext();

        setBackgroundDrawable(new BitmapDrawable());
        setTouchable(true);
        setOutsideTouchable(true);
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        RecyclerView mListView = (RecyclerView) contentView.findViewById(R.id.dir_list);
        mListView.setAdapter(new DirAdapter(data));
    }


    public void setListener(Listener listener) {
        mListener = listener;
    }


    private class DirAdapter extends XListAdapter<ImageFolder> {

        public DirAdapter(List<ImageFolder> items) {
            super(R.layout.item_dir, items);
        }

        @Override
        public void onBindViewHolder(XViewHolder holder, int position) {
            final ImageFolder item = getData().get(position);
            holder.getView(R.id.dir_item_name, TextView.class).setText(item.getName());
            holder.getView(R.id.dir_item_count, TextView.class).setText("(" + item.getCount() + ")");
            Glide.with(mContext)
                    .load(item.getFirstImagePath())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.profile_default)
                    .into(holder.getView(R.id.dir_item_image, ImageView.class));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onSelected(item);
                    }
                }
            });
        }

    }
}
