package me.buryinmind.android.app.adapter;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.utils.XStringUtil;

import me.buryinmind.android.app.R;

/**
 * Created by jasontujun on 2016/6/14.
 */
public class DescriptionAdapter extends XListAdapter<String> {

    public static final String TAG = DescriptionAdapter.class.getSimpleName();

    public interface Listener {
        void onAdd(String des);
        void onDelete(String des);
        void onSelect(int pos, String des);
    }

    private static final int TYPE_INPUT = 1;
    private static final int TYPE_DES = 2;
    private int selectedIndex;
    private Context context;
    private Listener listener;

    public DescriptionAdapter(Context context) {
        this(context, null);
    }

    public DescriptionAdapter(Context context, Listener listener) {
        super(R.layout.item_description);
        selectedIndex = -1;
        this.context = context;
        this.listener = listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1)
            return TYPE_INPUT;
        else
            return TYPE_DES;
    }

    @Override
    public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_INPUT) {
            XLog.d(TAG, "onCreateViewHolder(). TYPE_INPUT.");
            return new XViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_description_input, parent, false));
        } else {
            XLog.d(TAG, "onCreateViewHolder(). TYPE_DES.");
            return super.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(final XViewHolder holder, final int position) {
        int type = getItemViewType(position);
        if (type == TYPE_INPUT) {
            XLog.d(TAG, "onBindViewHolder(). TYPE_INPUT.");
            // 初始化描述输入框
            final EditText editText = (EditText) holder.getView(R.id.des_input);
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == EditorInfo.IME_ACTION_GO) {
                        editText.setError(null);
                        String des = editText.getText().toString().trim();
                        if (!XStringUtil.isEmpty(des)) {
                            if (!getData().contains(des)) {
                                // 清除之前选中的item
                                int lastSelectedIndex = selectedIndex;
                                selectedIndex = -1;
                                if (lastSelectedIndex != -1) {
                                    notifyItemChanged(lastSelectedIndex);
                                }
                                XLog.d(TAG, "Enter IME_ACTION_GO！");
                                // 添加item
                                editText.setText(null);
                                addData(des);
                                // 添加成功，回调
                                if (listener != null)
                                    listener.onAdd(des);
                            } else {
                                editText.setError(context.getString(R.string.error_duplicate_des));
                            }
                        }
                        return true;
                    }
                    return false;
                }
            });
            editText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL &&
                            event.getAction() == KeyEvent.ACTION_UP) {
                        String des = editText.getText().toString();
                        if (XStringUtil.isEmpty(des) && getData().size() > 0) {
                            if (selectedIndex == -1) {
                                // 默认选中最后一个description
                                selectedIndex = getData().size() - 1;
                                XLog.d(TAG, "Enter KEYCODE_DEL！");
                                notifyItemChanged(selectedIndex);
                                // 选中成功，回调
                                if (listener != null)
                                    listener.onSelect(selectedIndex, getData().get(selectedIndex));
                            } else {
                                // 删除选中的description
                                int deleteIndex = selectedIndex;
                                String deleteDes = getData().get(deleteIndex);
                                selectedIndex = -1;
                                deleteData(deleteIndex);
                                // 删除成功，回调
                                if (listener != null)
                                    listener.onDelete(deleteDes);
                            }
                        }
                    }
                    return false;
                }
            });
        } else {
            XLog.d(TAG, "onBindViewHolder(). TYPE_DES.");
            String item = getData().get(position);
            holder.getView(R.id.des_txt, TextView.class).setText(item);
            if (position == selectedIndex) {
                holder.getView(R.id.des_txt, TextView.class)
                        .setTextColor(context.getResources().getColor(R.color.white));
                holder.getView(R.id.des_txt).setBackgroundResource(R.color.gray);
            } else {
                holder.getView(R.id.des_txt, TextView.class)
                        .setTextColor(context.getResources().getColor(R.color.gray));
                holder.getView(R.id.des_txt).setBackgroundResource(R.color.white);
            }
            holder.getView(R.id.des_txt).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedIndex == position) {
                        selectedIndex = -1;
                        holder.getView(R.id.des_txt, TextView.class)
                                .setTextColor(context.getResources().getColor(R.color.gray));
                        holder.getView(R.id.des_txt).setBackgroundResource(R.color.white);
                        // 取消选中，回调
                        if (listener != null)
                            listener.onSelect(selectedIndex, null);
                    } else {
                        holder.getView(R.id.des_txt, TextView.class)
                                .setTextColor(context.getResources().getColor(R.color.white));
                        holder.getView(R.id.des_txt).setBackgroundResource(R.color.gray);
                        int lastSelectedIndex = selectedIndex;
                        selectedIndex = position;
                        notifyItemChanged(lastSelectedIndex);
                        // 选中成功，回调
                        if (listener != null)
                            listener.onSelect(selectedIndex, getData().get(selectedIndex));
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return getData().size() + 1;
    }
}
