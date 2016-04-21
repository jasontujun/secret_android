package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tj.xengine.core.utils.XStringUtil;

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.R;
import me.buryinmind.android.app.model.MemoryGift;
import me.buryinmind.android.app.model.User;

/**
 * Created by jasontujun on 2016/4/20.
 */
public class ChooseAccountFragment extends Fragment {

    public static final String KEY_ACCOUNTS = "accounts";
    public static final String KEY_GIFTS = "gifts";
    public static final String KEY_SEED = "isSeed";

    private FragmentInteractListener mListener;
    private List<User> accountList;
    private List<MemoryGift> giftList;
    private boolean isSeed;// 是否是种子用户选择列表

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mListener = (FragmentInteractListener) getArguments().getSerializable(FragmentInteractListener.KEY);
            isSeed = getArguments().getBoolean(KEY_SEED);
            if (isSeed) {
                giftList = (List<MemoryGift>) getArguments().getSerializable(KEY_GIFTS);
                if (giftList == null)
                    giftList = new ArrayList<MemoryGift>();
            } else {
                accountList = (List<User>) getArguments().getSerializable(KEY_ACCOUNTS);
                if (accountList == null)
                    accountList = new ArrayList<User>();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_account_list, container, false);
        TextView mAccountListPromptView = (TextView) rootView.findViewById(R.id.account_list_prompt_txt);
        RecyclerView mAccountListView = (RecyclerView) rootView.findViewById(R.id.account_list);
        if (isSeed) {
            mAccountListView.setAdapter(new GiftAdapter(giftList));
            mAccountListPromptView.setText(R.string.info_activate_account);
        } else {
            mAccountListView.setAdapter(new UserAdapter(accountList));
            mAccountListPromptView.setText(R.string.info_choose_account);
        }
        return rootView;
    }


    public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<User> mValues;

        public UserAdapter(List<User> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mHeadView.setImageResource(R.drawable.headicon_active);
            holder.mNameView.setText(holder.mItem.name);
            holder.mDescriptionView.setText(XStringUtil.list2String(holder.mItem.descriptions, ", "));
            holder.mQuestionView.setVisibility(View.GONE);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onFinish(true, holder.mItem);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final ImageView mHeadView;
            public final TextView mNameView;
            public final TextView mDescriptionView;
            public final TextView mQuestionView;
            public User mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mHeadView = (ImageView) view.findViewById(R.id.account_head_img);
                mNameView = (TextView) view.findViewById(R.id.account_name_txt);
                mDescriptionView = (TextView) view.findViewById(R.id.account_des_txt);
                mQuestionView = (TextView) view.findViewById(R.id.account_question_txt);
            }
        }
    }


    public class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.ViewHolder> {
        private final List<MemoryGift> mValues;

        public GiftAdapter(List<MemoryGift> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mHeadView.setImageResource(R.drawable.headicon_seed);
            holder.mNameView.setText(holder.mItem.receiverName);
            holder.mDescriptionView.setText(XStringUtil.list2String(holder.mItem.receiverDescription, ", "));
            holder.mQuestionView.setVisibility(View.VISIBLE);
            holder.mQuestionView.setText(holder.mItem.question);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onFinish(true, holder.mItem);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final ImageView mHeadView;
            public final TextView mNameView;
            public final TextView mDescriptionView;
            public final TextView mQuestionView;
            public MemoryGift mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mHeadView = (ImageView) view.findViewById(R.id.account_head_img);
                mNameView = (TextView) view.findViewById(R.id.account_name_txt);
                mDescriptionView = (TextView) view.findViewById(R.id.account_des_txt);
                mQuestionView = (TextView) view.findViewById(R.id.account_question_txt);
            }
        }
    }
}
