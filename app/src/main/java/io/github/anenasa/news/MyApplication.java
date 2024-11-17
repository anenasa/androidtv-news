package io.github.anenasa.news;

import android.content.Context;

import com.chaquo.python.android.PyApplication;

import org.acra.ACRA;

public class MyApplication extends PyApplication {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }
}