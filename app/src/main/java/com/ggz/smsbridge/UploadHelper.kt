package com.ggz.smsbridge

import android.util.Base64
import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Properties
import javax.crypto.Cipher
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object UploadHelper {

    fun encrypt(data: String, publicKeyString: String): String? {
        if (publicKeyString.isBlank()) {
            Log.w("UploadHelper", "Public key is blank, returning unencrypted data.")
            return data
        }
        return try {
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKeySpec = X509EncodedKeySpec(Base64.decode(publicKeyString, Base64.DEFAULT))
            val publicKey = keyFactory.generatePublic(publicKeySpec)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            Base64.encodeToString(cipher.doFinal(data.toByteArray()), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("UploadHelper", "Encryption failed", e)
            null
        }
    }

    fun sendEmail(user: String, pass: String, to: String, host: String, port: String, ssl: Boolean, subject: String, body: String) {
        try {
            val props = Properties()
            props["mail.smtp.host"] = host
            props["mail.smtp.port"] = port
            props["mail.smtp.auth"] = "true"
            if (ssl) {
                props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                props["mail.smtp.socketFactory.fallback"] = "false"
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(user, pass)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(user))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            message.subject = subject
            message.setContent(body, "text/html; charset=utf-8")

            Transport.send(message)
            Log.d("UploadHelper", "Email sent successfully via SMTP")
        } catch (e: Exception) {
            Log.e("UploadHelper", "Failed to send email via SMTP", e)
            throw e
        }
    }

    fun sendToServer(data: String, urlString: String) {
        try {
            val url = URL(urlString)
            (url.openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                OutputStreamWriter(outputStream).use { it.write("{\"code\": \"$data\"}") }
                if (responseCode !in 200..299) throw RuntimeException("Server responded with code $responseCode")
            }
            Log.d("UploadHelper", "Sent to server successfully")
        } catch (e: Exception) {
            Log.e("UploadHelper", "Failed to send to server", e)
            throw e
        }
    }
}
