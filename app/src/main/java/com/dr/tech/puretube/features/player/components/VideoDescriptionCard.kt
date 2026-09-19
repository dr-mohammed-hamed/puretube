package com.dr.tech.puretube.features.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Collapsible description card displaying video notes, references, and external details.
 * Conforms to Constitution Principles V, VI (< 100 LOC), VII.
 */
@Composable
fun VideoDescriptionCard(
    description: String?,
    modifier: Modifier = Modifier
) {
    if (description.isNullOrBlank()) return

    var isExpanded by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(16.dp))
    Surface(
        onClick = { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        color = PureTheme.colors.surface,
        border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الوصف والمراجع",
                    style = PureTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureTheme.colors.textPrimary
                )
                Text(
                    text = if (isExpanded) "عرض أقل" else "عرض المزيد",
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = PureTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
                color = PureTheme.colors.textSecondary,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
