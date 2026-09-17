package com.dr.tech.puretube.features.channel.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dr.tech.puretube.core.data.model.ChannelProfileDetails
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * About tab presenting full channel description and official channel stats.
 * Conforms to Constitution Principles I, V, VI (< 300 LOC), VII.
 */
@Composable
fun ChannelAboutTab(
    profile: ChannelProfileDetails,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val channelUrl = "https://www.youtube.com/channel/${profile.channelId}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = PureTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الوصف",
                        style = PureTheme.typography.titleMedium,
                        color = PureTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = profile.description?.ifBlank { null } ?: "لا يتوفر وصف لهذه القناة.",
                    style = PureTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = PureTheme.colors.textSecondary
                )
            }
        }

        // Details & Stats Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "تفاصيل القناة",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )

                // Channel Link with copy action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Channel Link", channelUrl))
                            Toast.makeText(context, "تم نسخ رابط القناة", Toast.LENGTH_SHORT).show()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = PureTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رابط القناة",
                            style = PureTheme.typography.labelSmall,
                            color = PureTheme.colors.textSecondary
                        )
                        Text(
                            text = channelUrl,
                            style = PureTheme.typography.bodyMedium,
                            color = PureTheme.colors.primary,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "نسخ الرابط",
                        tint = PureTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Subscribers info
                if (profile.subscriberCount > 0L || !profile.subscriberCountText.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            tint = PureTheme.colors.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "المشتركون",
                                style = PureTheme.typography.labelSmall,
                                color = PureTheme.colors.textSecondary
                            )
                            Text(
                                text = profile.subscriberCountText ?: "${profile.subscriberCount}",
                                style = PureTheme.typography.bodyMedium,
                                color = PureTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
