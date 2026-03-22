package io.github.anenasa.news;

import android.content.Context;

import com.chaquo.python.android.PyApplication;

import org.acra.ACRA;

import java.io.File;

import okhttp3.OkHttpClient;

public class MyApplication extends PyApplication {
    public static OkHttpClient okHttpClient;
    public static TxtCookieJar cookieJar;

    @Override
    public void onCreate() {
        super.onCreate();
        cookieJar = new TxtCookieJar(new File(getExternalFilesDir(null), "cookies.txt"));
        okHttpClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);
    }
}