package com.mikimn.recordio

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mikimn.recordio.device.PermissionGuard
import com.mikimn.recordio.layout.AudioPlayer
import com.mikimn.recordio.phone.launchCall


@Composable
fun CallRecordingScreen(
    navController: NavController,
    recordingsViewModel: CallRecordingsViewModel,
    recordingId: Int
) {
    val scaffoldState = rememberScaffoldState()
    var recordingState: CallRecording? by remember { mutableStateOf(null) }

    LaunchedEffect(recordingId) {
        recordingState = recordingsViewModel.findById(recordingId)
    }

    recordingState?.let { callRecording ->
        val context = LocalContext.current
        val recordingFile = callRecording.documentFile(context)
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopBar("Details") {
                    navController.navigateUp()
                }
            }
        ) { innerPadding ->
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.padding(innerPadding),
                color = MaterialTheme.colors.background
            ) {
                Column {
                    RecordingHeader(callRecording)
                    Divider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        text = "Playback",
                        style = MaterialTheme.typography.h5.copy(color = Color.Gray)
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = recordingFile?.name ?: "Unknown File"
                    )
                    AudioPlayer(callRecording.duration)
                    Divider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        text = "Actions",
                        style = MaterialTheme.typography.h5.copy(color = Color.Gray)
                    )
                    RecordingActions(callRecording) { recording, action ->
                        when (action) {
                            RecordingAction.CALL -> launchCall(context, recording.source)
                            RecordingAction.DELETE_RECORDING -> {
                                recordingsViewModel.delete(recording, context)
                                navController.navigateUp()
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RecordingHeader(callRecording: CallRecording) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Recording ${callRecording.id}", style = MaterialTheme.typography.h3)
        Text(text = "Duration: ${callRecording.duration.humanReadable()}")
    }
}


enum class RecordingAction {
    CALL,
    DELETE_RECORDING
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordingActions(
    callRecording: CallRecording,
    onAction: (recording: CallRecording, action: RecordingAction) -> Unit = { _, _ -> }
) {
    Column {
        PermissionGuard(
            android.Manifest.permission.CALL_PHONE,
            action = { onAction(callRecording, RecordingAction.CALL) }
        ) {
            ButtonTile(icon = Icons.Default.Call, text = "Call") { it() }
        }
        ButtonTile(icon = Icons.Default.Delete, text = "Delete Recording") {
            onAction(callRecording, RecordingAction.DELETE_RECORDING)
        }
    }
}


@Composable
fun ButtonTile(icon: ImageVector, text: String, onClick: () -> Unit = {}) {
    Card(
        shape = RectangleShape,
        backgroundColor = Color.Transparent,
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.button
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = "Button Tile")
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(text = text)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewRecordingHeader() {
    RecordingHeader(callRecordingFromId(-1))
}


@Preview(showBackground = true)
@Composable
fun PreviewRecordingPlayback() {
    AudioPlayer(callRecordingFromId(-1).duration)
}


@ExperimentalPermissionsApi
@Preview(showBackground = true)
@Composable
fun PreviewRecordingActions() {
    RecordingActions(callRecordingFromId(-1))
}


@Preview(showBackground = true)
@Composable
fun PreviewButtonTile() {
    ButtonTile(icon = Icons.Default.Star, "A Button")
}