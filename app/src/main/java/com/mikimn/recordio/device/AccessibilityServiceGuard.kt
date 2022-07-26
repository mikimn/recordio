package com.mikimn.recordio.device

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mikimn.recordio.compose.Guard
import com.mikimn.recordio.compose.GuardedAccessibilityServiceState

inline fun <reified T: AccessibilityService> checkAccessibilityService(context: Context): Boolean {
    return checkAccessibilityService(context, T::class.java)
}

/**
 * Check that the accessibility service is enabled.
 *
 * @param context The application's context
 * @return true is the accessibility service has been enabled through the settings, false otherwise
 */
fun <T: AccessibilityService> checkAccessibilityService(context: Context, clazz: Class<T>): Boolean {
    val service = context.packageName + "/" + clazz.canonicalName
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
        throw IllegalStateException("${clazz.simpleName} is not an accessibility service")
    }
}

fun launchAccessibilityServiceSettings(context: Context) {
    val goToSettings = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    goToSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
    context.startActivity(goToSettings)
}

@Composable
fun <T : AccessibilityService> rememberAccessibilityServiceGuardedState(clazz: Class<T>) = GuardedAccessibilityServiceState(
    LocalContext.current,
    clazz
)

@Composable
fun <T : AccessibilityService> AccessibilityServiceGuard(
    clazz: Class<T>,
    action: () -> Unit,
    contents: @Composable (protectedAction: () -> Unit) -> Unit
) {
    val guardState = rememberAccessibilityServiceGuardedState(clazz)
    Guard(guardState, action, contents)
}
