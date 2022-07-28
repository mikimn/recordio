package com.mikimn.recordio.calls

import android.content.Context
import android.content.Intent
import android.net.Uri


/**
 * Launch a phone call to [destination] from this context.
 */
fun Context.launchCall(destination: String) {
    val uri = "tel:${destination.trim()}"
    val intent = Intent(Intent.ACTION_CALL)
    intent.data = Uri.parse(uri)
    startActivity(intent)
}