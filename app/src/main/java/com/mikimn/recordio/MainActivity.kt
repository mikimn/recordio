package com.mikimn.recordio

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mikimn.recordio.db.AppDatabase
import com.mikimn.recordio.device.PermissionGuard
import com.mikimn.recordio.layout.rememberDirectoryPickerState
import com.mikimn.recordio.ui.theme.RecordioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import android.content.Intent
import android.provider.Settings
import com.mikimn.recordio.device.RecordingAccessibilityService
import com.mikimn.recordio.device.checkAccessibilityService


class MainActivity : ComponentActivity() {

    private val applicationScope = CoroutineScope(SupervisorJob())
    private val recordingsViewModel: CallRecordingsViewModel by viewModels {
        CallRecordingsViewModelFactory(
            CallRecordingsRepository(
                AppDatabase.instance(applicationContext, applicationScope).callRecordingsDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RecordioTheme(darkTheme = false) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") { MainScreen(navController, recordingsViewModel) }
                    composable(
                        "recording/{recordingId}",
                    ) { backStackEntry ->
                        CallRecordingScreen(
                            navController,
                            recordingsViewModel,
                            backStackEntry.arguments?.getString("recordingId")?.toInt() ?: -1
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!checkAccessibilityService<RecordingAccessibilityService>(this)) {
            val goToSettings = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            goToSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(goToSettings)
        }
    }
}

@Composable
fun MainScreen(
    navController: NavController,
    recordingsViewModel: CallRecordingsViewModel
) {
    val scaffoldState = rememberScaffoldState()
    val snackbarCoroutineScope = rememberCoroutineScope()
    val recordings by recordingsViewModel.recordings.observeAsState(emptyList())

    PermissionGuard(
        Manifest.permission.PROCESS_OUTGOING_CALLS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                val context = LocalContext.current
                SelectDirectoryFloatingActionButton {
                    val sharedPrefs =
                        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString("saveFolder", it.toString()).apply()
                }
            },
            topBar = {
                TopBar("Recordio")
            }
        ) { innerPadding ->
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.padding(innerPadding),
                color = MaterialTheme.colors.background
            ) {
                RecordingList(recordings = recordings) { _, recording ->
                    snackbarCoroutineScope.launch {
                        navController.navigate("recording/${recording.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = onBack?.let {
            {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
                }
            }
        }
    )
}

@Composable
fun SelectDirectoryFloatingActionButton(onDirectorySelected: (Uri) -> Unit = {}) {
    var requestSelectFile by remember { mutableStateOf(false) }

    val filePickerState = rememberDirectoryPickerState()
    val context = LocalContext.current

    LaunchedEffect(filePickerState.uri) {
        if (requestSelectFile) {
            if (filePickerState.uri != null) {
                val uri = filePickerState.directory(context).uri
                onDirectorySelected(uri)
            }
            requestSelectFile = false
        }
    }

    ExtendedFloatingActionButton(
        onClick = {
            if (!requestSelectFile) {
                filePickerState.launch()
                requestSelectFile = true
            }
        },
        text = { Text(text = "Select Directory") },
        icon = {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Recording"
            )
        }
    )
}


@Composable
fun CallList(
    recordings: List<RegisteredCall>,
    onClick: (index: Int, recording: RegisteredCall) -> Unit,
) {
    val scrollState = rememberLazyListState()

    LazyColumn(state = scrollState) {
        itemsIndexed(recordings) { index, recording ->
            CallItem(recording) { onClick(index, recording) }
        }
    }
}


@Composable
fun CallType.Icon() {
    return when {
        this == CallType.INCOMING -> Icon(
            Icons.Default.CallMade,
            contentDescription = "Incoming Call",
            tint = androidx.compose.ui.graphics.Color.Green
        )
        this == CallType.OUTGOING -> Icon(
            Icons.Default.CallReceived,
            contentDescription = "Outgoing Call",
            tint = androidx.compose.ui.graphics.Color.Blue
        )
        else -> Icon(
            Icons.Default.CallMissed,
            contentDescription = "Missed Call",
            tint = androidx.compose.ui.graphics.Color.Blue
        )
    }
}


@Composable
fun CallItem(
    registeredCall: RegisteredCall,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                registeredCall.callType.Icon()
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = registeredCall.source)
                Text(
                    text = registeredCall.duration.humanReadable(),
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CallItemPreview() {
    CallItem(
        registeredCall = callFromId(0)
    )
}


@Preview
@Composable
fun DirectoryFABPreview() {
    SelectDirectoryFloatingActionButton()
}


fun Duration.timerFormatted(): String {
    val hours = toHours().toString().padStart(2, '0')
    val minutes = toMinutes().toString().padStart(2, '0')
    val minutesPart = (toMinutes() % 60).toString().padStart(2, '0')
    val seconds = (seconds % 60).toString().padStart(2, '0')
    if (toMinutes() < 60) {
        return "${minutes}:${seconds}"
    }
    return "${hours}:${minutesPart}:${seconds}"
}


fun Duration.humanReadable(): String {
    if (this.seconds < 60) {
        return "${this.seconds} seconds"
    }
    val minutes = this.toMinutes()
    if (minutes < 60) {
        if (this.seconds == 0L) {
            return "$minutes min."
        }
        val fractionalSeconds = this.seconds % 60
        return "${this.toMinutes()}:${fractionalSeconds} min."
    }
    return "over an hour"
}


@Composable
fun CallType.Icon() {
    return when {
        this == CallType.INCOMING -> Icon(
            Icons.Default.CallMade,
            contentDescription = "Incoming Call",
            tint = androidx.compose.ui.graphics.Color.Green
        )
        this == CallType.OUTGOING -> Icon(
            Icons.Default.CallReceived,
            contentDescription = "Outgoing Call",
            tint = androidx.compose.ui.graphics.Color.Blue
        )
        else -> Icon(
            Icons.Default.CallMissed,
            contentDescription = "Missed Call",
            tint = androidx.compose.ui.graphics.Color.Blue
        )
    }
}
