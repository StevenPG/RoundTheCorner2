package com.stevenpg.roundthecorner2.core.messaging.sms

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Normalises a phone number as typed ("(555) 555-0100") into E.164
 * ("+15555550100") using the SIM/network country, falling back to the
 * device locale. If that fails, strips formatting and lets the carrier decide.
 */
internal fun normalizePhoneNumber(context: Context, raw: String): String {
    val telephony = context.getSystemService(TelephonyManager::class.java)
    val country = listOfNotNull(telephony?.networkCountryIso, telephony?.simCountryIso, Locale.getDefault().country)
        .firstOrNull { it.isNotBlank() }
        ?.uppercase(Locale.ROOT)
    val e164 = country?.let { PhoneNumberUtils.formatNumberToE164(raw, it) }
    return e164 ?: PhoneNumberUtils.normalizeNumber(raw)
}
