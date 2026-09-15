package com.dr.tech.puretube.features.subscriptions.sheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import kotlinx.coroutines.launch

/**
 * Bottom Sheet for Data Portability (Constitution Principle III & IV).
 * Facilitates importing from NewPipe JSON and Google Takeout CSV,
 * and exporting current subscriptions into open NewPipe JSON format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportSheet(
    onDismiss: () -> Unit,
    onImportNewPipeJson: (String) -> Unit,
    onImportTakeoutCsv: (String) -> Unit,
    onExportNewPipeJson: suspend () -> String?,
    isImporting: Boolean = false,
    onError: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // File pickers
    val newPipePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val content = readTextFromUri(context, it)
            if (content != null) {
                onImportNewPipeJson(content)
                onDismiss()
            } else {
                onError?.invoke("تعذر قراءة ملف NewPipe المحدد. يرجى التحقق من صلاحية الملف.")
                onDismiss()
            }
        }
    }

    val takeoutPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val content = readTextFromUri(context, it)
            if (content != null) {
                onImportTakeoutCsv(content)
                onDismiss()
            } else {
                onError?.invoke("تعذر قراءة ملف Google Takeout المحدد. يرجى التحقق من صلاحية الملف.")
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = PureTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "نقل واستيراد الاشتراكات",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "بياناتك ملك لك. يمكنك استيراد اشتراكاتك أو تصديرها دون أي قيود.",
                style = PureTheme.typography.bodyMedium,
                color = PureTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isImporting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = PureTheme.colors.primary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "جارٍ معالجة الملف واستيراد القنوات...",
                        style = PureTheme.typography.bodyMedium,
                        color = PureTheme.colors.textPrimary
                    )
                }
            } else {
                PortabilityOptionCard(
                    icon = Icons.Default.FileUpload,
                    title = "استيراد اشتراكات NewPipe (JSON)",
                    subtitle = "قراءة ملف نسخ احتياطي من تطبيق NewPipe",
                    onClick = {
                        newPipePicker.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PortabilityOptionCard(
                    icon = Icons.Default.FileUpload,
                    title = "استيراد Google Takeout (CSV)",
                    subtitle = "قراءة ملف الاشتراكات المُصدر من حساب YouTube",
                    onClick = {
                        takeoutPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*"))
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PortabilityOptionCard(
                    icon = Icons.Default.FileDownload,
                    title = "تصدير الاشتراكات كملف NewPipe (JSON)",
                    subtitle = "مشاركة أو حفظ اشتراكاتك بتنسيق متوافق مع كافة المنصات",
                    onClick = {
                        scope.launch {
                            val exportedJson = onExportNewPipeJson()
                            if (!exportedJson.isNullOrBlank()) {
                                shareExportedText(context, exportedJson)
                                onDismiss()
                            } else {
                                onError?.invoke("لا توجد اشتراكات لتصديرها أو تعذر إعداد الملف.")
                                onDismiss()
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PortabilityOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PureTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PureTheme.colors.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PureTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = PureTheme.typography.bodyMedium,
                    color = PureTheme.colors.textSecondary
                )
            }
        }
    }
}

private fun readTextFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        }
    } catch (_: Exception) {
        null
    }
}

private fun shareExportedText(context: Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "application/json"
    }
    val shareIntent = Intent.createChooser(sendIntent, "تصدير اشتراكات PureTube")
    context.startActivity(shareIntent)
}
