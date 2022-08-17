package com.mikimn.recordio.system

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


/**
 * Check whether the given [permissions] are all granted by the application, returning
 * true if they are or false otherwise.
 */
fun Context.checkPermissions(vararg permissions: String): Boolean {
    return listOf(permissions.forEach {
        ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }).any()
}