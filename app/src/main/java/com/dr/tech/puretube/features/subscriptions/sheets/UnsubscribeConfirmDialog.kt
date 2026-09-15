package com.dr.tech.puretube.features.subscriptions.sheets

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Confirmation dialog to prevent accidental unsubscriptions (Principle I & UX Safety).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnsubscribeConfirmDialog(
    channel: SubscriptionEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PureTheme.colors.surfaceElevated,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonRemove,
                                contentDescription = null,
                                tint = PureTheme.colors.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "تأكيد إلغاء الاشتراك",
                        style = PureTheme.typography.titleMedium,
                        color = PureTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "هل أنت متأكد من رغبتك في إلغاء متابعة قناة \"${channel.channelName}\"؟ لن تظهر مقاطع هذه القناة في خلاصتك الرئيسية بعد الآن.",
                    style = PureTheme.typography.bodyMedium,
                    color = PureTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PureTheme.colors.borderOutline)
                    ) {
                        Text(
                            text = "تراجع",
                            style = PureTheme.typography.labelMedium,
                            color = PureTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureTheme.colors.error,
                            contentColor = PureTheme.colors.background
                        )
                    ) {
                        Text(
                            text = "إلغاء المتابعة",
                            style = PureTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
