package ch.lottomatchapp.app.ui.dialogs

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import ch.lottomatchapp.app.R

@Composable
fun CameraPermissionGate(
    onDenied: () -> Unit,
    onGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val alreadyGranted = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(alreadyGranted) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { ok -> granted = ok }
    )

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(android.Manifest.permission.CAMERA)
    }

    if (granted) {
        onGranted()
    } else {
        AlertDialog(
            onDismissRequest = onDenied,
            confirmButton = {
                TextButton(onClick = { launcher.launch(android.Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.request_again))
                }
            },
            dismissButton = { TextButton(onClick = onDenied) { Text(stringResource(id = R.string.cancel)) } },
            title = { Text(stringResource(R.string.camera_permission_denied_title)) },
            text = { Text(stringResource(R.string.camera_permission_denied_message)) }
        )
    }
}
