package io.github.anenasa.news;

import android.content.Context;

import com.chaquo.python.android.PyApplication;

import org.acra.ACRA;

import okhttp3.OkHttpClient;

public class MyApplication extends PyApplication {
    public static final OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }
}