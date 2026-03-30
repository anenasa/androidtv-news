package io.github.anenasa.news

import android.content.Context
import android.os.Build
import com.chaquo.python.android.PyApplication
import okhttp3.OkHttpClient
import org.acra.ACRA.init
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class MyApplication : PyApplication() {
    override fun onCreate() {
        super.onCreate()
        cookieJar = TxtCookieJar()
        okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
        // Dirty hack for https://github.com/anenasa/androidtv-news/issues/6
        // java.security.cert.CertPathValidatorException:
        // Trust anchor for certification path not found.
        // Only happens on Android 7 and below.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            class InsecureTrustManager : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val myTrustManager = InsecureTrustManager()
            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, arrayOf(myTrustManager), null)
            insecureClient = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, myTrustManager)
                .build()
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        init(this)
    }

    companion object {
        lateinit var okHttpClient: OkHttpClient
        lateinit var cookieJar: TxtCookieJar
        lateinit var insecureClient: OkHttpClient
    }
}
