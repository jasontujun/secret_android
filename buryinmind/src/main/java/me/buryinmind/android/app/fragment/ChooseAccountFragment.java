package me.buryinmind.android.app.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.tj.xengine.core.utils.XStringUtil;

import java.util.ArrayList;
import java.util.List;

import me.buryinmind.android.app.R;
import me.buryinmind.android.app.model.MemoryGift;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;

/**
 * Created by jasontujun on 2016/4/20.
 */
public class ChooseAccountFragment extends Fragment {

    public static final String KEY_ACCOUNTS = "accounts";
    public static final String KEY_GIFTS = "gifts";
    public static final String KEY_TYPE = "type";
    public static final String KEY_CHOOSE_USER = "chooseUser";

    public static final int TYPE_ACTIVE_ACCOUNT_LIST = 1;
    public static final int TYPE_SEED_ACCOUNT_LIST = 2;
    public static final int TYPE_GIFT_LIST = 3;

    private FragmentInteractListener mListener;
    private User chooseUser;
    private List<User> accountList;
    private List<MemoryGift> giftList;
    private int type;// 列表界面类型

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mListener = (FragmentInteractListener) getArguments().getSerializable(FragmentInteractListener.KEY);
            type = getArguments().getInt(KEY_TYPE);
            switch (type) {
                case TYPE_ACTIVE_ACCOUNT_LIST:
                case TYPE_SEED_ACCOUNT_LIST:
                    accountList = (List<User>) getArguments().getSerializable(KEY_ACCOUNTS);
                    if (accountList == null)
                        accountList = new ArrayList<User>();
                    break;
                case TYPE_GIFT_LIST:
                    giftList = (List<MemoryGift>) getArguments().getSerializable(KEY_GIFTS);
                    if (giftList == null)
                        giftList = new ArrayList<MemoryGift>();
                    chooseUser = (User) getArguments().getSerializable(KEY_CHOOSE_USER);
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_choose_account, container, false);
        View accountLayout = rootView.findViewById(R.id.account_info_layout);
        TextView mAccountListPromptView = (TextView) rootView.findViewById(R.id.account_list_prompt_txt);
        RecyclerView mAccountListView = (RecyclerView) rootView.findViewById(R.id.account_list);
        ImageView accountHeadView = (ImageView) rootView.findViewById(R.id.account_head_img);
        TextView accountNameView = (TextView) rootView.findViewById(R.id.account_name_txt);
        TextView accountDesView = (TextView) rootView.findViewById(R.id.account_des_txt);
        Button backButton = (Button) rootView.findViewById(R.id.back_btn);
        switch (type) {
            case TYPE_ACTIVE_ACCOUNT_LIST:
            case TYPE_SEED_ACCOUNT_LIST:
                accountLayout.setVisibility(View.GONE);
                if (accountList.size() == 0) {
                    mAccountListPromptView.setText(R.string.info_account_none);
                    backButton.setVisibility(View.VISIBLE);
                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mListener != null)
                                mListener.onBack();
                        }
                    });
                } else {
                    mAccountListView.setAdapter(new UserAdapter(accountList));
                    mAccountListPromptView.setText(R.string.info_choose_account);
                }
                break;
            case TYPE_GIFT_LIST:
                if (chooseUser != null) {
                    accountLayout.setVisibility(View.VISIBLE);
                    accountNameView.setText(chooseUser.name);
                    accountDesView.setVisibility(View.GONE);
                    Glide.with(getActivity())
                            .load(ApiUtil.getHeadUrl(chooseUser.uid))
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.headicon_default)
                            .error(R.drawable.headicon_default)
                            .into(accountHeadView);
                } else {
                    accountLayout.setVisibility(View.GONE);
                }
                if (giftList.size() == 0) {
                    mAccountListPromptView.setText(R.string.info_account_none);
                    backButton.setVisibility(View.VISIBLE);
                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mListener != null)
                                mListener.onBack();
                        }
                    });
                    break;
                }
                mAccountListView.setAdapter(new GiftAdapter(giftList));
                mAccountListPromptView.setText(R.string.info_activate_account);
                break;
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
            Glide.with(getActivity())
                    .load(ApiUtil.getHeadUrl(holder.mItem.uid))
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.headicon_default)
                    .error(R.drawable.headicon_default)
                    .into(holder.mHeadView);

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
            public User mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mHeadView = (ImageView) view.findViewById(R.id.account_head_img);
                mNameView = (TextView) view.findViewById(R.id.account_name_txt);
                mDescriptionView = (TextView) view.findViewById(R.id.account_des_txt);
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
            holder.mNameView.setText(holder.mItem.senderName);
            holder.mDescriptionView.setVisibility(View.GONE);
            holder.mAboutLayout.setVisibility(View.VISIBLE);
            holder.mAboutView.setText(XStringUtil.list2String(holder.mItem.receiverDescription, ", "));
            holder.mQuestionLayout.setVisibility(View.VISIBLE);
            holder.mQuestionView.setText(holder.mItem.question);

            Glide.with(getActivity())
                    .load(ApiUtil.getHeadUrl(holder.mItem.receiverId))
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.headicon_default)
                    .error(R.drawable.headicon_default)
                    .into(holder.mHeadView);
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
            public final View mAboutLayout;
            public final TextView mAboutView;
            public final View mQuestionLayout;
            public final TextView mQuestionView;
            public MemoryGift mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mHeadView = (ImageView) view.findViewById(R.id.account_head_img);
                mNameView = (TextView) view.findViewById(R.id.account_name_txt);
                mDescriptionView = (TextView) view.findViewById(R.id.account_des_txt);
                mAboutLayout = view.findViewById(R.id.account_about_layout);
                mAboutView = (TextView) view.findViewById(R.id.account_about_txt);
                mQuestionLayout = view.findViewById(R.id.account_question_layout);
                mQuestionView = (TextView) view.findViewById(R.id.account_question_txt);
            }
        }
    }
}
