package org.tera.plugins.livy.actions

import java.lang.Exception
import java.net.URL
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun main(args: Array<String>) {

    // TODO need to change build.gradle for this and add Kotlin runtime ..
    println("Hello World!")
    // disable certificate verification. TODO think of better workaround
//    val trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(
//            object: X509TrustManager {
//                override fun getAcceptedIssuers(): Array<X509Certificate?>? {
//                    return null
//                }
//
//                override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {
//                }
//
//                override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {
//                }
//            }
//    )
//
//    // Install the all-trusting trust manager
//    try {
//        val sc: SSLContext = SSLContext.getInstance("SSL")
//        sc.init(null, trustAllCerts, SecureRandom())
//        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//
//    val selectedText = "1 + 1"
//    val url = URL("https://livy-dev.service.ckd.dns.teralytics.net/sessions/467/statements")
//    with(url.openConnection() as HttpsURLConnection) {
//        requestMethod = "POST"
//        setRequestProperty("Content-Type", "application/json")
//        doOutput = true
//
//        // TODO escape selectedText
//        // TODO we get http 400, bad request. Find out why
//        val jsonInputString: String = "{\"code\": \"" + selectedText + "\"}"
//        outputStream.use { os ->
//            val input: ByteArray = jsonInputString.toByteArray(Charset.forName("utf-8"))
//            os.write(input, 0, input.size)
//        }
//
//        // TODO find out where to put status updates and stuff
//        println("\nSent 'POST' request to URL : $url; Response Code : $responseCode")
//
//        inputStream.bufferedReader().use {
//            it.lines().forEach { line ->
//                println(line)
//            }
//        }
//    }
}