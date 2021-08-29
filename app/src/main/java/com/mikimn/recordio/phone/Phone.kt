package com.mikimn.recordio.phone

import android.content.Context
import android.content.Intent
import android.net.Uri


fun launchCall(context: Context, destination: String) {
    val uri = "tel:${destination.trim()}"
    val intent = Intent(Intent.ACTION_CALL)
    intent.data = Uri.parse(uri)
    context.startActivity(intent)
}