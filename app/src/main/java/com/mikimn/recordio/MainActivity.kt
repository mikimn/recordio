package com.mikimn.recordio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikimn.recordio.ui.theme.RecordioTheme
import kotlinx.coroutines.launch
import java.time.Duration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecordioTheme {
                val scaffoldState = rememberScaffoldState()
                val snackbarCoroutineScope = rememberCoroutineScope()

                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar("Recordio") }
                ) { innerPadding ->
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = MaterialTheme.colors.background
                    ) {
                        RecordingList(
                            recordings = dummyCallRecordings(100)
                        ) { _, recording ->
                            snackbarCoroutineScope.launch {
                                scaffoldState.snackbarHostState
                                    .showSnackbar("Call recording ${recording.id} clicked")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(title: String) {
    TopAppBar(
        title = { Text(text = title) }
    )
}


@Composable
fun RecordingList(
    recordings: List<RegisteredCall>,
    onClick: (index: Int, recording: RegisteredCall) -> Unit,
) {
    val scrollState = rememberLazyListState()

    LazyColumn(state = scrollState) {
        itemsIndexed(recordings) { index, recording ->
            RecordingItem(recording) { onClick(index, recording) }
        }
    }
}


@Composable
fun RecordingItem(
    recording: RegisteredCall,
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
                recording.callType.Icon()
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = recording.source)
                Text(
                    text = recording.duration.humanReadable(),
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun RecordingItemPreview() {
    RecordingItem(
        recording = callRecordingFromId(0)
    )
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