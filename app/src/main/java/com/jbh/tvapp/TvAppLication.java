package com.jbh.tvapp;

import android.app.Application;

import cn.bmob.v3.Bmob;

import static com.jbh.tvapp.config.Constant.APP_KEY;


public class TvAppLication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //第一：默认初始化
        Bmob.initialize(this, APP_KEY);

    }
}
