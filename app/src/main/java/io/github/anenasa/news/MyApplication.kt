package io.github.anenasa.news

import android.content.Context
import android.os.Build
import com.chaquo.python.android.PyApplication
import okhttp3.OkHttpClient
import org.acra.ACRA.init
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class MyApplication : PyApplication() {

    override fun onCreate() {
        super.onCreate()
        cookieJar = TxtCookieJar()
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            val myTrustManager = buildTrustManager(applicationContext)
            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, arrayOf(myTrustManager), null)
            builder.sslSocketFactory(sslContext.socketFactory, myTrustManager)
        }
        okHttpClient = builder.build()
    }

    fun buildTrustManager(context: Context): X509TrustManager {
        val isrgRoot = context.resources.openRawResource(R.raw.isrg_root_x1).use {
            CertificateFactory.getInstance("X.509").generateCertificate(it)
        }

        val defaultTrustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
            init(null as KeyStore?)
            trustManagers.filterIsInstance<X509TrustManager>().first()
        }

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            defaultTrustManager.acceptedIssuers.forEachIndexed { i, cert -> setCertificateEntry("system_$i", cert) }
            setCertificateEntry("isrg_root_x1", isrgRoot)
        }

        return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
            init(keyStore)
            trustManagers.filterIsInstance<X509TrustManager>().first()
        }
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
