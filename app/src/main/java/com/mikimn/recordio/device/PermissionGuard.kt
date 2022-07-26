package com.mikimn.recordio.device

import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.mikimn.recordio.compose.Guard
import com.mikimn.recordio.compose.GuardedMultiPermissionState
import com.mikimn.recordio.compose.GuardedPermissionState

@ExperimentalPermissionsApi
@Composable
fun rememberGuardedPermissionState(permission: String): GuardedPermissionState = GuardedPermissionState(
    rememberPermissionState(permission = permission)
)

@ExperimentalPermissionsApi
@Composable
fun rememberGuardedPermissionState(vararg permission: String): GuardedMultiPermissionState = GuardedMultiPermissionState(
    rememberMultiplePermissionsState(permission.toList())
)

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
    val callPermissionState = rememberGuardedPermissionState(permission)
    Guard(callPermissionState, action, contents)
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGuard(
    vararg permission: String,
    contents: @Composable () -> Unit
) {
    val callPermissionState = rememberMultiplePermissionsState(permission.toList())

    LaunchedEffect(callPermissionState.allPermissionsGranted) {
        if (!callPermissionState.allPermissionsGranted) {
            callPermissionState.launchMultiplePermissionRequest()
        }
    }

    if (callPermissionState.allPermissionsGranted) {
        contents()
    }
}