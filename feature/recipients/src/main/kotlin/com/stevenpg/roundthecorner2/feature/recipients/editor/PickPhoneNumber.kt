package com.stevenpg.roundthecorner2.feature.recipients.editor

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opens the system contact picker filtered to phone numbers.
 *
 * Picking from `Phone.CONTENT_URI` grants temporary read access to just the
 * chosen row, so the app never needs the broad READ_CONTACTS permission.
 */
internal class PickPhoneNumber : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}

internal data class PickedContact(val displayName: String?, val phoneNumber: String)

/** Reads the picked row (display name + number) off the main thread. */
internal suspend fun ContentResolver.readPickedContact(uri: Uri): PickedContact? = withContext(Dispatchers.IO) {
    runCatching {
        query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val number = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: return@use null
            PickedContact(displayName = cursor.getString(0), phoneNumber = number)
        }
    }.getOrNull()
}
