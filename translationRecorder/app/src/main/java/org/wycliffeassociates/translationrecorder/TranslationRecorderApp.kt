package org.wycliffeassociates.translationrecorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.okhttp.OkHttpStack
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Created by sarabiaj on 11/28/2017.
 */
@HiltAndroidApp
class TranslationRecorderApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "TranslationRecorderUpload"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        UploadServiceConfig.httpStack = OkHttpStack(okHttpClient)
        UploadServiceConfig.initialize(this, NOTIFICATION_CHANNEL_ID, false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Upload Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for file uploads"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private val okHttpClient: OkHttpClient
        get() = Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS) // unlimited timeout
            .readTimeout(
                30,
                TimeUnit.SECONDS
            ) //.sslSocketFactory(getSSLSocketFactory())
            .build()

    @get:Throws(
        CertificateException::class,
        IOException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        KeyManagementException::class
    )
    val sSLSocketFactory: SSLSocketFactory
        get() {
            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            val cf =
                CertificateFactory.getInstance("X.509")
            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
            val caInput: InputStream =
                BufferedInputStream(assets.open("rootCA.crt"))
            val ca: Certificate
            try {
                ca = cf.generateCertificate(caInput)
                println("ca=" + (ca as X509Certificate).subjectDN)
            } finally {
                caInput.close()
            }

            // Create a KeyStore containing our trusted CAs
            val keyStoreType = KeyStore.getDefaultType()
            val keyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ca", ca)

            // Create a TrustManager that trusts the CAs in our KeyStore
            val tmfAlgorithm =
                TrustManagerFactory.getDefaultAlgorithm()
            val tmf =
                TrustManagerFactory.getInstance(tmfAlgorithm)
            tmf.init(keyStore)

            // Create an SSLContext that uses our TrustManager
            val context = SSLContext.getInstance("TLS")
            context.init(null, tmf.trustManagers, null)
            return context.socketFactory
        }
}
