package me.buryinmind.android.app.controller;

import com.tj.xengine.core.toolkit.task.XTaskBean;

import me.buryinmind.android.app.model.Secret;

/**
 * Created by jasontujun on 2016/5/26.
 */
public class SecretUploadBean implements XTaskBean {
    private int status;
    private int type;
    public Secret secret;

    public SecretUploadBean(Secret secret) {
        super();
        this.secret = secret;
    }

    @Override
    public String getId() {
        return secret.sid;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int i) {
        status = i;
    }
}
