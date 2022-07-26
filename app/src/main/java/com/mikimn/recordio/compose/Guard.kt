package com.mikimn.recordio.compose

import android.accessibilityservice.AccessibilityService
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.mikimn.recordio.device.checkAccessibilityService


interface GuardedState<T> {
    val flag: Boolean
    fun launch()
}

@ExperimentalPermissionsApi
class GuardedPermissionState constructor(
    permissionState: PermissionState
): GuardedState<PermissionState>, PermissionState by permissionState {
    override val flag: Boolean
        get() = hasPermission

    override fun launch() = launchPermissionRequest()
}

@ExperimentalPermissionsApi
class GuardedMultiPermissionState constructor(
    permissionState: MultiplePermissionsState
): GuardedState<PermissionState>, MultiplePermissionsState by permissionState {
    override val flag: Boolean
        get() = allPermissionsGranted

    override fun launch() = launchMultiplePermissionRequest()
}

class GuardedAccessibilityServiceState<T: AccessibilityService>(
    private val context: Context,
    private val clazz: Class<T>
): GuardedState<T> {
    override val flag: Boolean
        get() = checkAccessibilityService(context, clazz)

    override fun launch() {
        TODO("Not yet implemented")
    }
}

@Composable
fun <State> Guard(
    state: GuardedState<State>,
    action: () -> Unit,
    contents: @Composable (protectedAction: () -> Unit) -> Unit
) {
    // val callPermissionState = rememberPermissionState(permission)
    var shouldPerformAction by remember { mutableStateOf(false) }

    LaunchedEffect(state.flag) {
        if (shouldPerformAction && state.flag) {
            action()
            shouldPerformAction = false
        }
    }

    val protectedAction = {
        if (state.flag) {
            action()
        } else {
            shouldPerformAction = true
            state.launch()
            // callPermissionState.launchPermissionRequest()
        }
    }
    return contents(protectedAction)
}