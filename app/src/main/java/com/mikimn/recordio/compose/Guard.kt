package com.mikimn.recordio.compose

import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState


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