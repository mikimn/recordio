package com.mikimn.recordio.device

import android.util.Log
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState


/**
 * Allows guarding an action which requires a [permission]. This composable accepts a [contents]
 * block, which supplies its composed elements with a protected action. Elements attempting to use
 * [action] should use the supplied protected action. Calling it will check whether the given
 * permission is granted, and if so, call [action]. Otherwise, it will first ask the user for the
 * permission and will invoke [action] only if the user grants the permission.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGuard(
    permission: String,
    action: () -> Unit,
    contents: @Composable (protectedAction: () -> Unit) -> Unit
) {
    val callPermissionState = rememberPermissionState(permission)
    var shouldPerformAction by remember { mutableStateOf(false) }

    LaunchedEffect(callPermissionState.hasPermission) {
        if (shouldPerformAction && callPermissionState.hasPermission) {
            action()
            shouldPerformAction = false
        }
    }

    val protectedAction = {
        if (callPermissionState.hasPermission) {
            action()
        } else {
            shouldPerformAction = true
            callPermissionState.launchPermissionRequest()
        }
    }
    return contents(protectedAction)
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGuard(
    vararg permission: String,
    contents: @Composable () -> Unit
) {
    val callPermissionState = rememberMultiplePermissionsState(permission.toList())
    // var shouldPerformAction by remember { mutableStateOf(false) }

    LaunchedEffect(callPermissionState.allPermissionsGranted) {
        if (!callPermissionState.allPermissionsGranted) {
            callPermissionState.launchMultiplePermissionRequest()
        }
    }

    if (callPermissionState.allPermissionsGranted) {
        contents()
    }
}