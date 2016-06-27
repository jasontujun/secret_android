package me.buryinmind.android.app.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.tj.xengine.android.network.http.XAsyncHttp;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONObject;
import java.util.Calendar;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.controller.ProgressListener;
import me.buryinmind.android.app.controller.ResultListener;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.dialog.ConfirmDialog;
import me.buryinmind.android.app.dialog.DialogListener;
import me.buryinmind.android.app.model.Memory;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.ImageUtil;
import me.buryinmind.android.app.util.TimeUtil;
import me.buryinmind.android.app.util.ViewUtil;

/**
 * Created by jasontujun on 2016/6/15.
 */
public class MemoryAddFragment extends XFragment {

    private static final String TAG = MemoryAddFragment.class.getSimpleName();
    public static final int REFRESH_SET_COVER = 31;

    private boolean mWaiting;
    private Memory mNewMemory;
    private long[] mHappenTime;// GMT时间
    private String mLocalCoverPath;

    private View mCoverImageLayout;
    private ImageView mCoverImage;
    private View mCoverTip;
    private EditText mNameInputView;
    private TextView mHappenTimeView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNewMemory = new Memory();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_memory, container, false);
        mCoverImageLayout = rootView.findViewById(R.id.memory_cover_img_layout);
        mCoverImage = (ImageView) rootView.findViewById(R.id.memory_cover_img);
        mCoverTip = rootView.findViewById(R.id.memory_cover_add_tip);
        mNameInputView = (EditText) rootView.findViewById(R.id.memory_name_input_view);
        mHappenTimeView = (TextView) rootView.findViewById(R.id.memory_time_view);

        refreshCover();
        refreshHappenTime();
        mNameInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                XLog.d(TAG, "beforeTextChanged().s=" + s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                XLog.d(TAG, "onTextChanged().s=" + s);
                mNewMemory.name = s.toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {
                XLog.d(TAG, "afterTextChanged().s=" + s);
            }
        });
        mNameInputView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == EditorInfo.IME_ACTION_SEARCH) {
                    XLog.d(TAG, "mNameInputView键入enter键");
                    ViewUtil.hideInputMethod(getActivity());
                    return true;
                }
                return false;
            }
        });
        mCoverImageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 选择图片，并上传
                notifyRefresh(REFRESH_SET_COVER, null);
            }
        });
        mHappenTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 弹出日期选择器
                final User user = ((GlobalSource) XDefaultDataRepo.getInstance()
                        .getSource(MyApplication.SOURCE_GLOBAL)).getUser();
                final Calendar nowCal = Calendar.getInstance();
                final Calendar birthCal = TimeUtil.getCalendar(user.bornTime);
                DatePickerDialog datePicker = new DatePickerDialog();
                datePicker.initialize(new DatePickerDialog.OnDateSetListener() {
                                          @Override
                                          public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                                              Calendar targetCal = Calendar.getInstance();
                                              targetCal.set(year, month, day);
                                              // 检查所选日期是否合法
                                              if (targetCal.before(birthCal) || targetCal.after(Calendar.getInstance())) {
                                                  // 选择日期早于出生日期，或晚于当前日期，重选！
                                                  Toast.makeText(datePickerDialog.getActivity(), R.string.error_invalid_date, Toast.LENGTH_SHORT).show();
                                                  ViewUtil.animationShake(mHappenTimeView);
                                              } else {
                                                  if (mHappenTime == null) {
                                                      mHappenTime = new long[2];
                                                  }
                                                  // 记得转换成GMT时间
                                                  mHappenTime[0] = TimeUtil.changeTimeZoneToUTC(targetCal.getTimeInMillis());
                                                  mHappenTime[1] = mHappenTime[0];
                                                  mNewMemory.happenStartTime = mHappenTime[0];
                                                  mNewMemory.happenEndTime = mHappenTime[1];
                                                  refreshHappenTime();
                                              }
                                          }
                                      }, nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH),
                        nowCal.get(Calendar.DAY_OF_MONTH), false);
                datePicker.setYearRange(birthCal.get(Calendar.YEAR), Calendar.getInstance().get(Calendar.YEAR));
                datePicker.show(((AppCompatActivity)getActivity()).getSupportFragmentManager(), "datepicker");
            }
        });
        return rootView;
    }

    private void refreshHappenTime() {
        if (mHappenTime == null) {
            mHappenTimeView.setText(String.format(getResources()
                    .getString(R.string.info_memory_time), "XXXX.XX.XX"));
        } else {
            mHappenTimeView.setText(String.format(getResources().getString(R.string.info_memory_time),
                    XStringUtil.calendar2str(TimeUtil.getCalendar(mHappenTime[0]), ".")));
        }
    }

    private void refreshCover() {
        if (XStringUtil.isEmpty(mLocalCoverPath)) {
            mCoverTip.setVisibility(View.VISIBLE);
            mCoverImage.setImageResource(R.color.gray);
        } else {
            mCoverTip.setVisibility(View.GONE);
            Glide.with(this)
                    .load(mLocalCoverPath)
                    .placeholder(R.color.gray)
                    .error(R.color.gray)
                    .into(mCoverImage);
        }
    }

    public void setCoverPath(String localPath) {
        mLocalCoverPath = localPath;
        refreshCover();
    }

    public void confirm() {
        mNameInputView.setError(null);
        mNewMemory.name = mNameInputView.getText().toString().trim();
        if (XStringUtil.isEmpty(mNewMemory.name)) {
            mNameInputView.setError(getString(R.string.error_field_required));
            mNameInputView.requestFocus();
            return;
        }
        if (mHappenTime == null) {
            Toast.makeText(getActivity(), R.string.error_empty_date, Toast.LENGTH_SHORT).show();
            ViewUtil.animationShake(mHappenTimeView);
            return;
        }
        if (mHappenTime[0] > mHappenTime[1]) {
            Toast.makeText(getActivity(), R.string.error_invalid_date, Toast.LENGTH_SHORT).show();
            ViewUtil.animationShake(mHappenTimeView);
            return;
        }
        if (XStringUtil.isEmpty(mLocalCoverPath)) {
            // 没有设置封面，弹出提示对话框！
            ConfirmDialog.newInstance(
                    getResources().getString(R.string.info_add_memory_no_cover),
                    new DialogListener() {
                        boolean confirm = false;

                        @Override
                        public void onDone(Object... result) {
                            confirm = (boolean) result[0];
                            if (confirm) {
                                addAndUploadMemory();
                            }
                        }

                        @Override
                        public void onDismiss() {
                            if (!confirm) {
                                ViewUtil.animationShake(mCoverImageLayout);
                            }
                        }
                    }).show(getFragmentManager(), ConfirmDialog.TAG);
        } else {
            addAndUploadMemory();
        }
    }

    private void addMemory(final Context context, String memoryName,
                                  long happenStartTime, long happenEndTime,
                                  final ResultListener<Memory> listener) {
        putAsyncTask(MyApplication.getAsyncHttp().execute(
                ApiUtil.addMemory(memoryName, happenStartTime, happenEndTime),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onCancelled() {
                        if (listener != null)
                            listener.onResult(false, null);
                    }

                    @Override
                    public void onNetworkError() {
                        Toast.makeText(context, R.string.error_network, Toast.LENGTH_SHORT).show();
                        if (listener != null)
                            listener.onResult(false, null);
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        Toast.makeText(context, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        if (listener != null)
                            listener.onResult(false, null);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject jsonObject) {
                        Memory memory = Memory.fromJson(jsonObject);
                        if (listener != null)
                            listener.onResult(memory != null, memory);
                    }
                }
        ));
    }

    private void addAndUploadMemory() {
        if (mWaiting)
            return;
        mWaiting = true;
        notifyLoading(true);
        // 先在服务器创建Memory
        addMemory(getActivity(), mNewMemory.name, mNewMemory.happenStartTime,
                mNewMemory.happenEndTime, new ResultListener<Memory>() {
                    @Override
                    public void onResult(boolean result, final Memory memory) {
                        // 创建Memory失败
                        if (!result) {
                            mWaiting = false;
                            notifyLoading(false);
                            return;
                        }
                        // 没有设置封面图片，直接返回创建成功
                        if (XStringUtil.isEmpty(mLocalCoverPath)) {
                            mWaiting = false;
                            notifyFinish(true, memory);
                            return;
                        }
                        // 如果设置了封面图片，则压缩并上传图片
                        ImageUtil.compressAndUploadImage(getActivity(),
                                ApiUtil.getMemoryCoverToken(memory.mid),
                                mLocalCoverPath,
                                new ProgressListener<String>() {
                                    @Override
                                    public void onProgress(String filePath,
                                                           long completeSize, long totalSize) {
                                        // TODO 显示上传进度？
                                    }

                                    @Override
                                    public void onResult(boolean result, final String key) {
                                        if (result) {
                                            // 上传成功，回调服务器
                                            final String url = ApiUtil.PUBLIC_DOMAIN + "/" + key;
                                            final int[] dimension = ImageUtil.getDimension(mLocalCoverPath);
                                            putAsyncTask(MyApplication.getAsyncHttp().execute(
                                                    ApiUtil.updateMemoryCover(memory.mid,
                                                            url, dimension[0], dimension[1]),
                                                    new XAsyncHttp.Listener() {
                                                        @Override
                                                        public void onCancelled() {
                                                            mWaiting = false;
                                                            notifyLoading(false);
                                                        }

                                                        @Override
                                                        public void onNetworkError() {
                                                            // 尽管回调失败，但Memory还是添加成功了！让用户下次再设置封面
                                                            mWaiting = false;
                                                            Toast.makeText(getActivity(), R.string.error_upload_picture,
                                                                    Toast.LENGTH_SHORT).show();
                                                            notifyFinish(true, memory);
                                                        }

                                                        @Override
                                                        public void onFinishError(XHttpResponse xHttpResponse) {
                                                            // 尽管回调失败，但Memory还是添加成功了！让用户下次再设置封面
                                                            mWaiting = false;
                                                            Toast.makeText(getActivity(), R.string.error_upload_picture,
                                                                    Toast.LENGTH_SHORT).show();
                                                            notifyFinish(true, memory);
                                                        }

                                                        @Override
                                                        public void onFinishSuccess(XHttpResponse xHttpResponse, Object o) {
                                                            mWaiting = false;
                                                            memory.coverUrl = url;
                                                            memory.coverWidth = dimension[0];
                                                            memory.coverHeight = dimension[1];
                                                            notifyFinish(true, memory);
                                                        }
                                                    }));
                                        } else {
                                            // 尽管图片上传失败，但Memory还是添加成功了！让用户下次再设置封面
                                            mWaiting = false;
                                            Toast.makeText(getActivity(), R.string.error_upload_picture,
                                                    Toast.LENGTH_SHORT).show();
                                            notifyFinish(true, memory);
                                        }
                                    }
                                });
                    }
                });
    }
}
