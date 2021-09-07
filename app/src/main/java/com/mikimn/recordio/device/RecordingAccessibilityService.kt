package com.mikimn.recordio.device

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.mikimn.recordio.phone.PhoneCall
import com.mikimn.recordio.phone.handleIncomingCall
import com.mikimn.recordio.phone.handleOutgoingCall
import com.mikimn.recordio.phone.phoneCallInformation

class RecordingAccessibilityService : AccessibilityService() {
    override fun onCreate() {
        super.onCreate()
        bindCallBroadcastReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindCallBroadcastReceiver()
    }

    private fun bindCallBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL)
        registerReceiver(CallBroadcastReceiver(), filter)
    }

    private fun unbindCallBroadcastReceiver() {
        unregisterReceiver(CallBroadcastReceiver())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // TODO Stub
    }

    override fun onInterrupt() {
        // TODO Stub
    }

    inner class CallBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(_c: Context, intent: Intent) {
            val context = this@RecordingAccessibilityService
            when (val phoneCall = intent.phoneCallInformation()) {
                is PhoneCall.Incoming -> handleIncomingCall(context, phoneCall)
                is PhoneCall.Outgoing -> handleOutgoingCall(context, phoneCall)
            }
        }
    }
}


/**
 * Check that the accessibility service is enabled.
 *
 * @param context The application's context
 * @return true is the accessibility service has been enabled through the settings, false otherwise
 */
inline fun <reified T: AccessibilityService> checkAccessibilityService(context: Context): Boolean {
    val service = context.packageName + "/" + T::class.java.canonicalName
    try {
        val accessibilityEnabled: Boolean = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        ) == 1

        val stringSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled) {
            val settingValue = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                stringSplitter.setString(settingValue)
                while (stringSplitter.hasNext()) {
                    val accessibilityService = stringSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    } catch (e: Settings.SettingNotFoundException) {
        throw IllegalStateException("${T::class.simpleName} is not an accessibility service")
    }
}