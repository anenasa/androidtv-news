package io.github.anenasa.news

import android.content.Context
import com.chaquo.python.android.PyApplication
import okhttp3.OkHttpClient
import org.acra.ACRA.init

class MyApplication : PyApplication() {
    override fun onCreate() {
        super.onCreate()
        cookieJar = TxtCookieJar()
        okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        init(this)
    }

    companion object {
        lateinit var okHttpClient: OkHttpClient
        lateinit var cookieJar: TxtCookieJar
    }
}
