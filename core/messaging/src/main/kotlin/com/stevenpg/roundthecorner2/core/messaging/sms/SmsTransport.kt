package com.stevenpg.roundthecorner2.core.messaging.sms

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The two [SmsManager] calls we need, behind an interface so the sending /
 * confirmation logic in [AutomaticSmsSender] can be unit-tested without a
 * real radio.
 */
interface SmsTransport {
    /**
     * Splits [message] into carrier-sized parts.
     * @throws UnsupportedOperationException if the device can't send SMS.
     */
    fun divideMessage(message: String): ArrayList<String>

    fun sendMultipart(destination: String, parts: ArrayList<String>, sentIntents: ArrayList<PendingIntent>)
}

class AndroidSmsTransport @Inject constructor(
    @ApplicationContext private val context: Context,
) : SmsTransport {

    override fun divideMessage(message: String): ArrayList<String> = smsManager().divideMessage(message)

    @SuppressLint("MissingPermission") // Callers check SEND_SMS first.
    override fun sendMultipart(destination: String, parts: ArrayList<String>, sentIntents: ArrayList<PendingIntent>) {
        smsManager().sendMultipartTextMessage(destination, null, parts, sentIntents, null)
    }

    /** Null from getSystemService means the device has no telephony at all. */
    @Suppress("DEPRECATION") // getDefault() is the only option below API 31.
    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
                ?: throw UnsupportedOperationException("No SmsManager on this device")
        } else {
            SmsManager.getDefault()
        }
}
