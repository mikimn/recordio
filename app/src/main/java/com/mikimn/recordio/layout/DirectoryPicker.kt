package com.mikimn.recordio.layout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.*
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.DocumentFile.fromTreeUri


class DirectoryPickerState {
    internal var launcher: ManagedActivityResultLauncher<Uri?, Uri?>? = null
    internal var result by mutableStateOf<Uri?>(null)

    fun launch() = launcher?.launch(null)

    val uri: Uri?
        get() = result

    fun directory(context: Context): DocumentFile {
        val uri = uri ?: throw IllegalStateException("Must launch file picker first")
        val file = fromTreeUri(context, uri)
        return file!!
    }
}


@Composable
fun rememberDirectoryPickerState(): DirectoryPickerState {
    val state = remember {
        DirectoryPickerState()
    }
    val launcher =
        rememberLauncherForActivityResult(OpenPersistableDocumentsTree()) { originalUri ->
            state.result = originalUri
            Log.d("DirectoryPicker", "originalUri = ${originalUri?.encodedPath}")
        }
    state.launcher = launcher
    return state
}


private class OpenPersistableDocumentsTree : ActivityResultContract<Uri?, Uri?>() {
    var context: Context? = null

    override fun createIntent(context: Context, input: Uri?): Intent {
        this.context = context
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else {
            val treeUri = intent.data!!
            val context = context!!
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.grantUriPermission(context.packageName, treeUri, flags)
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
            this.context = null
            treeUri
        }
    }

    override fun getSynchronousResult(context: Context, input: Uri?): SynchronousResult<Uri?>? {
        return null
    }
}
