package me.buryinmind.android.app.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.tj.xengine.android.network.http.handler.XJsonArrayHandler;
import com.tj.xengine.android.network.http.handler.XJsonObjectHandler;
import com.tj.xengine.core.data.XDefaultDataRepo;
import com.tj.xengine.core.network.http.XAsyncHttp;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

import me.buryinmind.android.app.MyApplication;
import me.buryinmind.android.app.R;
import me.buryinmind.android.app.data.GlobalSource;
import me.buryinmind.android.app.fragment.ActivateAccountFragment;
import me.buryinmind.android.app.fragment.AnswerAccountFragment;
import me.buryinmind.android.app.fragment.ChooseAccountFragment;
import me.buryinmind.android.app.fragment.FragmentInteractListener;
import me.buryinmind.android.app.fragment.LoginAccountFragment;
import me.buryinmind.android.app.fragment.SearchAccountFragment;
import me.buryinmind.android.app.model.MemoryGift;
import me.buryinmind.android.app.model.User;
import me.buryinmind.android.app.util.ApiUtil;
import me.buryinmind.android.app.util.ViewUtil;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final int STEP_START = 1;
    private static final int STEP_LOGIN_LIST = 11;
    private static final int STEP_LOGIN_HAS_USER = 12;
    private static final int STEP_ACTIVATE_USERS = 21;
    private static final int STEP_ACTIVATE_GIFTS = 22;
    private static final int STEP_ACTIVATE_ANSWER = 23;// 选中一个种子账号，准备回答问题
    private static final int STEP_ACTIVATE_PASSWORD = 24;// 选中一个种子账号设置邮箱密码激活

    // UI references.
    private View mProgressView;
    private View mLoginFormView;
    private boolean mWaiting;

    // temp data
    private List<User> activeAccounts;
    private List<User> seedAccounts;
    private User chooseUser;
    private String lastUserId;
    private String lastUserName;
    private List<String> lastUserDescription;
    private List<MemoryGift> gifts;
    private MemoryGift chooseGift;
    private String giftAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView = findViewById(R.id.content_layout);

        if (savedInstanceState == null) {
            GlobalSource source = (GlobalSource) XDefaultDataRepo
                    .getInstance().getSource(MyApplication.SOURCE_GLOBAL);
            if (!XStringUtil.isEmpty(source.getLastUserId()) &&
                    !XStringUtil.isEmpty(source.getUserToken()) &&
                    System.currentTimeMillis() - source.getLastTokenTime()
                            < GlobalSource.DEFAULT_TOKEN_DURATION) {
                lastUserId = source.getLastUserId();
                lastUserName = source.getLastUserName();
                lastUserDescription = source.getLastUserDescriptions();
                // 如果有上次的token，则尝试快速验证Token登录
                checkTokenLogin(source.getLastUserId(), source.getUserToken());
            } else {
                if (!XStringUtil.isEmpty(source.getLastUserId())) {
                    // 有记录上次登录的账号，则直接输入密码即可
                    chooseUser = null;
                    lastUserId = source.getLastUserId();
                    lastUserName = source.getLastUserName();
                    lastUserDescription = source.getLastUserDescriptions();
                    getFragmentManager().beginTransaction()
                            .add(R.id.content_layout, createFragment(STEP_LOGIN_HAS_USER))
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .commit();
                } else {
                    // 没有任何记录，从搜索账号界面开始
                    getFragmentManager().beginTransaction()
                            .add(R.id.content_layout, createFragment(STEP_START))
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .commit();
                }
            }
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                    return false;
                } else {
                    // 点击2次退出
                    long currentTime = System.currentTimeMillis();
                    GlobalSource source = (GlobalSource) XDefaultDataRepo
                            .getInstance().getSource(MyApplication.SOURCE_GLOBAL);
                    if (currentTime - source.getLastBackTime() <= GlobalSource.PRESS_BACK_INTERVAL) {
//                        SystemMgr.getInstance().clearSystem();
                        finish();
                    } else {
                        source.setLastBackTime(currentTime);
                        Toast.makeText(this, R.string.info_press_back_again, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            case KeyEvent.KEYCODE_MENU:
                break;
        }
        return false;
    }

    private Fragment createFragment(final int step) {
        Bundle arguments = new Bundle();
        Fragment fragment = null;
        switch (step) {
            case STEP_START:
                fragment = new SearchAccountFragment();
                arguments.putSerializable(FragmentInteractListener.KEY,
                        new FragmentInteractListener() {
                            @Override
                            public void onLoading() {
                                showProgress(true);
                            }

                            @Override
                            public void onBack() {
                                showProgress(false);
                                if (getFragmentManager().getBackStackEntryCount() > 0) {
                                    getFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result && data != null) {
                                    Pair<Boolean, List> data2 = (Pair<Boolean, List>) data;
                                    if (data2.first) {// 点击注册
                                        seedAccounts = (List<User>)data2.second;
                                        getFragmentManager().beginTransaction()
                                                .replace(R.id.content_layout, createFragment(STEP_ACTIVATE_USERS))
                                                .addToBackStack(null)
                                                .commit();
                                    } else {// 点击登陆
                                        activeAccounts = (List<User>)data2.second;
                                        getFragmentManager().beginTransaction()
                                                .replace(R.id.content_layout, createFragment(STEP_LOGIN_LIST))
                                                .addToBackStack(null)
                                                .commit();
                                    }
                                }
                            }
                        });
                break;
            case STEP_LOGIN_LIST:
                fragment = new ChooseAccountFragment();
                arguments.putInt(ChooseAccountFragment.KEY_TYPE,
                        ChooseAccountFragment.TYPE_ACTIVE_ACCOUNT_LIST);
                arguments.putSerializable(ChooseAccountFragment.KEY_ACCOUNTS,
                        (Serializable) activeAccounts);
                arguments.putSerializable(FragmentInteractListener.KEY,
                        new FragmentInteractListener() {
                            @Override
                            public void onLoading() {
                                showProgress(true);
                            }

                            @Override
                            public void onBack() {
                                showProgress(false);
                                if (getFragmentManager().getBackStackEntryCount() > 0) {
                                    getFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result && data != null) {
                                    chooseUser = (User)data;
                                    getFragmentManager().beginTransaction()
                                            .replace(R.id.content_layout, createFragment(STEP_LOGIN_HAS_USER))
                                            .addToBackStack(null)
                                            .commit();

                                }
                            }
                        });
                break;
            case STEP_LOGIN_HAS_USER:
                fragment = new LoginAccountFragment();
                if (chooseUser != null) {
                    arguments.putString(LoginAccountFragment.KEY_USER_ID, chooseUser.uid);
                    arguments.putString(LoginAccountFragment.KEY_USER_NAME, chooseUser.name);
                    arguments.putSerializable(LoginAccountFragment.KEY_USER_DESCRIPTION,
                            (Serializable) chooseUser.descriptions);
                } else {
                    arguments.putString(LoginAccountFragment.KEY_USER_ID, lastUserId);
                    arguments.putString(LoginAccountFragment.KEY_USER_NAME, lastUserName);
                    arguments.putSerializable(LoginAccountFragment.KEY_USER_DESCRIPTION,
                            (Serializable) lastUserDescription);
                }
                arguments.putSerializable(FragmentInteractListener.KEY,
                        new FragmentInteractListener() {
                            @Override
                            public void onLoading() {
                                showProgress(true);
                            }

                            @Override
                            public void onBack() {
                                showProgress(false);
                                if (getFragmentManager().getBackStackEntryCount() > 0) {
                                    getFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result) {
                                    // 进入MainActivity
                                    enterMainActivity();
                                }
                            }
                        });
                break;
            case STEP_ACTIVATE_USERS:
                fragment = new ChooseAccountFragment();
                arguments.putInt(ChooseAccountFragment.KEY_TYPE,
                        ChooseAccountFragment.TYPE_SEED_ACCOUNT_LIST);
                arguments.putSerializable(ChooseAccountFragment.KEY_ACCOUNTS,
                        (Serializable) seedAccounts);
                arguments.putSerializable(FragmentInteractListener.KEY,
                        new FragmentInteractListener() {
                            @Override
                            public void onLoading() {
                                showProgress(true);
                            }

                            @Override
                            public void onBack() {
                                showProgress(false);
                                if (getFragmentManager().getBackStackEntryCount() > 0) {
                                    getFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result && data != null) {
                                    chooseUser = (User)data;
                                    getSeedAccountDetail(chooseUser.uid,
                                            new XAsyncHttp.Listener<List<MemoryGift>>() {
                                                @Override
                                                public void onNetworkError() {
                                                }

                                                @Override
                                                public void onFinishError(XHttpResponse xHttpResponse) {
                                                }

                                                @Override
                                                public void onFinishSuccess(XHttpResponse xHttpResponse, List<MemoryGift> memoryGifts) {
                                                    gifts = memoryGifts;
                                                    getFragmentManager().beginTransaction()
                                                            .replace(R.id.content_layout, createFragment(STEP_ACTIVATE_GIFTS))
                                                            .addToBackStack(null)
                                                            .commit();
                                                }
                                            });
                                }
                            }
                        });
                break;
            case STEP_ACTIVATE_GIFTS:
                fragment = new ChooseAccountFragment();
                arguments.putInt(ChooseAccountFragment.KEY_TYPE,
                        ChooseAccountFragment.TYPE_GIFT_LIST);
                arguments.putSerializable(ChooseAccountFragment.KEY_GIFTS,
                        (Serializable) gifts);
                arguments.putSerializable(ChooseAccountFragment.KEY_CHOOSE_USER,
                        chooseUser);
                arguments.putSerializable(FragmentInteractListener.KEY,
                        new FragmentInteractListener() {
                            @Override
                            public void onLoading() {
                                showProgress(true);
                            }

                            @Override
                            public void onBack() {
                                showProgress(false);
                                if (getFragmentManager().getBackStackEntryCount() > 0) {
                                    getFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result && data != null) {
                                    chooseGift = (MemoryGift)data;
                                    getFragmentManager().beginTransaction()
                                            .replace(R.id.content_layout, createFragment(STEP_ACTIVATE_ANSWER))
                                            .addToBackStack(null)
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                            .commit();
                                }
                            }
                        });
                break;
            case STEP_ACTIVATE_ANSWER:
                fragment = new AnswerAccountFragment();
                arguments.putSerializable(AnswerAccountFragment.KEY_GIFT, chooseGift);
                arguments.putSerializable(FragmentInteractListener.KEY,
                        new FragmentInteractListener() {
                            @Override
                            public void onLoading() {
                                showProgress(true);
                            }

                            @Override
                            public void onBack() {
                                showProgress(false);
                                if (getFragmentManager().getBackStackEntryCount() > 0) {
                                    getFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result) {
                                    giftAnswer = (String) data;
                                    getFragmentManager().beginTransaction()
                                            .replace(R.id.content_layout, createFragment(STEP_ACTIVATE_PASSWORD))
                                            .addToBackStack(null)
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                            .commit();
                                }
                            }
                        });
                break;
            case STEP_ACTIVATE_PASSWORD:
                fragment = new ActivateAccountFragment();
                arguments.putSerializable(ActivateAccountFragment.KEY_GIFT, chooseGift);
                arguments.putSerializable(ActivateAccountFragment.KEY_ANSWER, giftAnswer);
                arguments.putSerializable(FragmentInteractListener.KEY,
                        new FragmentInteractListener() {
                            @Override
                            public void onLoading() {
                                showProgress(true);
                            }

                            @Override
                            public void onBack() {
                                showProgress(false);
                                if (getFragmentManager().getBackStackEntryCount() > 0) {
                                    getFragmentManager().popBackStack();
                                }
                            }

                            @Override
                            public void onFinish(boolean result, Object data) {
                                showProgress(false);
                                if (result) {
                                    if (data == null) {
                                        // 自动登录失败，需要手动登录..
                                        lastUserId = chooseGift.receiverId;
                                        lastUserName = chooseGift.receiverName;
                                        lastUserDescription = chooseGift.receiverDescription;
                                        getFragmentManager().beginTransaction()
                                                .replace(R.id.content_layout, createFragment(STEP_LOGIN_HAS_USER))
                                                .addToBackStack(null)
                                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                                .commit();
                                    } else {
                                        // 进入MainActivity
                                        enterMainActivity();
                                    }
                                }
                            }
                        });
                break;
        }
        if (fragment != null) {
            fragment.setArguments(arguments);
        }
        return fragment;
    }


    private void showProgress(final boolean show) {
        ViewUtil.animateFadeInOut(mLoginFormView, show);
        ViewUtil.animateFadeInOut(mProgressView, !show);
    }

    private void checkTokenLogin(String userId, String token) {
        if (mWaiting)
            return;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.checkToken(userId, token),
                new XJsonObjectHandler(),
                new XAsyncHttp.Listener<JSONObject>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                        showProgress(false);
                        getFragmentManager().beginTransaction()
                                .add(R.id.content_layout, createFragment(STEP_LOGIN_HAS_USER))
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .commit();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(LoginActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        showProgress(false);
                        getFragmentManager().beginTransaction()
                                .add(R.id.content_layout, createFragment(STEP_LOGIN_HAS_USER))
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .commit();
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONObject obj) {
                        mWaiting = false;
                        String token = null;
                        User user = null;
                        try {
                            if (obj != null) {
                                token = obj.getString("token");
                                user = User.fromJson(obj.getJSONObject("user"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (XStringUtil.isEmpty(token) || user == null) {
                            Toast.makeText(LoginActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        } else {
                            GlobalSource source = (GlobalSource) XDefaultDataRepo.getInstance().getSource(MyApplication.SOURCE_GLOBAL);
                            source.loginSuccess(user, token);
                            enterMainActivity();
                        }
                    }
                });
    }

    private void getSeedAccountDetail(String userId, final XAsyncHttp.Listener<List<MemoryGift>> listener) {
        if (mWaiting)
            return;
        mWaiting = true;
        showProgress(true);
        MyApplication.getAsyncHttp().execute(
                ApiUtil.getSeedUserDetail(userId),
                new XJsonArrayHandler(),
                new XAsyncHttp.Listener<JSONArray>() {
                    @Override
                    public void onNetworkError() {
                        mWaiting = false;
                        Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                        showProgress(false);
                        if (listener != null)
                            listener.onNetworkError();
                    }

                    @Override
                    public void onFinishError(XHttpResponse xHttpResponse) {
                        mWaiting = false;
                        Toast.makeText(LoginActivity.this, R.string.error_api_return_failed, Toast.LENGTH_SHORT).show();
                        showProgress(false);
                        if (listener != null)
                            listener.onFinishError(xHttpResponse);
                    }

                    @Override
                    public void onFinishSuccess(XHttpResponse xHttpResponse, JSONArray ja) {
                        mWaiting = false;
                        showProgress(false);
                        List<MemoryGift> gifts = MemoryGift.fromJson(ja);
                        if (listener != null)
                            listener.onFinishSuccess(xHttpResponse, gifts);
                    }
                });
    }

    private void enterMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

